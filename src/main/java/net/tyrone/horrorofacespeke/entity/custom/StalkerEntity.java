package net.tyrone.horrorofacespeke.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import net.tyrone.horrorofacespeke.entity.ai.StalkerAITracker;

import javax.annotation.Nullable;
import java.util.EnumSet;

public class StalkerEntity extends Monster {
    private static final EntityDataAccessor<Boolean> IS_STALKING = SynchedEntityData.defineId(StalkerEntity.class, EntityDataSerializers.BOOLEAN);
    private int teleportCooldown = 0;
    private int lastAmbientSoundTime = 0;
    private boolean aggressive = false;
    private boolean fleeing = false;
    private int aggressiveTimer = 0; // counts ticks spent hunting
    private Player lastDamagingPlayer = null;

    private StalkerAITracker tracker;

    public StalkerEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, 0.0F);
    }

    public StalkerAITracker getTracker() {
        if (tracker == null) {
            tracker = new StalkerAITracker(this);
        }
        return tracker;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(IS_STALKING, false);
    }

    public void setStalking(boolean stalking) {
        this.entityData.set(IS_STALKING, stalking);
    }

    public boolean isAggressive() {
        return aggressive;
    }

    public boolean isFleeing() {return fleeing;}

    public Player getLastDamagingPlayer(){return lastDamagingPlayer;}

    public void endFlee(){fleeing=false;}

    private void becomeAggressive(Player player) {
        aggressive = true;
        fleeing = false;               // cannot flee while hunting
        aggressiveTimer = 0;
        this.setTarget(player);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new net.tyrone.horrorofacespeke.entity.ai.goals.StalkerFleeGoal(this));
        this.goalSelector.addGoal(3, new net.tyrone.horrorofacespeke.entity.ai.goals.StalkerStalkGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new net.tyrone.horrorofacespeke.entity.ai.goals.StalkerAttackGoal(this, 1.4D, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 0.5D);
    }

    @Override
    public void tick() {
        super.tick();
        
        if (!this.level.isClientSide) {
            if (this.teleportCooldown > 0) {
                this.teleportCooldown--;            
            }

            //========  AGGRESSIVE/HUNT MODE MANAGEMENT  =========
            if (aggressive) {
                aggressiveTimer++;
                LivingEntity tgt = this.getTarget();
                // stop hunting after 30 s or if target lost for long
                if (aggressiveTimer > 600 || tgt == null || !tgt.isAlive() || this.distanceToSqr(tgt) > 1024 && !this.hasLineOfSight(tgt)) {
                    aggressive = false;
                    this.setTarget(null);
                }
            }
            
            // Update tracker
            this.getTracker().tick();
            
            // Play ambient sounds occasionally
            if (this.tickCount - this.lastAmbientSoundTime > 200 && this.random.nextInt(3) == 0) {
                this.playAmbientSound();
                this.lastAmbientSoundTime = this.tickCount;
            }
        }
    }
    
    @Override
    public void remove(RemovalReason reason) {
        // Clean up tracker when entity is removed
        if (!this.level.isClientSide) {
            StalkerAITracker.unregisterStalker(this);
        }
        super.remove(reason);
    }

    @Override
    public void playAmbientSound() {
        this.playSound(SoundEvents.ENDERMAN_STARE, 0.5F, 0.5F);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (super.doHurtTarget(target)) {
            this.playSound(SoundEvents.PLAYER_HURT, 1.0F, 1.0F);
            return true;
        }
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean res = super.hurt(source, amount);
        if (!level.isClientSide && source.getEntity() instanceof Player player) {
            if (this.random.nextBoolean()) {
                // === FLEE MODE ===
                this.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.INVISIBILITY, 200, 0, false, false));
                fleeing = true;
                aggressive = false;      // ensure we don't keep attacking while fleeing
                this.setTarget(null);
                lastDamagingPlayer = player;
            } else {
                // === HUNT MODE ===
                becomeAggressive(player);
            }
        }
        return res;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        this.playSound(SoundEvents.GENERIC_DEATH, 1.0F, 1.0F);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData spawnData, @Nullable CompoundTag dataTag) {
        return super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);
    }

    @Override
    public boolean isPersistenceRequired() {
        return true;
    }

    // Custom stalking goal with enhanced stealth and mining tunnel behavior
    static class StalkerStalkGoal extends Goal {
        private final StalkerEntity stalker;
        private Player targetPlayer;
        private int teleportCooldown = 0;
        private int hideTimer = 0;
        private BlockPos lastHidingSpot = null;
        private static final int MIN_STALK_DISTANCE = 8;
        private static final int MAX_STALK_DISTANCE = 20;
        private static final int MIN_TELEPORT_DELAY = 100;
        private static final int MAX_TELEPORT_DELAY = 300;
        private static final int HIDE_CHECK_INTERVAL = 20;

        public StalkerStalkGoal(StalkerEntity stalker) {
            this.stalker = stalker;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            this.targetPlayer = this.stalker.level.getNearestPlayer(this.stalker, 32.0D);
            return this.targetPlayer != null && this.stalker.hasLineOfSight(this.targetPlayer);
        }

        @Override
        public boolean canContinueToUse() {
            return this.targetPlayer != null && 
                   this.targetPlayer.isAlive() && 
                   this.stalker.distanceToSqr(this.targetPlayer) < 1024.0D; // 32 blocks squared
        }

        @Override
        public void start() {
            this.stalker.setStalking(true);
            this.teleportCooldown = this.stalker.random.nextInt(100) + 100;
            this.hideTimer = 0;
        }

        @Override
        public void stop() {
            this.stalker.setStalking(false);
            this.targetPlayer = null;
            this.lastHidingSpot = null;
            this.stalker.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (this.targetPlayer == null) return;

            this.stalker.getLookControl().setLookAt(this.targetPlayer, 30.0F, 30.0F);
            double distance = this.stalker.distanceToSqr(this.targetPlayer);

            // Normal stalking behavior
            if (this.hideTimer-- <= 0) {
                this.hideTimer = HIDE_CHECK_INTERVAL;
                findHidingSpot();
            }

            // Handle movement based on distance
            if (distance < MIN_STALK_DISTANCE * MIN_STALK_DISTANCE) {
                // Too close, try to teleport away
                this.stalker.getNavigation().stop();
                if (this.teleportCooldown <= 0) {
                    this.teleportToHidingSpot(true);
                    this.teleportCooldown = this.stalker.random.nextInt(MAX_TELEPORT_DELAY - MIN_TELEPORT_DELAY) + MIN_TELEPORT_DELAY;
                }
            } else if (distance > MAX_STALK_DISTANCE * MAX_STALK_DISTANCE) {
                // Too far, move closer
                moveToHidingSpot();
            }

            if (this.teleportCooldown > 0) {
                this.teleportCooldown--;
            }
        }

        private void findHidingSpot() {
            if (this.targetPlayer == null) return;

            // Try to find a position behind cover relative to the player
            Vec3 playerPos = this.targetPlayer.position();
            Vec3 toStalker = this.stalker.position().subtract(playerPos).normalize();
            
            // Calculate position behind cover
            double distance = MIN_STALK_DISTANCE + (MAX_STALK_DISTANCE - MIN_STALK_DISTANCE) * this.stalker.random.nextDouble();
            Vec3 targetPos = playerPos.add(
                toStalker.x * distance,
                0,
                toStalker.z * distance
            );

            // Find a valid position near the target
            BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos(
                (int)targetPos.x,
                (int)playerPos.y,
                (int)targetPos.z
            );

            // Adjust Y position to find a valid spot
            while (checkPos.getY() > this.stalker.level.getMinBuildHeight() + 1) {
                if (isGoodHidingSpot(checkPos)) {
                    this.lastHidingSpot = checkPos.immutable();
                    moveToHidingSpot();
                    return;
                }
                checkPos.move(0, -1, 0);
            }
        }

        private boolean isGoodHidingSpot(BlockPos pos) {
            // Check if the spot is near cover (trees, tall grass, etc.)
            BlockState state = this.stalker.level.getBlockState(pos);
            boolean nearCover = false;
            
            // Check adjacent blocks for cover
            for (Direction dir : Direction.values()) {
                BlockPos checkPos = pos.relative(dir);
                BlockState checkState = this.stalker.level.getBlockState(checkPos);
                if (checkState.is(Blocks.TALL_GRASS) || 
                    checkState.is(Blocks.OAK_LEAVES) || 
                    checkState.is(Blocks.SPRUCE_LEAVES) ||
                    checkState.is(Blocks.BIRCH_LEAVES) ||
                    checkState.is(Blocks.JUNGLE_LEAVES) ||
                    checkState.is(Blocks.ACACIA_LEAVES) ||
                    checkState.is(Blocks.DARK_OAK_LEAVES) ||
                    checkState.is(Blocks.MANGROVE_LEAVES) ||
                    checkState.is(Blocks.AZALEA_LEAVES) ||
                    checkState.is(Blocks.FLOWERING_AZALEA_LEAVES)) {
                    nearCover = true;
                    break;
                }
            }

            return nearCover && 
                   this.stalker.level.isEmptyBlock(pos) && 
                   this.stalker.level.isEmptyBlock(pos.above()) &&
                   !this.stalker.level.isEmptyBlock(pos.below());
        }

        private void moveToHidingSpot() {
            if (this.lastHidingSpot != null) {
                this.stalker.getNavigation().moveTo(
                    this.lastHidingSpot.getX() + 0.5,
                    this.lastHidingSpot.getY(),
                    this.lastHidingSpot.getZ() + 0.5,
                    0.8D
                );
            }
        }

        private void teleportToHidingSpot(boolean playSound) {
            if (this.lastHidingSpot != null) {
                this.stalker.teleportTo(
                    this.lastHidingSpot.getX() + 0.5,
                    this.lastHidingSpot.getY(),
                    this.lastHidingSpot.getZ() + 0.5
                );
                if (playSound) {
                    this.stalker.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
                }
            } else {
                findHidingSpot();
            }
        }
    }
}