package net.messer.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(targets = "net/minecraft/block/entity/LecternBlockEntity$1")
public class LecternBlockEntityInventoryMixin {
    /**
     * @author Mystical Index
     */
    @Overwrite()
    public int size() {
        return 2;
    }

    public void getStack() // TODO DO IT HERE
}
