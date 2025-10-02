import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedList;

public class Simulador {

    // Parâmetros Globais da Simulação
    private double tempoGlobal;
    private PriorityQueue<Evento> agenda;
    private LCG rng;
    private List<Fila> redeDeFilas;

    // Parâmetros da Rede
    private double minChegada;
    private double maxChegada;
    private double initialArrivalTime; // ex: 2.0

    // Rotas probabilísticas: map sourceQueueId -> list of (targetId, probability)
    private Map<Integer, List<Transicao>> roteamento;

    // Classe interna para representar transição
    private static class Transicao {
        public int target;
        public double prob;
        public Transicao(int t, double p) { this.target = t; this.prob = p; }
    }

    // Construtor estendido: passa seed e rndLimit para o LCG e também o time do 1º cliente
    public Simulador(double minChegada, double maxChegada, long seed, int rndLimit, double initialArrivalTime) {
        this.minChegada = minChegada;
        this.maxChegada = maxChegada;
        this.initialArrivalTime = initialArrivalTime;
        this.rng = new LCG(seed, rndLimit);
        this.agenda = new PriorityQueue<>();
        this.redeDeFilas = new ArrayList<>();
        this.roteamento = new HashMap<>();
    }

    // Método para adicionar fila (usa sua classe Fila existente)
    public void adicionarFila(Fila f) {
        this.redeDeFilas.add(f);
    }

    // Adiciona uma transição probabilística (sourceId -> targetId com prob)
    public void adicionarTransicao(int sourceId, int targetId, double prob) {
        roteamento.computeIfAbsent(sourceId, k -> new LinkedList<>()).add(new Transicao(targetId, prob));
    }

    // Escolhe destino a partir de uma fila de origem, usando RNG do LCG
    private int escolherDestino(int sourceId) {
        List<Transicao> lista = roteamento.get(sourceId);
        if (lista == null || lista.isEmpty()) {
            // fallback: se não configurado, cliente sai do sistema
            return -1; // sem destino
        }
        double u = rng.nextRandom(); // vai decrementar count internamente
        double cumul = 0.0;
        for (Transicao t : lista) {
            cumul += t.prob;
            if (u <= cumul) return t.target;
        }
        // proteção: retornar último destino se soma ≈ 1
        return lista.get(lista.size()-1).target;
    }

    private void tratarChegada(Evento e) {
        Fila fila = redeDeFilas.get(e.filaDestino); // Chegada externa -> filaDestino normalmente 0
        fila.clientesNoSistema++;
        if (fila.clientesNoSistema <= fila.servidores) {
            // inicia atendimento e agenda saida ou passagem
            if (roteamento.containsKey(fila.id)) {
                agendarPassagem(fila, tempoGlobal);
            } else {
                agendarSaida(fila, tempoGlobal);
            }
        }
        // se superou a capacidade, conta perda
        if (fila.clientesNoSistema > fila.capacidade) {
            fila.clientesNoSistema--; // não entra no sistema
            fila.clientesPerdidos++;
        }
        // após processar uma chegada externa, agenda próxima chegada (se houver aleatórios)
        agendarProximaChegada();
    }

    private void tratarPassagem(Evento e) {
        // passagem: cliente sai da filaOrigem e vai para filaDestino
        Fila origem = redeDeFilas.get(e.filaOrigem);
        origem.clientesNoSistema--; // saiu da origem

        int destino = -1;
        // se filaDestino >=0 no evento, usa-o; caso contrário, resolve via tabela de rotas
        if (e.filaDestino >= 0) destino = e.filaDestino;
        else destino = escolherDestino(e.filaOrigem);

        if (destino >= 0 && destino < redeDeFilas.size()) {
            Fila fDestino = redeDeFilas.get(destino);
            
            // Verifica se pode entrar na fila destino (não exceder capacidade)
            if (fDestino.clientesNoSistema < fDestino.capacidade) {
                fDestino.clientesNoSistema++;
                if (fDestino.clientesNoSistema <= fDestino.servidores) {
                    // decide se agenda passagem ou saída baseado nas rotas
                    if (roteamento.containsKey(destino)) {
                        agendarPassagem(fDestino, tempoGlobal);
                    } else {
                        agendarSaida(fDestino, tempoGlobal);
                    }
                }
            } else {
                // perda na fila destino - capacidade lotada
                fDestino.clientesPerdidos++;
            }
        }
        // se não há destino ou destino inválido, cliente sai do sistema

        // se ainda há clientes na fila origem esperando, agenda próximo atendimento
        if (origem.clientesNoSistema >= origem.servidores) {
            if (roteamento.containsKey(origem.id)) {
                agendarPassagem(origem, tempoGlobal);
            } else {
                agendarSaida(origem, tempoGlobal);
            }
        }
    }

    private void tratarSaida(Evento e) {
        Fila fila = redeDeFilas.get(e.filaOrigem);
        fila.clientesNoSistema--;
        if (fila.clientesNoSistema >= fila.servidores) {
            if (roteamento.containsKey(fila.id)) {
                agendarPassagem(fila, tempoGlobal);
            } else {
                agendarSaida(fila, tempoGlobal);
            }
        }
    }

