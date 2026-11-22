package yhjm1208.basicstatuscommand.metrics;

import io.netty.channel.Channel;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * 玩家连接流量统计：给每个玩家 channel 挂 {@link TrafficHandler}，累计收发字节与包数。
 * 统计范围是玩家通道（登录后的数据），不含服务端列表 ping。
 */
public final class TrafficMeter implements Listener {

    private static final String HANDLER_NAME = "basicstatuscmd_traffic";

    private static final LongAdder IN_BYTES = new LongAdder();
    private static final LongAdder OUT_BYTES = new LongAdder();
    private static final LongAdder IN_PACKETS = new LongAdder();
    private static final LongAdder OUT_PACKETS = new LongAdder();

    private final JavaPlugin plugin;
    // 命令的采样在异步线程读，join/quit 在主线程写
    private final Map<UUID, Channel> channels = new ConcurrentHashMap<>();

    public TrafficMeter(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            inject(player);
        }
    }

    public void stop() {
        HandlerList.unregisterAll(this);
        for (Channel channel : channels.values()) {
            try {
                if (channel.pipeline().get(HANDLER_NAME) != null) {
                    channel.pipeline().remove(HANDLER_NAME);
                }
            } catch (Throwable ignored) {
                // 连接已经关闭
            }
        }
        channels.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        inject(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        channels.remove(event.getPlayer().getUniqueId());
    }

    private void inject(Player player) {
        try {
            Channel channel = ChannelAccess.find(player);
            if (channel == null) {
                return;
            }
            if (channel.pipeline().get(HANDLER_NAME) == null) {
                channel.pipeline().addFirst(HANDLER_NAME, new TrafficHandler());
            }
            channels.put(player.getUniqueId(), channel);
        } catch (Throwable ignored) {
            // 该版本结构不认识，交给命令层提示「未接入通道」
        }
    }

    public int channels() {
        return channels.size();
    }

    static void addIn(int bytes) {
        IN_BYTES.add(bytes);
        IN_PACKETS.increment();
    }

    static void addOut(int bytes) {
        OUT_BYTES.add(bytes);
        OUT_PACKETS.increment();
    }

    /** {纳秒时间戳, 入站字节, 出站字节, 入站包数, 出站包数} */
    public static long[] snapshot() {
        return new long[]{System.nanoTime(), IN_BYTES.sum(), OUT_BYTES.sum(), IN_PACKETS.sum(), OUT_PACKETS.sum()};
    }
}
