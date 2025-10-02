import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigLoader {

    public static Map<String, Object> loadConfig(String fileName) {
        Map<String, Object> config = new HashMap<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            List<String> lines = new ArrayList<>();
            String line;
            
            // Ler todas as linhas primeiro
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            
            // Parse linha por linha
            for (int i = 0; i < lines.size(); i++) {
                String currentLine = lines.get(i).trim();
                
                if (currentLine.isEmpty() || currentLine.startsWith("#") || currentLine.startsWith("!")) {
                    continue;
                }
                
                // Parse configurações gerais
                if (currentLine.contains("rndnumbersPerSeed:")) {
                    String[] parts = currentLine.split(":");
                    config.put("rndnumbersPerSeed", Integer.parseInt(parts[1].trim()));
                }
                
                // Parse seções
                else if (currentLine.equals("seeds:")) {
                    i = parseSeeds(lines, i + 1, config);
                }
                else if (currentLine.equals("arrivals:")) {
                    i = parseArrivals(lines, i + 1, config);
                }
                else if (currentLine.equals("queues:")) {
                    i = parseQueues(lines, i + 1, config);
                }
                else if (currentLine.equals("network:")) {
                    i = parseNetwork(lines, i + 1, config);
                }
            }
            
        } catch (IOException e) {
            throw new RuntimeException("Erro ao carregar configuração: " + e.getMessage(), e);
        }
        
        return config;
    }
    
    private static int parseSeeds(List<String> lines, int startIndex, Map<String, Object> config) {
        List<Integer> seeds = new ArrayList<>();
        int i = startIndex;
        
        while (i < lines.size()) {
            String line = lines.get(i);
            if (line.trim().isEmpty()) {
                i++;
                continue;
            }
            
            if (line.startsWith("-")) {
                String seedStr = line.substring(1).trim();
                seeds.add(Integer.parseInt(seedStr));
                i++;
            } else {
                break;
            }
        }
        
        config.put("seeds", seeds);
        return i - 1; // Voltar um índice para não pular linha
    }
    
    private static int parseArrivals(List<String> lines, int startIndex, Map<String, Object> config) {
        Map<String, Object> arrivals = new HashMap<>();
        int i = startIndex;
        
        while (i < lines.size()) {
            String line = lines.get(i);
            if (line.trim().isEmpty()) {
                i++;
                continue;
            }
            
            // Verificar se a linha tem indentação e contém ':'
            if (line.contains(":") && (line.startsWith("   ") || line.startsWith("	"))) {
                String[] parts = line.trim().split(":", 2);
                String key = parts[0].trim();
                String value = parts[1].trim();
                arrivals.put(key, Double.parseDouble(value));
                i++;
            } else {
                break;
            }
        }
        
        config.put("arrivals", arrivals);
        return i - 1;
    }
    
    private static int parseQueues(List<String> lines, int startIndex, Map<String, Object> config) {
        Map<String, Object> queues = new HashMap<>();
        int i = startIndex;
        
        while (i < lines.size()) {
            String line = lines.get(i);
            if (line.trim().isEmpty()) {
                i++;
                continue;
            }
            
            // Se é uma nova fila (sem indentação, com :)
            if (line.contains(":") && line.startsWith("   ") && !line.contains("servers") && 
                !line.contains("capacity") && !line.contains("min") && !line.contains("max")) {
                
                String queueName = line.replace(":", "").trim();
                Map<String, Object> queueConfig = new HashMap<>();
                i++;
                
                // Ler configurações da fila
                while (i < lines.size()) {
                    String configLine = lines.get(i);
                    if (configLine.trim().isEmpty()) {
                        i++;
                        continue;
                    }
                    
                    // Se encontrou outra fila ou seção, parar
                    if (!configLine.startsWith("      ")) {
                        break;
                    }
                    
                    // Parse da configuração
                    String trimmed = configLine.trim();
                    if (trimmed.contains(":")) {
                        String[] parts = trimmed.split(":", 2);
                        String key = parts[0].trim();
                        String value = parts[1].trim();
                        
                        try {
                            queueConfig.put(key, Double.parseDouble(value));
                        } catch (NumberFormatException e) {
                            queueConfig.put(key, value);
                        }
                    }
                    i++;
                }
                
                queues.put(queueName, queueConfig);
            } else {
                break;
            }
        }
        
        config.put("queues", queues);
        return i - 1;
    }
    
    private static int parseNetwork(List<String> lines, int startIndex, Map<String, Object> config) {
        List<Map<String, Object>> networkList = new ArrayList<>();
        int i = startIndex;
        
        while (i < lines.size()) {
            String line = lines.get(i);
            if (line.trim().isEmpty()) {
                i++;
                continue;
            }
            
            if (line.startsWith("-")) {
                Map<String, Object> connection = new HashMap<>();
                
                // Parse da linha atual (que pode ter source na mesma linha)
                String currentLine = line.trim();
                if (currentLine.contains("source:")) {
                    String source = currentLine.substring(currentLine.indexOf("source:") + 7).trim();
                    connection.put("source", source);
                }
                
                i++;
                
                // Parse das próximas linhas da conexão
                while (i < lines.size()) {
                    String nextLine = lines.get(i);
                    if (nextLine.trim().isEmpty()) {
                        i++;
                        continue;
                    }
                    
                    // Se encontrou nova entrada ou nova seção
                    if (nextLine.startsWith("-") || (!nextLine.startsWith("   ") && nextLine.contains(":"))) {
                        break;
                    }
                    
                    String trimmed = nextLine.trim();
                    if (trimmed.contains("target:")) {
                        String target = trimmed.substring(trimmed.indexOf("target:") + 7).trim();
                        connection.put("target", target);
                    } else if (trimmed.contains("probability:")) {
                        String prob = trimmed.substring(trimmed.indexOf("probability:") + 12).trim();
                        connection.put("probability", Double.parseDouble(prob));
                    }
                    
                    i++;
                }
                
                if (connection.size() >= 2) { // Deve ter pelo menos source e target
                    networkList.add(connection);
                }
            } else {
                break;
            }
        }
        
        config.put("network", networkList);
        return i - 1;
    }
}
