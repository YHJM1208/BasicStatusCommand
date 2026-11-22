package yhjm1208.basicstatuscommand;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * bukkit 线（1.8.8 – 1.18.1）的语言加载器：文本用 {@code &} 色码，不引 Adventure。
 * 与 BasicTeleportCommand 的 legacy 线同一套写法。
 */
public final class LegacyLang implements Lang {

    private final BasicStatusCommand plugin;

    private YamlConfiguration messages = new YamlConfiguration();
    private String prefix = "";
    private String language = "zh_CN";

    public LegacyLang(BasicStatusCommand plugin) {
        this.plugin = plugin;
    }

    @Override
    public void load() {
        String configured = plugin.getConfig().getString("language", "zh_CN");
        String resource = "lang/" + configured + ".yml";
        if (plugin.getResource(resource) == null) {
            plugin.getLogger().warning("未知的语言 " + configured + "，回退到 zh_CN");
            configured = "zh_CN";
            resource = "lang/" + configured + ".yml";
        }
        this.language = configured;

        File file = new File(plugin.getDataFolder(), resource);
        if (!file.exists()) {
            plugin.saveResource(resource, false);
        }
        this.messages = YamlConfiguration.loadConfiguration(file);

        // 只在内存里补缺失的键，不覆盖玩家改过的内容
        try (InputStream in = plugin.getResource(resource)) {
            if (in != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(in, StandardCharsets.UTF_8));
                messages.setDefaults(defaults);
                messages.options().copyDefaults(true);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("读取语言文件失败：" + e.getMessage());
        }

        this.prefix = messages.getString("prefix", "");
    }

    @Override
    public String language() {
        return language;
    }

    @Override
    public String str(String key, Object... placeholders) {
        String raw = messages.getString(key);
        if (raw == null) {
            return "&c<missing:" + key + ">";
        }
        return fill(raw, placeholders);
    }

    @Override
    public void send(CommandSender sender, String key, Object... placeholders) {
        sender.sendMessage(translate(prefix + str(key, placeholders)));
    }

    @Override
    public void line(CommandSender sender, String key, Object... placeholders) {
        sender.sendMessage(translate(str(key, placeholders)));
    }

    @Override
    public String color(String tone) {
        if ("good".equals(tone)) {
            return "&a";
        }
        if ("warn".equals(tone)) {
            return "&e";
        }
        return "&c";
    }

    private static String fill(String raw, Object... placeholders) {
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            String token = "{" + placeholders[i] + "}";
            String value = placeholders[i + 1] instanceof Raw
                    ? ((Raw) placeholders[i + 1]).text
                    : sanitize(String.valueOf(placeholders[i + 1]));
            raw = raw.replace(token, value);
        }
        return raw;
    }

    /** 玩家名/世界名里如果混进 § 会被当成色码，直接去掉。 */
    private static String sanitize(String value) {
        return value.replace("§", "");
    }

    private static String translate(String raw) {
        return ChatColor.translateAlternateColorCodes('&', raw);
    }
}
