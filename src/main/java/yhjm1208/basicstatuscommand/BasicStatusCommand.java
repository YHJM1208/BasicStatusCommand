package yhjm1208.basicstatuscommand;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import yhjm1208.basicstatuscommand.metrics.TrafficMeter;

/**
 * 基础状态命令：延迟、流量、TPS、JVM、实体、区块、磁盘与一键巡检。
 * 两个产物共用主类，差别在 {@link PlatformImpl}（读数）与 {@link LangFactory}（语言渲染）。
 */
public final class BasicStatusCommand extends JavaPlugin {

    private Lang lang;
    private Platform platform;
    private TrafficMeter traffic;
    private StatusCommand command;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        platform = PlatformImpl.create(this);
        platform.start();

        lang = LangFactory.create(this);
        lang.load();

        traffic = new TrafficMeter(this);
        traffic.start();

        command = new StatusCommand(this, platform, lang, traffic);
        for (String name : StatusCommand.commandNames()) {
            PluginCommand pluginCommand = getCommand(name);
            if (pluginCommand == null) {
                getLogger().warning("commands." + name + " 未在 plugin.yml 中声明，命令不可用");
                continue;
            }
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }

        getLogger().info("BasicStatusCmd 已启用，语言 = " + lang.language());
    }

    @Override
    public void onDisable() {
        if (traffic != null) {
            traffic.stop();
        }
        if (platform != null) {
            platform.stop();
        }
    }

    /** /basicstatuscmd reload：配置与语言文件都重新读，不重启服务器。 */
    public void reloadPlugin() {
        reloadConfig();
        lang.load();
    }

    public Lang lang() {
        return lang;
    }

    public Platform platform() {
        return platform;
    }

    public TrafficMeter traffic() {
        return traffic;
    }
}
