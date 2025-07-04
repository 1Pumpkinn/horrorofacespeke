package net.tyrone.horrorofacespeke.entity.ai;

import com.google.common.collect.ImmutableList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.schedule.Schedule;
import net.minecraft.world.entity.player.Player;
import net.tyrone.horrorofacespeke.entity.brain.StalkBehavior;
import net.tyrone.horrorofacespeke.entity.custom.StalkerEntity;

import java.util.List;
import java.util.Set;

public class StalkerBrain {

    public static Brain<StalkerEntity> initialize(Brain<StalkerEntity> brain) {
        brain.setCoreActivities(Set.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.setSchedule(Schedule.EMPTY);

        brain.addActivity(Activity.CORE, 0, ImmutableList.of(
                new LookAtTargetSink(45, 90),
                new MoveToTargetSink()
        ));

        brain.addActivity(Activity.IDLE, 10, ImmutableList.of(
                new StalkBehavior()
        ));

        brain.useDefaultActivity();
        return brain;
    }

    public static void tick(StalkerEntity stalker) {
        // Simplified - let the Goal system handle the behavior
        // Brain system can be complex in Forge 1.19.2
    }

    public static final List<MemoryModuleType<?>> MEMORY_MODULES = List.of(
            MemoryModuleType.NEAREST_PLAYERS,
            MemoryModuleType.LOOK_TARGET,
            MemoryModuleType.WALK_TARGET,
            MemoryModuleType.PATH
    );

    public static final List<SensorType<? extends Sensor<? super StalkerEntity>>> SENSOR_TYPES = List.of(
            SensorType.NEAREST_PLAYERS,
            SensorType.HURT_BY
    );
}