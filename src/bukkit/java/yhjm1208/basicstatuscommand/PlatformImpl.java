package yhjm1208.basicstatuscommand;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import yhjm1208.basicstatuscommand.metrics.TpsSampler;
import yhjm1208.basicstatuscommand.util.Reflect;

/**
 * Bukkit / Spigot 线：只用 Bukkit 公开 API。
 * TPS 由 {@link TpsSampler} 逐 tick 采样；ping 走 CraftPlayer 句柄反射（1.8.8 的 Player 没有 getPing）。
 */
public final class PlatformImpl implements Platform {

    private final JavaPlugin plugin;
    private TpsSampler sampler;

    private PlatformImpl(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public static PlatformImpl create(JavaPlugin plugin) {
        return new PlatformImpl(plugin);
    }

    @Override
    public String label() {
        return "Bukkit API";
    }

    @Override
    public void start() {
        sampler = new TpsSampler(plugin);
        sampler.start();
    }

    @Override
    public void stop() {
        if (sampler != null) {
            sampler.stop();
        }
    }

    @Override
    public double[] tps() {
        return sampler == null
                ? new double[]{Double.NaN, Double.NaN, Double.NaN}
                : sampler.tps();
    }

    @Override
    public double[] tpsCoverage() {
        return sampler == null
                ? new double[]{Double.NaN, Double.NaN, Double.NaN}
                : sampler.coverage();
    }

    @Override
    public double mspt() {
        return Double.NaN;
    }

    @Override
    public int ping(Player player) {
        // 1.17+ 的 Bukkit API 有 Player#getPing()，但 1.8.8 编译基线里没有，只能运行期反射
        Object viaApi = Reflect.call(player, "getPing");
        if (viaApi instanceof Number) {
            return Math.max(0, ((Number) viaApi).intValue());
        }
        // 1.8.8 - 1.16.5：EntityPlayer.ping / 1.20.5+ 的 latency
        Object handle = Reflect.call(player, "getHandle");
        Integer viaField = Reflect.intField(handle, "ping", "latency", "field_71138_i");
        return viaField == null ? -1 : Math.max(0, viaField);
    }
}
