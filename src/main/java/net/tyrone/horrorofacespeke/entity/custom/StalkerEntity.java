package net.tyrone.horrorofacespeke.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import com.mojang.serialization.Dynamic;
import net.tyrone.horrorofacespeke.entity.ai.StalkerBrain;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Collection;

public class StalkerEntity extends Monster {
    private static final EntityDataAccessor<Boolean> STALKING = SynchedEntityData.defineId(StalkerEntity.class, EntityDataSerializers.BOOLEAN);

    public StalkerEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, 0.0F);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(STALKING, false);
    }

    public void setStalking(boolean stalking) {
        this.entityData.set(STALKING, stalking);
    }

    public boolean isStalking() {
        return this.entityData.get(STALKING);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new StalkPlayerGoal(this));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 35.0D);
    }

    @Override
    protected Brain.Provider<StalkerEntity> brainProvider() {
        return Brain.provider(
                StalkerBrain.MEMORY_MODULES,
                StalkerBrain.SENSOR_TYPES
        );
    }

    @Override
    protected Brain<?> makeBrain(Dynamic<?> dynamic) {
        Brain<StalkerEntity> brain = this.brainProvider().makeBrain(dynamic);
        return StalkerBrain.initialize(brain);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Brain<StalkerEntity> getBrain() {
        return (Brain<StalkerEntity>) super.getBrain();
    }

    @Override
    public void tick() {
        super.tick();
        // Remove the brain tick call for now - the Goal system will handle behavior
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        this.playSound(SoundEvents.GENERIC_DEATH, 1.0F, 1.0F);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (target instanceof Player player) {
            this.playSound(SoundEvents.PLAYER_HURT, 1.0F, 1.0F);
        }
        return super.doHurtTarget(target);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor accessor, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        return super.finalizeSpawn(accessor, difficulty, reason, data, tag);
    }

    @Override
    public boolean isPersistenceRequired() {
        return true;
    }

    // Custom stalking goal
    public static class StalkPlayerGoal extends Goal {
        private final StalkerEntity stalker;
        private Player targetPlayer;
        private int stalkTime;

        public StalkPlayerGoal(StalkerEntity stalker) {
            this.stalker = stalker;
        }

        @Override
        public boolean canUse() {
            this.targetPlayer = this.stalker.level().getNearestPlayer(this.stalker, 16.0D);
            return this.targetPlayer != null && this.stalker.hasLineOfSight(this.targetPlayer);
        }

        @Override
        public boolean canContinueToUse() {
            return this.targetPlayer != null && this.targetPlayer.isAlive() &&
                    this.stalker.distanceToSqr(this.targetPlayer) < 256.0D;
        }

        @Override
        public void start() {
            this.stalkTime = 0;
            this.stalker.setStalking(true);
        }

        @Override
        public void stop() {
            this.targetPlayer = null;
            this.stalker.setStalking(false);
            this.stalker.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (this.targetPlayer != null) {
                this.stalker.getLookControl().setLookAt(this.targetPlayer, 30.0F, 30.0F);

                double distance = this.stalker.distanceToSqr(this.targetPlayer);

                if (distance > 36.0D) { // If more than 6 blocks away
                    this.stalker.getNavigation().moveTo(this.targetPlayer, 1.0D);
                } else if (distance < 4.0D) { // If less than 2 blocks away
                    this.stalker.getNavigation().moveTo(this.targetPlayer.getX() + (this.stalker.getRandom().nextDouble() - 0.5D) * 8.0D,
                            this.targetPlayer.getY(),
                            this.targetPlayer.getZ() + (this.stalker.getRandom().nextDouble() - 0.5D) * 8.0D, 1.0D);
                }

                this.stalkTime++;
            }
        }
    }
}