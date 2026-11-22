package yhjm1208.basicstatuscommand.metrics;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * bukkit 线的 TPS 采样：每 tick 记一次时间戳，按 1/5/15 分钟窗口回算。
 * 插件刚启用时窗口没铺满，{@link #coverage()} 会小于窗口长度，命令层据此标「采样中」。
 */
public final class TpsSampler {

    private static final int[] WINDOWS_SECONDS = {60, 300, 900};
    private static final int CAPACITY = 20 * 900 + 100;

    private final JavaPlugin plugin;
    private final long[] samples = new long[CAPACITY];

    private int head;
    private int size;
    private BukkitTask task;

    public TpsSampler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private synchronized void tick() {
        samples[head] = System.nanoTime();
        head = (head + 1) % CAPACITY;
        if (size < CAPACITY) {
            size++;
        }
    }

    public synchronized double[] tps() {
        double[] coverage = new double[WINDOWS_SECONDS.length];
        return compute(coverage);
    }

    public synchronized double[] coverage() {
        double[] coverage = new double[WINDOWS_SECONDS.length];
        compute(coverage);
        return coverage;
    }

    private double[] compute(double[] coverageOut) {
        double[] out = {Double.NaN, Double.NaN, Double.NaN};
        if (size < 2) {
            return out;
        }
        long now = System.nanoTime();
        int newestIndex = (head - 1 + CAPACITY) % CAPACITY;

        for (int w = 0; w < WINDOWS_SECONDS.length; w++) {
            long from = now - WINDOWS_SECONDS[w] * 1_000_000_000L;
            int index = newestIndex;
            int count = 0;
            long oldest = 0L;
            for (int i = 0; i < size; i++) {
                long stamp = samples[index];
                if (stamp < from) {
                    break;
                }
                oldest = stamp;
                count++;
                index = (index - 1 + CAPACITY) % CAPACITY;
            }
            if (count >= 2) {
                double span = (samples[newestIndex] - oldest) / 1_000_000_000.0;
                if (span > 0.0) {
                    out[w] = (count - 1) / span;
                    coverageOut[w] = span;
                }
            }
        }
        return out;
    }
}
