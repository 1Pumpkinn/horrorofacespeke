package net.tyrone.horrorofacespeke.entity.brain;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.tyrone.horrorofacespeke.entity.custom.StalkerEntity;

import java.util.Map;

public class StalkerFollowVisiblePlayer extends Behavior<StalkerEntity> {

    public StalkerFollowVisiblePlayer() {
        super(Map.of(MemoryModuleType.NEAREST_PLAYERS, MemoryStatus.VALUE_PRESENT));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, StalkerEntity stalker) {
        return stalker.getBrain().getMemory(MemoryModuleType.NEAREST_PLAYERS)
                .map(players -> players.stream()
                        .anyMatch(player -> stalker.hasLineOfSight(player)))
                .orElse(false);
    }

    @Override
    protected void start(ServerLevel world, StalkerEntity stalker, long gameTime) {
        stalker.getBrain().getMemory(MemoryModuleType.NEAREST_PLAYERS).ifPresent(players -> {
            players.stream()
                    .filter(player -> stalker.hasLineOfSight(player))
                    .findFirst()
                    .ifPresent(player -> {
                        stalker.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
                                new WalkTarget(player, 1.0F, 6));
                        // Set look target using the player's position
                        stalker.getLookControl().setLookAt(player, 30.0F, 30.0F);
                    });
        });
    }

    @Override
    protected void stop(ServerLevel world, StalkerEntity stalker, long gameTime) {
        stalker.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
    }
}