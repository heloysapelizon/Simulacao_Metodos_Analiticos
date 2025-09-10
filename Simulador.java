import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public class Simulador {

    // Parâmetros Globais da Simulação
    private double tempoGlobal;
    private PriorityQueue<Evento> agenda;
    private LCG rng;
    private List<Fila> redeDeFilas;

    // Parâmetros da Rede
    private double minChegada;
    private double maxChegada;

    public Simulador(double minChegada, double maxChegada) {
        this.minChegada = minChegada;
        this.maxChegada = maxChegada;
        this.rng = new LCG(System.currentTimeMillis(), 100000); 
        this.agenda = new PriorityQueue<>();
        this.redeDeFilas = new ArrayList<>();
    }

    public void adicionarFila(Fila fila) {
        this.redeDeFilas.add(fila);
    }

    private void tratarChegada(Evento e) {
        Fila fila = redeDeFilas.get(e.filaDestino); // Chegada sempre na Fila 0 do exterior

        if (fila.clientesNoSistema < fila.capacidade) {
            fila.clientesNoSistema++;
            if (fila.clientesNoSistema <= fila.servidores) {
                agendarPassagem(fila, tempoGlobal);
            }
        } else {
            fila.clientesPerdidos++;
        }
        agendarProximaChegada();
    }
    
    private void tratarPassagem(Evento e) {
        Fila filaOrigem = redeDeFilas.get(e.filaOrigem);
        
        // Processa a SAÍDA da fila de origem
        filaOrigem.clientesNoSistema--;
        if (filaOrigem.clientesNoSistema >= filaOrigem.servidores) {
             agendarPassagem(filaOrigem, tempoGlobal);
        }

        // Processa a CHEGADA na fila de destino
        Fila filaDestino = redeDeFilas.get(e.filaDestino);
        if (filaDestino.clientesNoSistema < filaDestino.capacidade) {
            filaDestino.clientesNoSistema++;
            if (filaDestino.clientesNoSistema <= filaDestino.servidores) {
                agendarSaida(filaDestino, tempoGlobal);
            }
        } else {
            filaDestino.clientesPerdidos++;
        }
    }

    private void tratarSaida(Evento e) {
        Fila fila = redeDeFilas.get(e.filaOrigem); 
        fila.clientesNoSistema--;
        if (fila.clientesNoSistema >= fila.servidores) {
            agendarSaida(fila, tempoGlobal);
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
            agenda.add(new Evento(tempoAtual + servico, Evento.PASSAGEM, filaOrigem.id, filaOrigem.id + 1));
        }
    }
    
    private void agendarSaida(Fila filaOrigem, double tempoAtual) {
        if (rng.hasNext()) {
            double servico = rng.uniforme(filaOrigem.minAtendimento, filaOrigem.maxAtendimento);
            agenda.add(new Evento(tempoAtual + servico, Evento.SAIDA, filaOrigem.id, -1));
        }
    }

    public void simular() {
        tempoGlobal = 0.0;
        agenda.clear();
        for (Fila f : redeDeFilas) {
            f.reset();
        }

        agenda.add(new Evento(1.5, Evento.CHEGADA, -1, 0));

        while (rng.hasNext() && !agenda.isEmpty()) {
            Evento e = agenda.poll();
            tempoGlobal = e.tempo;

            for (Fila f : redeDeFilas) {
                f.acumulaTempo(tempoGlobal);
            }

            if (e.tipo == Evento.CHEGADA) {
                tratarChegada(e);
            } else if (e.tipo == Evento.PASSAGEM) {
                tratarPassagem(e);
            } else if (e.tipo == Evento.SAIDA) {
                tratarSaida(e);
            }
        }
        imprimirResultados();
    }

    public void imprimirResultados() {
        // O tempo de simulação é global para toda a rede
        System.out.printf("Tempo total de simulação: %.2f\n\n", tempoGlobal);

        for (Fila f : redeDeFilas) {
            System.out.println("===== Resultado Fila " + (f.id + 1) + " (G/G/" + f.servidores + "/" + f.capacidade + ") =====");
            System.out.println("Clientes perdidos: " + f.clientesPerdidos);
            System.out.println("--- Distribuição de Tempo por Estado ---");
            for (int i = 0; i <= f.capacidade; i++) {
                double prob = f.temposPorEstado[i] / tempoGlobal;
                System.out.printf("Tempo no Estado %d: %.2f min (Prob: %.4f)\n", i, f.temposPorEstado[i], prob);
            }
            
            System.out.println("\n--- Métricas Adicionais ---");

            // 1. População média (L) = Σ (i * Pi)
            double populacaoMedia = 0.0;
            for (int i = 0; i <= f.capacidade; i++) {
                double prob = f.temposPorEstado[i] / tempoGlobal;
                populacaoMedia += i * prob;
            }
            System.out.printf("População média (L): %.4f clientes\n", populacaoMedia);

            // Probabilidade da fila estar vazia
            double probVazia = f.temposPorEstado[0] / tempoGlobal;
            System.out.printf("Probabilidade da fila estar vazia (P0): %.4f (%.2f%%)\n", probVazia, probVazia * 100);

            // Tempo com exatamente 1 cliente na fila
            double tempoCom1Cliente = f.temposPorEstado[1];
            System.out.printf("Tempo acumulado com 1 cliente: %.2f min\n", tempoCom1Cliente);
            
            System.out.println(); 
        }
    }
}