package yhjm1208.basicstatuscommand.metrics;

import io.netty.channel.Channel;
import org.bukkit.entity.Player;
import yhjm1208.basicstatuscommand.util.Reflect;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * 从 Bukkit 玩家对象摸到 Netty {@link Channel}。
 * 先按各版本常见的字段链找，找不到再从 handle 做一次浅层广度搜索（按字段类型匹配，不依赖字段名）。
 */
public final class ChannelAccess {

    private static final String[][] KNOWN_PATHS = {
            // 1.20.5+ Mojang 映射 / 1.18 前后的 Spigot 映射
            {"connection", "connection", "channel"},
            {"playerConnection", "networkManager", "channel"},
            // 少数版本少一层
            {"connection", "channel"},
            {"playerConnection", "channel"},
    };

    private ChannelAccess() {
    }

    public static Channel find(Player player) {
        Object handle = Reflect.call(player, "getHandle");
        if (handle == null) {
            return null;
        }
        for (String[] path : KNOWN_PATHS) {
            Object current = handle;
            for (String name : path) {
                current = Reflect.field(current, name);
                if (current == null) {
                    break;
                }
            }
            if (current instanceof Channel) {
                return (Channel) current;
            }
        }
        return search(handle);
    }

    private static Channel search(Object handle) {
        Deque<Object> queue = new ArrayDeque<>();
        Map<Object, Integer> depth = new IdentityHashMap<>();
        queue.add(handle);
        depth.put(handle, 0);

        int visited = 0;
        while (!queue.isEmpty() && visited < 300) {
            Object current = queue.poll();
            int currentDepth = depth.get(current);
            visited++;

            for (Class<?> type = current.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
                for (Field field : type.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers())) {
                        continue;
                    }
                    Class<?> fieldType = field.getType();
                    if (fieldType.isPrimitive() || fieldType.isArray() || fieldType == String.class) {
                        continue;
                    }
                    try {
                        field.setAccessible(true);
                        Object value = field.get(current);
                        if (value instanceof Channel) {
                            return (Channel) value;
                        }
                        if (value == null || currentDepth >= 3 || skip(value.getClass())) {
                            continue;
                        }
                        if (!depth.containsKey(value)) {
                            depth.put(value, currentDepth + 1);
                            queue.add(value);
                        }
                    } catch (Throwable ignored) {
                        // 某些字段不可访问，跳过
                    }
                }
            }
        }
        return null;
    }

    private static boolean skip(Class<?> type) {
        String name = type.getName();
        if (name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("sun.")
                || name.startsWith("jdk.") || name.startsWith("com.mojang.authlib.")
                || name.startsWith("io.netty.") || name.startsWith("org.bukkit.")) {
            return true;
        }
        String simple = type.getSimpleName();
        return simple.contains("Level") || simple.contains("World") || simple.contains("MinecraftServer");
    }
}
