package net.messer.mystical_index.item.custom.base_books;


import net.messer.util.MysticalUtil;
import net.minecraft.item.ItemStack;


public class BaseGeneratingBook extends BaseStorageBook{
    public BaseGeneratingBook(Settings settings) {
        super(settings);
    }

    public void updateUseTime(ItemStack stack, long time){
        // The component copy has to go back onto the stack; mutating the fetched compound alone
        // would be discarded and the write silently lost.
        MysticalUtil.editCustomData(stack, compound -> compound.putLong("lastUsedTime", time));
    }
}
