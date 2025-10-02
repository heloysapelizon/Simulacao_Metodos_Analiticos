import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;

public class Main {
    public static void main(String[] args) {
        try {
            // 1. Carregar configuração do YAML
            Map<String, Object> config = ConfigLoader.loadConfig("Tandem.yml");

            // 2. Extrair parâmetros da simulação
            int rndnumbersPerSeed = ((Number) config.get("rndnumbersPerSeed")).intValue();
            List<Integer> seeds = (List<Integer>) config.get("seeds");
            long seed = seeds.get(0).longValue();

            // 3. Parâmetros de chegada (usar TRIAGEM como referência)
            Map<String, Object> arrivals = (Map<String, Object>) config.get("arrivals");
            double minChegada = 1.0;  // valor padrão
            double maxChegada = 5.0;  // valor padrão
            
            if (arrivals != null && arrivals.containsKey("TRIAGEM")) {
                double arrivalRate = ((Number) arrivals.get("TRIAGEM")).doubleValue();
                minChegada = arrivalRate * 0.8;  // 80% da taxa base
                maxChegada = arrivalRate * 1.2;  // 120% da taxa base
            }

            // 4. Criar simulador com parâmetros do YAML
            // Primeiro cliente chega em t = 2.0 conforme instruções
            Simulador simulador = new Simulador(minChegada, maxChegada, seed, rndnumbersPerSeed, 2.0);

            // 5. Criar filas baseadas no YAML
            Map<String, Object> queues = (Map<String, Object>) config.get("queues");
            Map<String, Integer> nomeParaId = new LinkedHashMap<>();
            
            int filaId = 0;
            // System.out.println("Criando filas baseadas no YAML:");
            for (Map.Entry<String, Object> entry : queues.entrySet()) {
                String nomeFila = entry.getKey();
                Map<String, Object> props = (Map<String, Object>) entry.getValue();

                int servers = ((Number) props.get("servers")).intValue();
                int capacity = ((Number) props.get("capacity")).intValue();
                
                // Buscar minService/maxService (nomes das instruções) ou minAtendimento/maxAtendimento
                double minService = 0, maxService = 0;
                if (props.containsKey("minService")) {
                    minService = ((Number) props.get("minService")).doubleValue();
                    maxService = ((Number) props.get("maxService")).doubleValue();
                } else if (props.containsKey("minAtendimento")) {
                    minService = ((Number) props.get("minAtendimento")).doubleValue();
                    maxService = ((Number) props.get("maxAtendimento")).doubleValue();
                } else {
                    // System.err.println("Aviso: Fila " + nomeFila + " sem tempos de serviço definidos");
                    minService = 1.0;
                    maxService = 5.0;
                }

                Fila fila = new Fila(filaId, servers, capacity, minService, maxService);
                simulador.adicionarFila(fila);
                nomeParaId.put(nomeFila, filaId);
                
                // System.out.printf(" - %s (ID %d): %d servidores, capacidade %d, serviço [%.1f-%.1f]\n", 
                //                  nomeFila, filaId, servers, capacity, minService, maxService);
                filaId++;
            }

            // 6. Configurar rotas probabilísticas
            List<Map<String, Object>> network = (List<Map<String, Object>>) config.get("network");
            // System.out.println("\nConfigurando rotas probabilísticas:");
            
            if (network != null) {
                for (Map<String, Object> conexao : network) {
                    String sourceName = (String) conexao.get("source");
                    String targetName = (String) conexao.get("target");
                    double probability = ((Number) conexao.get("probability")).doubleValue();

                    Integer sourceId = nomeParaId.get(sourceName);
                    Integer targetId = nomeParaId.get(targetName);

                    if (sourceId != null && targetId != null) {
                        simulador.adicionarTransicao(sourceId, targetId, probability);
                        // System.out.printf(" - %s (ID %d) -> %s (ID %d): %.1f%%\n", 
                        //                  sourceName, sourceId, targetName, targetId, probability * 100);
                    } else {
                        // System.err.println("Aviso: Rota inválida " + sourceName + " -> " + targetName);
                    }
                }
            }

            // 7. Executar simulação
            // System.out.println("\nParâmetros da simulação:");
            // System.out.printf(" - Seed: %d\n", seed);
            // System.out.printf(" - Números aleatórios: %d\n", rndnumbersPerSeed);
            // System.out.printf(" - Chegadas: [%.1f - %.1f]\n", minChegada, maxChegada);
            // System.out.printf(" - Primeiro cliente: t = 2.0\n");
            
            simulador.simular();

        } catch (Exception e) {
            System.err.println("Erro na simulação: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
