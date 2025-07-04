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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.entity.ai.sensing.SensorType;
import com.mojang.serialization.Dynamic;
import net.tyrone.horrorofacespeke.entity.ai.StalkerBrain;

import javax.annotation.Nullable;
import java.util.List;

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
        // We're using Brain now — no goals here
    }

    @Override
    protected Brain.Provider<StalkerEntity> brainProvider() {
        return Brain.provider(
                List.of(MemoryModuleType.NEAREST_PLAYERS),
                List.of(SensorType.NEAREST_PLAYERS)
        );
    }

    @Override
    protected Brain<?> makeBrain(Dynamic<?> dynamic) {
        Brain<StalkerEntity> brain = this.brainProvider().makeBrain(dynamic);
        return StalkerBrain.initialize(brain);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            getBrain().tick((ServerLevel) level(), this);
            StalkerBrain.tick(this);
        }
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
}
