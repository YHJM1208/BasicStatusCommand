package yhjm1208.basicstatuscommand;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import yhjm1208.basicstatuscommand.metrics.Jvm;
import yhjm1208.basicstatuscommand.metrics.TrafficMeter;
import yhjm1208.basicstatuscommand.metrics.WorldStats;
import yhjm1208.basicstatuscommand.metrics.WorldStats.EntitySnapshot;
import yhjm1208.basicstatuscommand.util.Fmt;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 14 条状态命令的直接分发与输出。
 * /ping、/cpu、/check 等直接触发；/basicstatuscmd 只做命令索引与 reload。
 */
public final class StatusCommand implements CommandExecutor, TabCompleter {

    private static final String ROOT = "basicstatuscmd";

    private static final String[][] SUBS = {
            {"ping", "basicstatuscmd.ping"},
            {"traffic", "basicstatuscmd.admin"},
            {"top", "basicstatuscmd.top"},
            {"tps", "basicstatuscmd.tps"},
            {"cpu", "basicstatuscmd.admin"},
            {"mem", "basicstatuscmd.admin"},
            {"gc", "basicstatuscmd.admin"},
            {"thread", "basicstatuscmd.admin"},
            {"entity", "basicstatuscmd.admin"},
            {"chunk", "basicstatuscmd.admin"},
            {"info", "basicstatuscmd.info"},
            {"env", "basicstatuscmd.admin"},
            {"disk", "basicstatuscmd.admin"},
            {"check", "basicstatuscmd.admin"},
    };

    private static final int OK = 0;
    private static final int WARN = 1;
    private static final int FAIL = 2;

    private final BasicStatusCommand plugin;
    private final Platform platform;
    private final Lang lang;
    private final TrafficMeter traffic;

    public StatusCommand(BasicStatusCommand plugin, Platform platform, Lang lang, TrafficMeter traffic) {
        this.plugin = plugin;
        this.platform = platform;
        this.lang = lang;
        this.traffic = traffic;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);

