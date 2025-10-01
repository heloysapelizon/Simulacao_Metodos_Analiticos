import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class YamlParser {
    private Yaml yaml;
    private InputStream inputStream;
    private SimulationConfig config;

    public static class QueueConfig {
        public int servers;
        public int capacity;
        public double minArrival;
        public double maxArrival;
        public double minService;
        public double maxService;
    }

    public static class NetworkConfig {
        public String source;
        public String target;
        public double probability;
    }

    public static class SimulationConfig {
        public Map<String, Double> arrivals;
        public Map<String, QueueConfig> queues;
        public List<NetworkConfig> network;
    }

    public YamlParser(String filePath) {
        yaml = new Yaml(new Constructor(SimulationConfig.class, null));
        inputStream = this.getClass().getClassLoader().getResourceAsStream(filePath);
        config = yaml.load(inputStream);
    }

    public double getMinChegada(){
        double minArrival = Double.MAX_VALUE;
        for (QueueConfig queue : config.queues.values()) {
            if (queue.minArrival < minArrival) {
            minArrival = queue.minArrival;
            }
        }
        return minArrival;
    }

    public double getMaxChegada(){
        double maxArrival = 0.0;
        for (QueueConfig queue : config.queues.values()) {
            if (queue.maxArrival > maxArrival) {
            maxArrival = queue.maxArrival;
            }
        }
        return maxArrival;
    }
    public List<Fila> getFilas(SimulationConfig config) {
        List<Fila> filas = new ArrayList<>();
        int id = 0;

        for (Map.Entry<String, QueueConfig> entry : config.queues.entrySet()) {
            QueueConfig queueConfig = entry.getValue();
            filas.add(new Fila(id++, queueConfig.servers, queueConfig.capacity, queueConfig.minService, queueConfig.maxService));
        }

        return filas;
    }
}