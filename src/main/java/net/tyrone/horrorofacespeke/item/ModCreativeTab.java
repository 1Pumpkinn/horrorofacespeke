package net.tyrone.horrorofacespeke.item;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModCreativeTab {
    public static final CreativeModeTab HORROR_TAB = new CreativeModeTab("horror_tab") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(ModItems.ACESPEKE_SPAWN_EGG.get());
        }
    };
}