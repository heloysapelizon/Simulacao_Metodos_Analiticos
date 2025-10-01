public class Main {
    public static void main(String[] args) {
        System.out.println("=== Simulação de Filas em Tandem ===\n");

        // Parse the YAML configuration file to get queue parameters and network topology
        YamlParser parser = new YamlParser("model/model.yml");
        Simulador simulador = new Simulador(parser.getMinChegada(), parser.getMaxChegada());

        // Add queues based on the parsed configuration
        for (Fila fila : parser.getFilas()) {
            simulador.adicionarFila(fila);
        }

        // Start the simulation
        simulador.simular();
    }
}