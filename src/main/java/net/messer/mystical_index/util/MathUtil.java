package net.messer.mystical_index.util;

public class MathUtil {
    public static float fixRadians(float radians) {
        return (float) Math.IEEEremainder(radians, Math.PI * 2);
    }
}
