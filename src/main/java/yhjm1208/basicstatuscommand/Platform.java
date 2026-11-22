package yhjm1208.basicstatuscommand;

import org.bukkit.entity.Player;

/**
 * 两个产物唯一需要分叉的读数入口。
 * bukkit 版全部走 Bukkit 公开 API + 反射；paper 版直接用 Paper 的 TPS / ping 接口。
 */
public interface Platform {

    /** 日志里显示的平台名。 */
    String label();

    void start();

    void stop();

    /** 1/5/15 分钟 TPS，未知用 {@link Double#NaN}。 */
    double[] tps();

    /** 1/5/15 分钟窗口实际覆盖的秒数，未知用 {@link Double#NaN}。 */
    double[] tpsCoverage();

    /** 平均 tick 耗时（毫秒），没有该 API 时返回 NaN。 */
    double mspt();

    /** 玩家延迟（毫秒），拿不到返回 -1。 */
    int ping(Player player);
}
