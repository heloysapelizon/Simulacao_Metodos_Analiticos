import java.util.PriorityQueue;

public class Fila {
    private int servidores;
    private int K;
    private int clientesSistema;
    private int clientesPerdidos;
    private double tempoGlobal;
    private double ultimoEvento;
    private double[] temposEstado;
    private PriorityQueue<Evento> agenda;
    private LCG rng;

    public Fila(int servidores, int K) {
        this.servidores = servidores;
        this.K = K;
        this.temposEstado = new double[K + 1];
        this.agenda = new PriorityQueue<>();
        this.rng = new LCG(12345, 100000); // semente fixa
    }

    public void simular() {
        clientesSistema = 0;
        clientesPerdidos = 0;
        tempoGlobal = 0.0;
        ultimoEvento = 0.0;
        for (int i = 0; i <= K; i++) temposEstado[i] = 0.0;
        agenda.clear();

        // primeira chegada em t=2.0
        agenda.add(new Evento(2.0, Evento.CHEGADA));

        while (rng.hasNext() && !agenda.isEmpty()) {
            Evento e = agenda.poll();
            double tempoAtual = e.tempo;

            // acumula tempo no estado atual
            if (clientesSistema <= K) {
                temposEstado[clientesSistema] += tempoAtual - ultimoEvento;
            }

            ultimoEvento = tempoAtual;
            tempoGlobal = tempoAtual;

            if (e.tipo == Evento.CHEGADA) {
                if (clientesSistema < K) {
                    clientesSistema++;
                    // se há servidor livre, agenda saída
                    if (clientesSistema <= servidores) {
                        double servico = rng.uniforme(3.0, 5.0);
                        agenda.add(new Evento(tempoAtual + servico, Evento.SAIDA));
                    }
                } else {
                    clientesPerdidos++;
                }

                // agenda próxima chegada
                double inter = rng.uniforme(2.0, 5.0);
                agenda.add(new Evento(tempoAtual + inter, Evento.CHEGADA));

            } else if (e.tipo == Evento.SAIDA) {
                clientesSistema--;
                if (clientesSistema >= servidores) {
                    // ainda há clientes esperando → agenda saída
                    double servico = rng.uniforme(3.0, 5.0);
                    agenda.add(new Evento(tempoAtual + servico, Evento.SAIDA));
                }
            }
        }
    }

    public void imprimirResultados() {
        System.out.println("===== Resultado G/G/" + servidores + "/" + K + " =====");
        System.out.printf("Tempo total de simulação: %.2f\n", tempoGlobal);
        System.out.println("Clientes perdidos: " + clientesPerdidos);
        for (int i = 0; i <= K; i++) {
            double prob = temposEstado[i] / tempoGlobal;
            System.out.printf("Estado %d: %.2f min (%.4f)\n", i, temposEstado[i], prob);
        }
        System.out.println();
    }
}
