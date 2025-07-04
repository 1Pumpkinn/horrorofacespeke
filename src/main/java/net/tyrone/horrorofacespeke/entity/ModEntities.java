package net.tyrone.horrorofacespeke.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.tyrone.horrorofacespeke.Horrorofacespeke;
import net.tyrone.horrorofacespeke.entity.custom.StalkerEntity;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Horrorofacespeke.MODID);

    public static final RegistryObject<EntityType<StalkerEntity>> STALKER = ENTITY_TYPES.register("stalker",
            () -> EntityType.Builder.of(StalkerEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f) // Player size
                    .clientTrackingRange(1000)
                    .setTrackingRange(100000)
                    .canSpawnFarFromPlayer()
                    .build("stalker"));
}