        if (ROOT.equals(name)) {
            if (args.length == 0) {
                help(sender);
                return true;
            }
            if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("basicstatuscmd.reload")) {
                    lang.send(sender, "general.no-permission");
                    return true;
                }
                plugin.reloadPlugin();
                lang.send(sender, "general.reloaded");
                return true;
            }
            lang.send(sender, "general.unknown-arg", "sub", args[0]);
            return true;
        }

        if (!isKnown(name)) {
            lang.send(sender, "general.unknown-arg", "sub", name);
            return true;
        }
        // 管理类命令的权限节点就是 basicstatuscmd.admin，其余各查自己的节点
        if (!canUse(sender, name)) {
            lang.send(sender, "general.no-permission");
            return true;
        }

        switch (name) {
            case "ping":
                ping(sender, args);
                break;
            case "traffic":
                traffic(sender);
                break;
            case "top":
                top(sender);
                break;
            case "tps":
                tps(sender);
                break;
            case "cpu":
                cpu(sender);
                break;
            case "mem":
                memory(sender);
                break;
            case "gc":
                gc(sender);
                break;
            case "thread":
                threads(sender);
                break;
            case "entity":
                entity(sender);
                break;
            case "chunk":
                chunk(sender);
                break;
            case "info":
                info(sender);
                break;
            case "env":
                env(sender);
                break;
            case "disk":
                disk(sender);
                break;
            case "check":
                check(sender);
                break;
            default:
                lang.send(sender, "general.unknown-arg", "sub", name);
                break;
        }
        return true;
    }

    /** 直接命令名列表，主类按它注册执行器。 */
    public static String[] commandNames() {
        String[] names = new String[SUBS.length + 1];
        names[0] = ROOT;
        for (int i = 0; i < SUBS.length; i++) {
            names[i + 1] = SUBS[i][0];
        }
        return names;
    }

    private boolean isKnown(String name) {
        for (String[] entry : SUBS) {
            if (entry[0].equals(name)) {
                return true;
            }
        }
        return false;
    }

    private boolean canUse(CommandSender sender, String node) {
        return sender.hasPermission("basicstatuscmd.admin") || sender.hasPermission("basicstatuscmd." + node);
    }

    private void help(CommandSender sender) {
        lang.send(sender, "help.header", "name", "BasicStatusCmd",
                "version", plugin.getDescription().getVersion());
        lang.send(sender, "help.entry", "command", ROOT,
                "desc", Lang.rawValue(lang.str("desc." + ROOT)));
        for (String[] entry : SUBS) {
            if (!canUse(sender, entry[1])) {
                continue;
            }
            lang.send(sender, "help.entry", "command", entry[0],
                    "desc", Lang.rawValue(lang.str("desc." + entry[0])));
        }
        if (sender.hasPermission("basicstatuscmd.reload")) {
            lang.send(sender, "help.reload", "command", ROOT,
                    "desc", Lang.rawValue(lang.str("desc.reload")));
        }
    }

    // ---------------------------------------------------------------- 网络

    private void ping(CommandSender sender, String[] args) {
        Player target;
        if (args.length >= 1) {
            target = plugin.getServer().getPlayerExact(args[0]);
            if (target == null) {
                lang.send(sender, "general.player-not-found", "player", args[0]);
                return;
            }
        } else {
            if (!(sender instanceof Player)) {
                lang.send(sender, "general.console-needs-player");
                return;
            }
            target = (Player) sender;
        }

        int ping = platform.ping(target);
        if (ping < 0) {
            lang.send(sender, "ping.entry-unknown", "player", target.getName());
        } else {
            lang.send(sender, "ping.entry-" + quality(ping), "player", target.getName(), "ping", ping);
        }
        lang.send(sender, "ping.world", "world", target.getWorld().getName());
    }

    private String quality(int ping) {
        int good = plugin.getConfig().getInt("ping.good-ms", 80);
        int medium = plugin.getConfig().getInt("ping.medium-ms", 200);
        if (ping <= good) {
            return "good";
        }
        return ping <= medium ? "medium" : "bad";
    }

    private void traffic(CommandSender sender) {
        final int seconds = Math.max(1, plugin.getConfig().getInt("traffic.sample-seconds", 3));
        lang.send(sender, "traffic.sampling", "seconds", seconds);

        final long[] start = TrafficMeter.snapshot();
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            if (!plugin.isEnabled()) {
                return;
            }
            long[] end = TrafficMeter.snapshot();
            double span = (end[0] - start[0]) / 1_000_000_000.0;
            long inBytes = Math.max(0L, end[1] - start[1]);
            long outBytes = Math.max(0L, end[2] - start[2]);
            long inPackets = Math.max(0L, end[3] - start[3]);
            long outPackets = Math.max(0L, end[4] - start[4]);

            final long inRate = span > 0 ? Math.round(inBytes / span) : 0L;
            final long outRate = span > 0 ? Math.round(outBytes / span) : 0L;
            final long inPktRate = span > 0 ? Math.round(inPackets / span) : 0L;
            final long outPktRate = span > 0 ? Math.round(outPackets / span) : 0L;
            final int channels = traffic.channels();

            Bukkit.getScheduler().runTask(plugin, () -> {
                lang.send(sender, "traffic.line-window", "seconds", seconds, "channels", channels);
                if (channels == 0) {
                    if (Bukkit.getOnlinePlayers().isEmpty()) {
                        lang.send(sender, "traffic.no-players");
                    } else {
                        lang.send(sender, "traffic.no-channel");
                    }
                }
                lang.send(sender, "traffic.line-down", "rate", Fmt.rate(inRate), "packets", inPktRate);
                lang.send(sender, "traffic.line-up", "rate", Fmt.rate(outRate), "packets", outPktRate);
                lang.send(sender, "traffic.line-total",
                        "in", Fmt.bytes(end[1]), "out", Fmt.bytes(end[2]));
            });
        }, seconds * 20L);
    }

    private void top(CommandSender sender) {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (players.isEmpty()) {
            lang.send(sender, "top.empty");
            return;
        }
        players.sort((a, b) -> Integer.compare(platform.ping(b), platform.ping(a)));

        int limit = Math.min(Math.max(1, plugin.getConfig().getInt("top.size", 5)), players.size());
        for (int i = 0; i < limit; i++) {
            Player player = players.get(i);
            int ping = platform.ping(player);
            if (ping < 0) {
                lang.send(sender, "top.entry-unknown", "rank", i + 1, "player", player.getName());
            } else {
                lang.send(sender, "top.entry-" + quality(ping), "rank", i + 1,
                        "player", player.getName(), "ping", ping);
            }
        }
    }

    // ---------------------------------------------------------------- 性能

    private void tps(CommandSender sender) {
        double[] tps = platform.tps();
        double[] coverage = platform.tpsCoverage();
        if (tps == null) {
            lang.send(sender, "tps.native-missing");
            return;
        }

        String[] labels = {"tps.label-1m", "tps.label-5m", "tps.label-15m"};
        int[] windows = {60, 300, 900};
        boolean any = false;
        for (int i = 0; i < labels.length; i++) {
            double value = i < tps.length ? tps[i] : Double.NaN;
            if (Double.isNaN(value)) {
                continue;
            }
            any = true;
            double covered = coverage != null && i < coverage.length ? coverage[i] : Double.NaN;
            if (!Double.isNaN(covered) && covered < windows[i] - 1.0) {
                lang.send(sender, "tps.warming", "label", Lang.rawValue(lang.str(labels[i])),
                        "seconds", Math.max(0, Math.round(covered)));
            } else {
                lang.send(sender, "tps.line", "label", Lang.rawValue(lang.str(labels[i])),
                        "color", Lang.rawValue(lang.color(Fmt.toneForTps(value))), "value", Fmt.tps(value));
            }
        }
        if (!any) {
            lang.send(sender, "tps.collecting");
        }

        double mspt = platform.mspt();
        if (!Double.isNaN(mspt)) {
            String tone = mspt >= 50.0 ? "bad" : (mspt >= 25.0 ? "warn" : "good");
            lang.send(sender, "tps.mspt", "color", Lang.rawValue(lang.color(tone)), "value", Fmt.ms(mspt));
        }
    }

    private void cpu(CommandSender sender) {
        double process = Jvm.processCpuLoad();
        if (process < 0) {
            lang.send(sender, "cpu.unavailable", "label", Lang.rawValue(lang.str("cpu.label-process")));
        } else {
            lang.send(sender, "cpu.process", "color", Lang.rawValue(lang.color(Fmt.toneForPercent(process))),
                    "value", String.format(Locale.ROOT, "%.1f", process));
        }
        double system = Jvm.systemCpuLoad();
        if (system < 0) {
            lang.send(sender, "cpu.unavailable", "label", Lang.rawValue(lang.str("cpu.label-system")));
        } else {
            lang.send(sender, "cpu.system", "color", Lang.rawValue(lang.color(Fmt.toneForPercent(system))),
                    "value", String.format(Locale.ROOT, "%.1f", system));
        }
        lang.send(sender, "cpu.cores", "value", Jvm.processors());
        double load = Jvm.systemLoadAverage();
        if (load >= 0) {
            lang.send(sender, "cpu.load", "value", String.format(Locale.ROOT, "%.2f", load));
        }
    }

    private void memory(CommandSender sender) {
        long[] heap = Jvm.heap();
        long max = heap[2] > 0 ? heap[2] : heap[1];
        double percent = max > 0 ? heap[0] * 100.0 / max : 0.0;
        lang.send(sender, "mem.heap", "color", Lang.rawValue(lang.color(Fmt.toneForPercent(percent))),
                "used", Fmt.bytes(heap[0]), "max", Fmt.bytes(max),
                "percent", String.format(Locale.ROOT, "%.1f", percent));
        lang.send(sender, "mem.nonheap", "used", Fmt.bytes(Jvm.nonHeapUsed()));

        long[] physical = Jvm.physicalMemory();
        if (physical[0] > 0) {
            long used = physical[0] - physical[1];
            double physicalPercent = used * 100.0 / physical[0];
            lang.send(sender, "mem.physical", "used", Fmt.bytes(used),
                    "total", Fmt.bytes(physical[0]),
                    "percent", String.format(Locale.ROOT, "%.1f", physicalPercent));
        }
    }

    private void gc(CommandSender sender) {
        long count = 0L;
        long time = 0L;
        for (Jvm.Gc collector : Jvm.gc()) {
            lang.send(sender, "gc.entry", "name", collector.name,
                    "count", collector.count, "time", collector.timeMs);
            count += collector.count;
            time += collector.timeMs;
        }
        long uptime = Math.max(1L, Jvm.uptimeMillis());
        double percent = time * 100.0 / uptime;
        lang.send(sender, "gc.total", "count", count, "time", time,
                "percent", String.format(Locale.ROOT, "%.2f", percent));
    }

    private void threads(CommandSender sender) {
        int[] values = Jvm.threads();
        lang.send(sender, "thread.live", "value", values[0]);
        lang.send(sender, "thread.peak", "value", values[1]);
        lang.send(sender, "thread.daemon", "value", values[2]);
        lang.send(sender, "thread.started", "value", values[3]);
    }

    // ---------------------------------------------------------------- 生态

    private void entity(CommandSender sender) {
        EntitySnapshot snapshot = WorldStats.entities();
        lang.send(sender, "entity.total", "value", snapshot.total);
        lang.send(sender, "entity.category",
                "monster", value(snapshot.categories, WorldStats.MONSTER),
                "animals", value(snapshot.categories, WorldStats.ANIMALS),
                "water", value(snapshot.categories, WorldStats.WATER),
                "villager", value(snapshot.categories, WorldStats.VILLAGER),
                "other", value(snapshot.categories, WorldStats.OTHER)
                        + value(snapshot.categories, WorldStats.AMBIENT));

        int max = Math.max(1, plugin.getConfig().getInt("entity.max-worlds", 8));
        int shown = 0;
        for (WorldStats.WorldEntities world : snapshot.worlds) {
            if (shown >= max) {
                lang.send(sender, "entity.more-worlds", "count", snapshot.worlds.size() - shown);
                break;
            }
            shown++;
            int monster = value(world.categories, WorldStats.MONSTER);
            int animals = value(world.categories, WorldStats.ANIMALS);
            lang.send(sender, "entity.world", "world", world.world, "total", world.total,
                    "monster", monster, "animals", animals, "other", world.total - monster - animals);
        }
    }

    private void chunk(CommandSender sender) {
        WorldStats.ChunkSnapshot snapshot = WorldStats.chunks();
        lang.send(sender, "chunk.total", "value", snapshot.total);
        for (WorldStats.WorldChunks world : snapshot.worlds) {
            lang.send(sender, "chunk.world", "world", world.world, "value", world.loaded);
        }
    }

    // ---------------------------------------------------------------- 系统

    private void info(CommandSender sender) {
        lang.send(sender, "info.server", "name", Bukkit.getName());
        lang.send(sender, "info.version", "version", Bukkit.getVersion());
        lang.send(sender, "info.bukkit", "version", Bukkit.getBukkitVersion());
        lang.send(sender, "info.players",
                "online", Bukkit.getOnlinePlayers().size(), "max", Bukkit.getMaxPlayers());
        lang.send(sender, "info.worlds",
                "worlds", Bukkit.getWorlds().size(),
                "plugins", Bukkit.getPluginManager().getPlugins().length);
        lang.send(sender, "info.uptime", "value", Fmt.duration(Jvm.uptimeMillis()));
        lang.send(sender, "info.java",
                "version", System.getProperty("java.version", "?"),
                "vendor", System.getProperty("java.vendor", "?"));
    }

    private void env(CommandSender sender) {
        lang.send(sender, "env.java",
                "version", System.getProperty("java.version", "?"),
                "vendor", System.getProperty("java.vendor", "?"));
        lang.send(sender, "env.vm",
                "name", System.getProperty("java.vm.name", "?"),
                "version", System.getProperty("java.vm.version", "?"));
        lang.send(sender, "env.os",
                "name", System.getProperty("os.name", "?"),
                "arch", System.getProperty("os.arch", "?"),
                "version", System.getProperty("os.version", "?"));
        lang.send(sender, "env.cores", "value", Jvm.processors());
        lang.send(sender, "env.misc",
                "encoding", System.getProperty("file.encoding", "?"),
                "timezone", System.getProperty("user.timezone", "?"));
        lang.send(sender, "env.dir", "value", serverDir());
    }

    private void disk(CommandSender sender) {
        long warnMb = Math.max(1L, plugin.getConfig().getLong("check.disk-free-warn-mb", 2048L));
        for (File root : File.listRoots()) {
            long total = root.getTotalSpace();
            if (total <= 0) {
                continue;
            }
            long free = root.getUsableSpace();
            long freeMb = free / (1024L * 1024L);
            String tone = freeMb < warnMb / 2 ? "bad" : (freeMb < warnMb ? "warn" : "good");
            lang.send(sender, "disk.root", "root", root.getPath(),
                    "free", Fmt.bytes(free), "total", Fmt.bytes(total),
                    "color", Lang.rawValue(lang.color(tone)), "percent", String.format(Locale.ROOT, "%.1f",
                            (total - free) * 100.0 / total));
        }
        lang.send(sender, "disk.data", "path", serverDir());
    }

    /** 服务端根目录，去掉 getAbsolutePath() 末尾的 {@code \.} 。 */
    private String serverDir() {
        File dir = plugin.getServer().getWorldContainer().getAbsoluteFile();
        return dir.toPath().normalize().toFile().getAbsolutePath();
    }

    // ---------------------------------------------------------------- 巡检

    private void check(CommandSender sender) {
        int checks = 0;
        int issues = 0;

        // TPS
        double[] tps = platform.tps();
        double tps1 = tps == null ? Double.NaN : tps[0];
        checks++;
        if (Double.isNaN(tps1)) {
            checkItem(sender, OK, lang.str("check.label-tps"), lang.str("check.collecting"), null);
        } else {
            double warn = plugin.getConfig().getDouble("check.tps-warn", 18.0);
            double fail = plugin.getConfig().getDouble("check.tps-fail", 10.0);
            int status = tps1 < fail ? FAIL : (tps1 < warn ? WARN : OK);
            issues += status == OK ? 0 : 1;
            checkItem(sender, status, lang.str("check.label-tps"), Fmt.tps(tps1),
                    lang.str("check.threshold", "value", Fmt.tps(warn)));
        }

        // 内存
        long[] heap = Jvm.heap();
        long max = heap[2] > 0 ? heap[2] : heap[1];
        double memoryPercent = max > 0 ? heap[0] * 100.0 / max : 0.0;
        checks++;
        double memoryWarn = plugin.getConfig().getDouble("check.memory-warn-percent", 85.0);
        double memoryFail = plugin.getConfig().getDouble("check.memory-fail-percent", 95.0);
        int memoryStatus = memoryPercent >= memoryFail ? FAIL : (memoryPercent >= memoryWarn ? WARN : OK);
        issues += memoryStatus == OK ? 0 : 1;
        checkItem(sender, memoryStatus, lang.str("check.label-memory"),
                String.format(Locale.ROOT, "%.1f%%", memoryPercent),
                lang.str("check.threshold", "value", String.format(Locale.ROOT, "%.0f%%", memoryWarn)));

        // 磁盘
        File dataFolder = plugin.getDataFolder();
        File root = dataFolder.getAbsoluteFile().toPath().getRoot() == null
                ? dataFolder.getAbsoluteFile() : dataFolder.getAbsoluteFile().toPath().getRoot().toFile();
        long freeMb = root.getUsableSpace() / (1024L * 1024L);
        checks++;
        long diskWarn = plugin.getConfig().getLong("check.disk-free-warn-mb", 2048L);
        int diskStatus = freeMb < diskWarn / 2 ? FAIL : (freeMb < diskWarn ? WARN : OK);
        issues += diskStatus == OK ? 0 : 1;
        checkItem(sender, diskStatus, lang.str("check.label-disk"),
                Fmt.bytes(root.getUsableSpace()) + " " + lang.str("check.label-free"),
                lang.str("check.threshold", "value", Fmt.bytes(diskWarn * 1024L * 1024L)));

        // 区块
        WorldStats.ChunkSnapshot chunks = WorldStats.chunks();
        checks++;
        int chunkWarn = plugin.getConfig().getInt("check.chunk-warn", 10000);
        int chunkStatus = chunks.total > chunkWarn ? WARN : OK;
        issues += chunkStatus == OK ? 0 : 1;
        checkItem(sender, chunkStatus, lang.str("check.label-chunk"), String.valueOf(chunks.total),
                lang.str("check.threshold", "value", chunkWarn));

        // 实体
        EntitySnapshot entities = WorldStats.entities();
        checks++;
        int entityWarn = plugin.getConfig().getInt("check.entity-warn", 5000);
        int entityStatus = entities.total > entityWarn ? WARN : OK;
        issues += entityStatus == OK ? 0 : 1;
        checkItem(sender, entityStatus, lang.str("check.label-entity"), String.valueOf(entities.total),
                lang.str("check.threshold", "value", entityWarn));

        // 线程
        int[] threads = Jvm.threads();
        checks++;
        int threadWarn = plugin.getConfig().getInt("check.thread-warn", 400);
        int threadStatus = threads[0] > threadWarn ? WARN : OK;
        issues += threadStatus == OK ? 0 : 1;
        checkItem(sender, threadStatus, lang.str("check.label-thread"), String.valueOf(threads[0]),
                lang.str("check.threshold", "value", threadWarn));

        // GC 占比
        long gcTime = 0L;
        for (Jvm.Gc collector : Jvm.gc()) {
            gcTime += collector.timeMs;
        }
        double gcPercent = gcTime * 100.0 / Math.max(1L, Jvm.uptimeMillis());
        checks++;
        double gcWarn = plugin.getConfig().getDouble("check.gc-warn-percent", 20.0);
        int gcStatus = gcPercent > gcWarn ? WARN : OK;
        issues += gcStatus == OK ? 0 : 1;
        checkItem(sender, gcStatus, lang.str("check.label-gc"),
                String.format(Locale.ROOT, "%.2f%%", gcPercent),
                lang.str("check.threshold", "value", String.format(Locale.ROOT, "%.0f%%", gcWarn)));

        if (issues == 0) {
            lang.send(sender, "check.summary-ok", "count", checks);
        } else {
            lang.send(sender, "check.summary-warn", "count", issues);
        }
    }

    private void checkItem(CommandSender sender, int status, String label, String value, String note) {
        String icon = status == OK ? lang.str("check.ok")
                : status == WARN ? lang.str("check.warn") : lang.str("check.fail");
        String text = value;
        if (note != null) {
            text = value + " " + note;
        }
        // value / note 里可能已经带平台标记（label、threshold 是先翻译过的），用 Raw 避免二次转义
        lang.send(sender, "check.item", "status", Lang.rawValue(icon),
                "label", Lang.rawValue(label), "value", Lang.rawValue(text));
    }

    private static int value(Map<String, Integer> map, String key) {
        Integer value = map.get(key);
        return value == null ? 0 : value;
    }

    // ---------------------------------------------------------------- Tab

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (ROOT.equals(name)) {
            return args.length == 1
                    ? filter(args[0], Collections.singletonList("reload"))
                    : Collections.emptyList();
        }
        if ("ping".equals(name) && args.length == 1 && canUse(sender, "ping")) {
            List<String> names = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                names.add(player.getName());
            }
            return filter(args[0], names);
        }
        return Collections.emptyList();
    }

    private static List<String> filter(String prefix, List<String> pool) {
        List<String> result = new ArrayList<>();
        String lower = prefix.toLowerCase(Locale.ROOT);
        for (String candidate : pool) {
            if (candidate.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(candidate);
            }
        }
        return result;
    }
}
