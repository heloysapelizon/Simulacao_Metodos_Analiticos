public class LCG {
    private long a = 1664525;
    private long c = 1013904223;
    private long M = (long) Math.pow(2, 32);
    private long seed;
    private int count;

    public LCG(long seed, int count) {
        this.seed = seed;
        this.count = count;
    }

    public boolean hasNext() {
        return count > 0;
    }

    public double nextRandom() {
        if (count <= 0) throw new RuntimeException("Limite de aleatórios atingido!");
        seed = (a * seed + c) % M;
        count--;
        return (double) seed / M;
    }

    public double uniforme(double min, double max) {
        return min + (max - min) * nextRandom();
    }
}