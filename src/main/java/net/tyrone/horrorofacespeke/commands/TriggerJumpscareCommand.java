package net.tyrone.horrorofacespeke.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.network.chat.Component;

public class TriggerJumpscareCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("triggerjumpscare")
                .requires(source -> source.hasPermission(2)) // OP-only
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();

                    // Play scary sound
                    player.playSound(SoundEvents.WARDEN_ROAR, 1.5f, 1f);

                    // Optional: Flash message or apply effects
                    player.displayClientMessage(Component.literal("§4§lJUMPSCARE!"), true);

                    // Hurt the player slightly
                    player.hurt(DamageSource.MAGIC, 4.0F);

                    return 1;
                }));
    }
}
