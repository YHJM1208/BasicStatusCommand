package yhjm1208.basicstatuscommand.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** 跨版本反射小工具：按方法名 / 字段名在类继承链上找。 */
public final class Reflect {

    private Reflect() {
    }

    public static Object call(Object target, String... methodNames) {
        if (target == null) {
            return null;
        }
        for (String name : methodNames) {
            try {
                Method method = target.getClass().getMethod(name);
                method.setAccessible(true);
                return method.invoke(target);
            } catch (NoSuchMethodException ignored) {
                // 试下一个名字
            } catch (Throwable ignored) {
                return null;
            }
        }
        return null;
    }

    public static Object field(Object target, String... fieldNames) {
        if (target == null) {
            return null;
        }
        for (String name : fieldNames) {
            Field field = findField(target.getClass(), name);
            if (field != null) {
                try {
                    field.setAccessible(true);
                    return field.get(target);
                } catch (Throwable ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    /** 在类继承链上找 int 字段，找不到返回 null。 */
    public static Integer intField(Object target, String... fieldNames) {
        if (target == null) {
            return null;
        }
        for (String name : fieldNames) {
            Field field = findField(target.getClass(), name);
            if (field != null && (field.getType() == int.class || field.getType() == Integer.class)) {
                try {
                    field.setAccessible(true);
                    return ((Number) field.get(target)).intValue();
                } catch (Throwable ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    public static Field findField(Class<?> type, String name) {
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                // 继续往父类找
            }
        }
        return null;
    }
}
