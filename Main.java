public class Main {
    public static void main(String[] args) {
        System.out.println("=== Simulação de Filas em Tandem ===\n");

        Simulador simulador = new Simulador(1.0, 4.0);

        simulador.adicionarFila(new Fila(0, 2, 3, 3.0, 4.0));

        simulador.adicionarFila(new Fila(1, 1, 5, 2.0, 3.0));

        simulador.simular();
    }
}