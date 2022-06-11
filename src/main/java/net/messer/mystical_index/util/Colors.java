package net.messer.mystical_index.util;

import net.minecraft.util.Formatting;

import java.util.List;

public class Colors {
    public static int fromRGB(int r, int g, int b) {
        return ((r & 0xFF) << 16) |
                ((g & 0xFF) << 8) |
                (b & 0xFF);
    }

    public static int getRed(int color) {
        return color >> 16 & 0xFF;
    }

    public static int getGreen(int color) {
        return color >> 8 & 0xFF;
    }

    public static int getBlue(int color) {
        return color & 0xFF;
    }

    public static int mixColors(int color1, int color2) {
        int red = (getRed(color1) + getRed(color2)) / 2;
        int green = (getGreen(color1) + getGreen(color2)) / 2;
        int blue = (getBlue(color1) + getBlue(color2)) / 2;
        return fromRGB(red, green, blue);
    }

    public static int mixColors(List<Integer> colors) {
        int red = 0;
        int green = 0;
        int blue = 0;
        for (int color : colors) {
            red += getRed(color);
            green += getGreen(color);
            blue += getBlue(color);
        }
        return fromRGB(red / colors.size(), green / colors.size(), blue / colors.size());
    }

    public static Formatting colorByRatio(double ratio) {
        return ratio < 0.75 ? Formatting.GREEN :
                ratio == 1 ? Formatting.RED : Formatting.GOLD;
    }
}
