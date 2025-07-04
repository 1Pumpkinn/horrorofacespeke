package net.tyrone.horrorofacespeke.entity.brain;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.tyrone.horrorofacespeke.entity.custom.StalkerEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.util.LandRandomPos;

import java.util.Optional;

public class StalkerFollowVisiblePlayer {

    public static BehaviorControl<StalkerEntity> create() {
        return BehaviorBuilder.create(stalker -> stalker.group(
                stalker.registered(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES),
                stalker.absent(MemoryModuleType.WALK_TARGET)
        ).apply((memory, world, entity, time) -> {
            Optional<LivingEntity> target = memory.get(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
                    .flatMap(list -> list.stream().filter(e -> e instanceof net.minecraft.world.entity.player.Player).findFirst());

            target.ifPresent(player -> BehaviorUtils.setWalkAndLookTargetMemories(entity, player, 1.0f, 6));
            return true;
        }));
    }
}
