import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Map;

public class Simulador {

    private double tempoGlobal;
    private PriorityQueue<Evento> agenda;
    private LCG rng;
    private List<Fila> redeDeFilas;

    public Simulador(double minChegada, double maxChegada, Map<String, Object> config) {
        this.rng = new LCG(System.currentTimeMillis(), 100000);
        this.agenda = new PriorityQueue<>();
        this.redeDeFilas = new ArrayList<>();
        carregarFilas(config);
        agendarProximaChegada(minChegada, maxChegada);
    }

    private void carregarFilas(Map<String, Object> config) {
        List<Map<String, Object>> filasConfig = (List<Map<String, Object>>) config.get("queues");
        for (Map<String, Object> filaConfig : filasConfig) {
            int id = (int) filaConfig.get("id");
            int servidores = (int) filaConfig.get("servers");
            int capacidade = (int) filaConfig.get("capacity");
            double minAtendimento = (double) filaConfig.get("minService");
            double maxAtendimento = (double) filaConfig.get("maxService");
            Fila fila = new Fila(id, servidores, capacidade, minAtendimento, maxAtendimento);
            this.redeDeFilas.add(fila);
        }
    }

    private void agendarProximaChegada(double minChegada, double maxChegada) {
        if (rng.hasNext()) {
            double intervalo = rng.uniforme(minChegada, maxChegada);
            agenda.add(new Evento(tempoGlobal + intervalo, Evento.CHEGADA, -1, 0));
        }
    }

    private void tratarChegada(Evento e) {
        Fila fila = redeDeFilas.get(e.filaDestino);
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
        filaOrigem.clientesNoSistema--;
        if (filaOrigem.clientesNoSistema >= filaOrigem.servidores) {
            agendarPassagem(filaOrigem, tempoGlobal);
        }

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

    public void simular() {
        tempoGlobal = 0.0;
        agenda.clear();
        for (Fila f : redeDeFilas) {
            f.reset();
        }

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
        System.out.printf("Tempo total de simulação: %.2f\n\n", tempoGlobal);
        for (Fila f : redeDeFilas) {
            System.out.println("===== Resultado Fila " + (f.id + 1) + " (G/G/" + f.servidores + "/" + f.capacidade + ") =====");
            System.out.println("Clientes perdidos: " + f.clientesPerdidos);
            System.out.println("--- Distribuição de Tempo por Estado ---");
            for (int i = 0; i <= f.capacidade; i++) {
                double prob = f.temposPorEstado[i] / tempoGlobal;
                System.out.printf("Tempo no Estado %d: %.2f min (Prob: %.4f)\n", i, f.temposPorEstado[i], prob);
            }
            System.out.println();
        }
    }
}