    private void agendarProximaChegada() {
        if (rng.hasNext()) {
            double intervalo = rng.uniforme(this.minChegada, this.maxChegada);
            agenda.add(new Evento(tempoGlobal + intervalo, Evento.CHEGADA, -1, 0));
        }
    }

    private void agendarPassagem(Fila filaOrigem, double tempoAtual) {
        if (rng.hasNext()) {
            double servico = rng.uniforme(filaOrigem.minAtendimento, filaOrigem.maxAtendimento);
            // usamos filaOrigem.id como origem e deixamos filaDestino = -1 para decidir via tabela de rotas
            agenda.add(new Evento(tempoAtual + servico, Evento.PASSAGEM, filaOrigem.id, -1));
        }
    }

    private void agendarSaida(Fila filaOrigem, double tempoAtual) {
        if (rng.hasNext()) {
            double servico = rng.uniforme(filaOrigem.minAtendimento, filaOrigem.maxAtendimento);
            agenda.add(new Evento(tempoAtual + servico, Evento.SAIDA, filaOrigem.id, -1));
        }
    }

    public void simular() {
        // System.out.println("=== Simulação de Filas em Tandem ===");
        tempoGlobal = 0.0;
        agenda.clear();
        for (Fila f : redeDeFilas) {
            f.reset();
        }

        // agenda a primeira chegada no tempo pedido (ex.: 2.0)
        agenda.add(new Evento(this.initialArrivalTime, Evento.CHEGADA, -1, 0));

        // loop principal: continuará enquanto existirem números aleatórios e eventos na agenda
        while (rng.hasNext() && !agenda.isEmpty()) {
            Evento e = agenda.poll();
            tempoGlobal = e.tempo;

            // acumula os tempos por estado em cada fila
            for (Fila f : redeDeFilas) f.acumulaTempo(tempoGlobal);

            switch (e.tipo) {
                case Evento.CHEGADA:
                    tratarChegada(e);
                    break;
                case Evento.PASSAGEM:
                    tratarPassagem(e);
                    break;
                case Evento.SAIDA:
                    tratarSaida(e);
                    break;
            }
        }

        // Imprime resultados no formato solicitado
        System.out.println("2.\tResultado da Fila 1: G/G/1, chegadas entre 2..4, atendimento entre 1..2:");
        Fila f1 = redeDeFilas.get(0);
        double populacaoMedia1 = 0.0;
        int maxIndex1 = Math.min(f1.temposPorEstado.length-1, f1.capacidade);
        for (int j = 0; j <= maxIndex1; j++) {
            populacaoMedia1 += j * (f1.temposPorEstado[j] / tempoGlobal);
        }
        double probVazia1 = f1.temposPorEstado[0] / tempoGlobal;
        System.out.printf("População média: %.4f clientes\n", populacaoMedia1);
        System.out.printf("Clientes perdidos: %d\n", f1.clientesPerdidos);
        System.out.printf("Probabilidade da fila estar vazia (P0): %.4f (%.2f%%)\n", probVazia1, probVazia1*100);
        System.out.println();
        System.out.println();
        System.out.println();

        System.out.println("3.\tResultado da Fila 2: G/G/2/5, atendimento entre 4..6:");
        Fila f2 = redeDeFilas.get(1);
        double populacaoMedia2 = 0.0;
        int maxIndex2 = Math.min(f2.temposPorEstado.length-1, f2.capacidade);
        for (int j = 0; j <= maxIndex2; j++) {
            populacaoMedia2 += j * (f2.temposPorEstado[j] / tempoGlobal);
        }
        double probVazia2 = f2.temposPorEstado[0] / tempoGlobal;
        System.out.printf("População média: %.4f clientes\n", populacaoMedia2);
        System.out.printf("Clientes perdidos: %d\n", f2.clientesPerdidos);
        System.out.printf("Probabilidade da fila estar vazia (P0): %.4f (%.2f%%)\n", probVazia2, probVazia2*100);
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();

        System.out.println("4.\tResultado da Fila 3: G/G/2/10, atendimento entre 5..15:");
        Fila f3 = redeDeFilas.get(2);
        double populacaoMedia3 = 0.0;
        int maxIndex3 = Math.min(f3.temposPorEstado.length-1, f3.capacidade);
        for (int j = 0; j <= maxIndex3; j++) {
            populacaoMedia3 += j * (f3.temposPorEstado[j] / tempoGlobal);
        }
        double probVazia3 = f3.temposPorEstado[0] / tempoGlobal;
        System.out.printf("População média: %.4f clientes\n", populacaoMedia3);
        System.out.printf("Clientes perdidos: %d\n", f3.clientesPerdidos);
        System.out.printf("Probabilidade da fila estar vazia (P0): %.4f (%.2f%%)\n", probVazia3, probVazia3*100);
        System.out.println();
        System.out.println();
        System.out.println();

        System.out.println("5.\tTempo total de simulação:");
        System.out.printf("%.2f minutos\n", tempoGlobal);
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
    }
}
