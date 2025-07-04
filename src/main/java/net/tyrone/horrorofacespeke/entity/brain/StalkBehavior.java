package net.tyrone.horrorofacespeke.entity.brain;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.player.Player;
import net.tyrone.horrorofacespeke.entity.custom.StalkerEntity;

import java.util.Map;

public class StalkBehavior extends Behavior<StalkerEntity> {
    public StalkBehavior() {
        super(Map.of(MemoryModuleType.NEAREST_VISIBLE_PLAYER, MemoryStatus.VALUE_PRESENT));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, StalkerEntity stalker) {
        return stalker.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER)
                .filter(stalker::hasLineOfSight)
                .isPresent();
    }

    @Override
    protected void start(ServerLevel world, StalkerEntity stalker, long gameTime) {
        stalker.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER).ifPresent(player -> {
            stalker.setStalking(true);
            stalker.getLookControl().setLookAt(player, 30.0F, 30.0F);
            stalker.getNavigation().moveTo(player, 1.0D);
        });
    }

    @Override
    protected void stop(ServerLevel world, StalkerEntity stalker, long gameTime) {
        stalker.setStalking(false);
        stalker.getNavigation().stop();
    }
}
