package yhjm1208.basicstatuscommand;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Paper 线：直接用 Paper 的 TPS / MSPT / ping 接口，数值是服务端自己算好的，无需暖机。
 */
public final class PlatformImpl implements Platform {

    private PlatformImpl(JavaPlugin plugin) {
        // 保留签名与 bukkit 版一致，主类不感知差异
    }

    public static PlatformImpl create(JavaPlugin plugin) {
        return new PlatformImpl(plugin);
    }

    @Override
    public String label() {
        return "Paper";
    }

    @Override
    public void start() {
        // Paper 自带 TPS 记录，无需采样任务
    }

    @Override
    public void stop() {
    }

    @Override
    public double[] tps() {
        try {
            double[] nativeTps = Bukkit.getTPS();
            if (nativeTps != null && nativeTps.length >= 3) {
                return nativeTps;
            }
        } catch (Throwable ignored) {
            // 非 Paper 实现
        }
        return new double[]{Double.NaN, Double.NaN, Double.NaN};
    }

    @Override
    public double[] tpsCoverage() {
        // Paper 的窗口是服务端完整统计的
        return new double[]{60.0, 300.0, 900.0};
    }

    @Override
    public double mspt() {
        try {
            return Bukkit.getServer().getAverageTickTime();
        } catch (Throwable ignored) {
            return Double.NaN;
        }
    }

    @Override
    public int ping(Player player) {
        try {
            return player.getPing();
        } catch (Throwable ignored) {
            return -1;
        }
    }
}
