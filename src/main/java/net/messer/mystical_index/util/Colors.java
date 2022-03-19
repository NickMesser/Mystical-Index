package net.messer.mystical_index.util;

import net.minecraft.util.Formatting;

public class Colors {
    public static Formatting colorByRatio(double ratio) {
        return ratio < 0.75 ? Formatting.GREEN :
                ratio == 1 ? Formatting.RED : Formatting.GOLD;
    }
}
