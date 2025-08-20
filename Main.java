public class Main {
    public static void main(String[] args) {
        System.out.println("=== Simulação de filas ===\n");

        // Simulação G/G/1/5
        Fila fila1 = new Fila(1, 5);
        fila1.simular();
        fila1.imprimirResultados();

        // Simulação G/G/2/5
        Fila fila2 = new Fila(2, 5);
        fila2.simular();
        fila2.imprimirResultados();
    }
}
