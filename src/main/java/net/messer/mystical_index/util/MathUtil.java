package net.messer.mystical_index.util;

public class MathUtil {
    public static float fixRadians(float radians) {
        return (float) Math.IEEEremainder(radians, Math.PI * 2);
    }

    // From https://stackoverflow.com/a/54141102
    public static String shortNumberFormat(int count) {
        if (count < 1000) return "" + count;
        int exp = (int) (Math.log(count) / Math.log(1000));
        String value = "" + (int) (count / Math.pow(1000, exp));
        return String.format("%s%c", value, "kMBTPE".charAt(exp - 1));
    }
}
