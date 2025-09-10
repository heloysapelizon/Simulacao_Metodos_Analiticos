public class Evento implements Comparable<Evento> {
    public static final int CHEGADA = 0;
    public static final int SAIDA = 1;
    public static final int PASSAGEM = 2; 

    double tempo;
    int tipo;
    int filaOrigem;
    int filaDestino;

    public Evento(double tempo, int tipo, int filaOrigem, int filaDestino) {
        this.tempo = tempo;
        this.tipo = tipo;
        this.filaOrigem = filaOrigem;
        this.filaDestino = filaDestino;
    }

    @Override
    public int compareTo(Evento o) {
        return Double.compare(this.tempo, o.tempo);
    }
}