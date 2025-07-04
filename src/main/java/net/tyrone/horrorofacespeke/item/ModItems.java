package net.tyrone.horrorofacespeke.item;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.tyrone.horrorofacespeke.Horrorofacespeke;
import net.tyrone.horrorofacespeke.entity.ModEntities;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Horrorofacespeke.MODID);

    public static final RegistryObject<Item> STALKER_SPAWN_EGG = ITEMS.register("stalker_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.STALKER, 0x2F2F2F, 0x000000,
                    new Item.Properties().tab(ModCreativeTab.HORROR_TAB)));
}

