public class Fila {
    // Parâmetros da Fila
    public final int id;
    public final int servidores;
    public final int capacidade;
    public final double minAtendimento;
    public final double maxAtendimento;

    // Variáveis de Estado e Estatísticas
    public int clientesNoSistema;
    public int clientesPerdidos;
    public double[] temposPorEstado;
    public double tempoUltimoEvento;

    public Fila(int id, int servidores, int capacidade, double minAtendimento, double maxAtendimento) {
        this.id = id;
        this.servidores = servidores;
        this.capacidade = capacidade;
        this.minAtendimento = minAtendimento;
        this.maxAtendimento = maxAtendimento;
        this.temposPorEstado = new double[capacidade + 1];
        reset();
    }

    public void reset() {
        this.clientesNoSistema = 0;
        this.clientesPerdidos = 0;
        this.tempoUltimoEvento = 0.0;
        for (int i = 0; i < temposPorEstado.length; i++) {
            temposPorEstado[i] = 0.0;
        }
    }

    // Acumula o tempo gasto no estado atual da fila
    public void acumulaTempo(double tempoGlobal) {
        if (clientesNoSistema <= capacidade) {
            temposPorEstado[clientesNoSistema] += tempoGlobal - tempoUltimoEvento;
        }
        tempoUltimoEvento = tempoGlobal;
    }
}