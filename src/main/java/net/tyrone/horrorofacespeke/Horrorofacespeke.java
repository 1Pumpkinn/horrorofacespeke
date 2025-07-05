package net.tyrone.horrorofacespeke;

import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.tyrone.horrorofacespeke.entity.ModEntities;
import net.tyrone.horrorofacespeke.entity.acespeke.AcespekeEntity;
import net.tyrone.horrorofacespeke.entity.acespeke.AcespekeEntityType;
import net.tyrone.horrorofacespeke.entity.acespeke.ai.AcespekeAIManager;
import net.tyrone.horrorofacespeke.client.renderer.AcespekeRenderer;
import net.tyrone.horrorofacespeke.item.ModItems;
import org.slf4j.Logger;

@Mod(Horrorofacespeke.MODID)
public class Horrorofacespeke {
    public static final String MODID = "horrorofacespeke";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Horrorofacespeke() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register entities
        AcespekeEntityType.ENTITIES.register(modEventBus);

        // Register items (spawn eggs)
        ModItems.ITEMS.register(modEventBus);

        // Register setup methods
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onEntityAttributeCreation);
        
        // Register for server stopping event
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopping);
    }
    
    private void onServerStopping(net.minecraftforge.event.server.ServerStoppingEvent event) {
        AcespekeAIManager.onServerStopping();
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Common setup for Horrorofacespeke");
    }
    
    private void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        // Entity attributes are registered in ModEvents.onAttributeCreate
    }

    // Handles Forge commands
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Register entity renderers
            EntityRenderers.register(AcespekeEntityType.ACESPEKE.get(), AcespekeRenderer::new);
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeEvents {
        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event) {
        }
    }

    // Handles attribute registration (correct way for Forge 1.19.2)
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent
        public static void onAttributeCreate(EntityAttributeCreationEvent event) {
            event.put(ModEntities.ACESPEKE.get(), AcespekeEntity.createAttributes().build());
        }
    }

    // Handles client-only setup like renderers
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("Client setup for Horrorofacespeke");
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
                event.registerEntityRenderer(ModEntities.ACESPEKE.get(), AcespekeRenderer::new);
        }
    }
}