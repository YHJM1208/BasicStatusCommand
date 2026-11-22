package yhjm1208.basicstatuscommand.util;

import java.util.Locale;

/** 数值格式化：字节、百分比、TPS、时长。输出保持语言无关，文案在语言文件里。 */
public final class Fmt {

    private static final String[] BYTE_UNITS = {"B", "KB", "MB", "GB", "TB", "PB"};

    private Fmt() {
    }

    public static String bytes(long value) {
        if (value < 1024L) {
            return value + " B";
        }
        double size = value;
        int unit = 0;
        while (size >= 1024.0 && unit < BYTE_UNITS.length - 1) {
            size /= 1024.0;
            unit++;
        }
        return String.format(Locale.ROOT, "%.1f %s", size, BYTE_UNITS[unit]);
    }

    public static String rate(long bytesPerSecond) {
        return bytes(bytesPerSecond) + "/s";
    }

    public static String percent(double value) {
        return String.format(Locale.ROOT, "%.1f%%", value);
    }

    public static String tps(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    public static String ms(double value) {
        if (value < 10.0) {
            return String.format(Locale.ROOT, "%.2f", value);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    /** 紧凑时长：2d 3h 5m 10s；不足一秒按 0s。 */
    public static String duration(long millis) {
        long seconds = millis / 1000L;
        long days = seconds / 86400L;
        seconds %= 86400L;
        long hours = seconds / 3600L;
        seconds %= 3600L;
        long minutes = seconds / 60L;
        seconds %= 60L;

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("d ");
        }
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0) {
            sb.append(minutes).append("m ");
        }
        if (sb.length() == 0 || (days == 0 && hours == 0)) {
            sb.append(seconds).append("s");
        }
        return sb.toString().trim();
    }

    /** 0/1/2 -> 低/中/高 三档语义色，交给 {@code Lang#color(String)} 按平台渲染。 */
    public static String toneForPercent(double percent) {
        if (percent < 60.0) {
            return "good";
        }
        if (percent < 85.0) {
            return "warn";
        }
        return "bad";
    }

    public static String toneForTps(double tps) {
        if (tps >= 18.0) {
            return "good";
        }
        if (tps >= 15.0) {
            return "warn";
        }
        return "bad";
    }
}
