package yhjm1208.basicstatuscommand.metrics;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** JVM 读数：CPU、内存、GC、线程、运行时长。全部走 java.lang.management，不引第三方库。 */
public final class Jvm {

    private static final OperatingSystemMXBean OS = ManagementFactory.getOperatingSystemMXBean();
    private static final RuntimeMXBean RUNTIME = ManagementFactory.getRuntimeMXBean();
    private static final MemoryMXBean MEMORY = ManagementFactory.getMemoryMXBean();
    private static final ThreadMXBean THREADS = ManagementFactory.getThreadMXBean();

    private Jvm() {
    }

    public static double processCpuLoad() {
        return load("getProcessCpuLoad");
    }

    public static double systemCpuLoad() {
        return load("getSystemCpuLoad");
    }

    private static double load(String methodName) {
        try {
            Class<?> type = Class.forName("com.sun.management.OperatingSystemMXBean");
            Method method = type.getMethod(methodName);
            Object value = method.invoke(OS);
            if (value instanceof Number) {
                double load = ((Number) value).doubleValue();
                return load < 0.0 ? -1.0 : load * 100.0;
            }
        } catch (Throwable ignored) {
            // 非 HotSpot 或模块被裁掉
        }
        return -1.0;
    }

    /** {used, committed, max}，max 可能为 -1。 */
    public static long[] heap() {
        MemoryUsage usage = MEMORY.getHeapMemoryUsage();
        return new long[]{usage.getUsed(), usage.getCommitted(), usage.getMax()};
    }

    public static long nonHeapUsed() {
        return MEMORY.getNonHeapMemoryUsage().getUsed();
    }

    /** {total, free}，拿不到返回 {-1, -1}。 */
    public static long[] physicalMemory() {
        try {
            Class<?> type = Class.forName("com.sun.management.OperatingSystemMXBean");
            Method total = type.getMethod("getTotalPhysicalMemorySize");
            Method free = type.getMethod("getFreePhysicalMemorySize");
            return new long[]{((Number) total.invoke(OS)).longValue(), ((Number) free.invoke(OS)).longValue()};
        } catch (Throwable ignored) {
            return new long[]{-1L, -1L};
        }
    }

    public static List<Gc> gc() {
        List<Gc> result = new ArrayList<>();
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            result.add(new Gc(bean.getName(), bean.getCollectionCount(), bean.getCollectionTime()));
        }
        return result;
    }

    /** {活线程, 峰值, 守护线程, 累计创建} */
    public static int[] threads() {
        return new int[]{
                THREADS.getThreadCount(),
                THREADS.getPeakThreadCount(),
                THREADS.getDaemonThreadCount(),
                (int) Math.min(Integer.MAX_VALUE, THREADS.getTotalStartedThreadCount())
        };
    }

    public static long uptimeMillis() {
        return RUNTIME.getUptime();
    }

    public static int processors() {
        return OS.getAvailableProcessors();
    }

    public static double systemLoadAverage() {
        return OS.getSystemLoadAverage();
    }

    public static final class Gc {
        public final String name;
        public final long count;
        public final long timeMs;

        Gc(String name, long count, long timeMs) {
            this.name = name;
            this.count = Math.max(0L, count);
            this.timeMs = Math.max(0L, timeMs);
        }
    }
}
