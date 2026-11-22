package yhjm1208.basicstatuscommand;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * paper 线（1.18.2+）的语言加载器：文本用 MiniMessage，
 * 与 BasicTeleportCommand 现代线的语言文件同一套格式。
 */
public final class MiniLang implements Lang {

    private static final Pattern LEGACY_CODE = Pattern.compile("&[0-9a-fk-orA-FK-OR]");
    private static final Map<Character, String> LEGACY_TAGS = new HashMap<>();

    static {
        LEGACY_TAGS.put('0', "<black>");
        LEGACY_TAGS.put('1', "<dark_blue>");
        LEGACY_TAGS.put('2', "<dark_green>");
        LEGACY_TAGS.put('3', "<dark_aqua>");
        LEGACY_TAGS.put('4', "<dark_red>");
        LEGACY_TAGS.put('5', "<dark_purple>");
        LEGACY_TAGS.put('6', "<gold>");
        LEGACY_TAGS.put('7', "<gray>");
        LEGACY_TAGS.put('8', "<dark_gray>");
        LEGACY_TAGS.put('9', "<blue>");
        LEGACY_TAGS.put('a', "<green>");
        LEGACY_TAGS.put('b', "<aqua>");
        LEGACY_TAGS.put('c', "<red>");
        LEGACY_TAGS.put('d', "<light_purple>");
        LEGACY_TAGS.put('e', "<yellow>");
        LEGACY_TAGS.put('f', "<white>");
        LEGACY_TAGS.put('k', "<obfuscated>");
        LEGACY_TAGS.put('l', "<bold>");
        LEGACY_TAGS.put('m', "<strikethrough>");
        LEGACY_TAGS.put('n', "<underlined>");
        LEGACY_TAGS.put('o', "<italic>");
        LEGACY_TAGS.put('r', "");
    }

    private final BasicStatusCommand plugin;
    private final MiniMessage mini = MiniMessage.miniMessage();

    private YamlConfiguration messages = new YamlConfiguration();
    private String prefix = "";
    private String language = "zh_CN";

    public MiniLang(BasicStatusCommand plugin) {
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

        // 1.0.0 的 paper 语言文件是 & 色码；升级后先在内存里转成 MiniMessage，
        // 不覆盖玩家文件，旧文案与自定义内容继续可用。
        if (isLegacyFormat(file)) {
            convertLegacyMessages();
        }

        // 只在内存里补缺失的键，绝不覆盖玩家已改过的文件内容
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

        this.prefix = migratePrefix(messages.getString("prefix", ""));
    }

    /** 1.2.1 把默认前缀 [BSC] 改成 [BasicStatusCmd]；只改内存，保留玩家自定义颜色，不写回文件。 */
    private String migratePrefix(String rawPrefix) {
        if (rawPrefix.contains("BSC") && !rawPrefix.contains("BasicStatusCmd")) {
            plugin.getLogger().warning("检测到旧版 BSC 前缀，已在内存中改为 [BasicStatusCmd]；"
                    + "要自定义请修改 lang/" + language + ".yml 的 prefix");
            return rawPrefix.replace("BSC", "BasicStatusCmd");
        }
        return rawPrefix;
    }

    @Override
    public String language() {
        return language;
    }

    private static boolean isLegacyFormat(File file) {
        try {
            String text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            return LEGACY_CODE.matcher(text).find();
        } catch (Exception e) {
            return false;
        }
    }

    private void convertLegacyMessages() {
        for (String key : messages.getKeys(true)) {
            if (!messages.isString(key)) {
                continue;
            }
            String raw = messages.getString(key);
            if (raw == null) {
                continue;
            }
            String converted = toMiniMessage(raw);
            if (!converted.equals(raw)) {
                messages.set(key, converted);
            }
        }
        plugin.getLogger().warning("检测到旧版 & 色码语言文件，已在内存中转换为 MiniMessage；"
                + "建议按新模板重建 lang/" + language + ".yml");
    }

    private static String toMiniMessage(String input) {
        StringBuilder out = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char current = input.charAt(i);
            if (current == '&' && i + 1 < input.length()) {
                String tag = LEGACY_TAGS.get(Character.toLowerCase(input.charAt(i + 1)));
                if (tag != null) {
                    out.append(tag);
                    i++;
                    continue;
                }
            }
            out.append(current);
        }
        return out.toString();
    }

    @Override
    public String str(String key, Object... placeholders) {
        String raw = messages.getString(key);
        if (raw == null) {
            return "<red><missing:" + key + "></red>";
        }
        return fill(raw, placeholders);
    }

    @Override
    public void send(CommandSender sender, String key, Object... placeholders) {
        Component message = Component.empty()
                .append(mini.deserialize(prefix))
                .append(mini.deserialize(str(key, placeholders)));
        sender.sendMessage(message);
    }

    @Override
    public String color(String tone) {
        if ("good".equals(tone)) {
            return "<green>";
        }
        if ("warn".equals(tone)) {
            return "<yellow>";
        }
        return "<red>";
    }

    private String fill(String raw, Object... placeholders) {
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            String token = "{" + placeholders[i] + "}";
            // 占位符内容来自玩家/世界名时先转义；已翻译片段用 Raw 跳过
            String value = placeholders[i + 1] instanceof Raw
                    ? ((Raw) placeholders[i + 1]).text
                    : mini.escapeTags(String.valueOf(placeholders[i + 1]));
            raw = raw.replace(token, value);
        }
        return raw;
    }
}
