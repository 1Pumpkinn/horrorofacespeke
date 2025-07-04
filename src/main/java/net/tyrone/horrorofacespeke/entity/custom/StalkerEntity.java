package net.tyrone.horrorofacespeke.entity.custom;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.state.BlockState;
import net.tyrone.horrorofacespeke.entity.ai.StalkerHideGoal;
import net.tyrone.horrorofacespeke.entity.ai.StalkerStalkGoal;
import net.tyrone.horrorofacespeke.entity.ai.StalkerTeleportGoal;

public class StalkerEntity extends Monster {
    private int jumpscareCounter = 0;
    private boolean isInvisible = false;
    private int invisibilityTimer = 0;
    private int panicTimer = 0;
    private boolean isPanicking = false;
    private Player trackedPlayer = null;
    private int playerTrackingTimer = 0;
    private boolean wasBeingWatched = false;

    public StalkerEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setCanPickUpLoot(false);
    }

    @Override
    protected void registerGoals() {
        // Priority 0: Don't drown
        this.goalSelector.addGoal(0, new FloatGoal(this));

        // Priority 1: Teleport goal (only when being watched)
        this.goalSelector.addGoal(1, new StalkerTeleportGoal(this));

        // Priority 2: Stalk and attack goal
        this.goalSelector.addGoal(2, new StalkerStalkGoal(this));

        // Priority 3: Hide and peek goal
        this.goalSelector.addGoal(3, new StalkerHideGoal(this));

        // Priority 8: Look at player (low priority, only when not doing other things)
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 12.0f));

        // Priority 9: Random look around (lowest priority)
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.FOLLOW_RANGE, 64.0) // Much longer detection range
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7);
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.level.isClientSide) {
            // Always track the nearest player
            updatePlayerTracking();

            // Handle panic mode
            if (isPanicking && panicTimer > 0) {
                panicTimer--;
                if (panicTimer <= 0) {
                    isPanicking = false;
                }
            }

            // Handle invisibility
            if (invisibilityTimer > 0) {
                invisibilityTimer--;
                if (invisibilityTimer <= 0) {
                    this.setInvisible(false);
                    isInvisible = false;
                }
            }

            // Check if being watched and handle teleportation
            if (trackedPlayer != null) {
                boolean currentlyBeingWatched = isBeingWatchedByPlayer();

                // If player starts looking at us, trigger teleport
                if (currentlyBeingWatched && !wasBeingWatched) {
                    if (this.random.nextFloat() < 0.8f) { // 80% chance to teleport when first spotted
                        teleportBehindPlayer();
                    }
                }

                wasBeingWatched = currentlyBeingWatched;

                // Make occasional scary sounds when close
                double distanceToPlayer = this.distanceToSqr(trackedPlayer);
                if (distanceToPlayer < 400 && this.random.nextFloat() < 0.003f) {
                    this.playSound(getAmbientSound(), 0.6f, 0.7f);
                }
            }
        }
    }

    private void updatePlayerTracking() {
        // Always find the nearest player within a large range
        Player nearestPlayer = this.level.getNearestPlayer(this, 64.0);

        if (nearestPlayer != null) {
            trackedPlayer = nearestPlayer;
            playerTrackingTimer = 0;
        } else if (playerTrackingTimer < 200) { // Keep tracking for 10 seconds even if out of range
            playerTrackingTimer++;
        } else {
            trackedPlayer = null;
        }
    }

    private boolean isBeingWatchedByPlayer() {
        if (trackedPlayer == null) return false;

        // Check if player has line of sight and is looking at us
        boolean hasLineOfSight = trackedPlayer.hasLineOfSight(this);
        boolean isLookingAt = isPlayerLookingAt(trackedPlayer, 0.6);

        return hasLineOfSight && isLookingAt;
    }

    private boolean isPlayerLookingAt(Player player, double precision) {
        Vec3 playerLook = player.getViewVector(1.0F).normalize();
        Vec3 playerToEntity = this.position().subtract(player.position()).normalize();
        return playerLook.dot(playerToEntity) > precision;
    }

    public void jumpscare(Player player) {
        if (player != null && !player.level.isClientSide) {
            // Play multiple scary sounds
            player.playSound(SoundEvents.WARDEN_ROAR, 2.0f, 0.8f);
            player.playSound(SoundEvents.SCULK_SHRIEKER_SHRIEK, 1.5f, 1.2f);

            // Deal damage
            player.hurt(DamageSource.mobAttack(this), 6.0f);

            // Apply stronger effects
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.BLINDNESS, 80, 0));
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.CONFUSION, 100, 0));

            // Stronger knockback
            double dx = player.getX() - this.getX();
            double dz = player.getZ() - this.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance > 0) {
                dx /= distance;
                dz /= distance;
                player.push(dx * 2.5, 0.7, dz * 2.5);
            }

            jumpscareCounter++;

            // Always teleport away after jumpscare
            teleportBehindPlayer();
        }
    }

    public void teleportBehindPlayer() {
        if (trackedPlayer == null) return;

        Vec3 playerPos = trackedPlayer.position();
        Vec3 playerLook = trackedPlayer.getViewVector(1.0F).normalize();

        // Try to teleport behind the player in a hidden location
        for (int attempts = 0; attempts < 25; attempts++) {
            double distance = 12 + this.random.nextDouble() * 15; // 12-27 blocks away
            double angle = this.random.nextDouble() * Math.PI * 2; // Full circle around player

            // Prefer positions behind the player
            double behindBonus = Math.PI + this.random.nextDouble() * Math.PI - Math.PI / 2; // Behind player ±90 degrees
            double finalAngle = angle * 0.3 + behindBonus * 0.7; // Weighted toward behind

            double x = playerPos.x + Math.cos(finalAngle) * distance;
            double z = playerPos.z + Math.sin(finalAngle) * distance;

            // Try different Y levels
            for (int yOffset = -3; yOffset <= 3; yOffset++) {
                double y = playerPos.y + yOffset;
                BlockPos targetPos = new BlockPos(x, y, z);

                if (isValidTeleportLocation(targetPos) && !playerCanSeePosition(targetPos)) {
                    // Play teleport sound at old position
                    this.level.playSound(null, this.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                            this.getSoundSource(), 0.5F, 1.0F);

                    // Teleport
                    this.teleportTo(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5);

                    // Play teleport sound at new position (quieter)
                    this.level.playSound(null, targetPos, SoundEvents.ENDERMAN_TELEPORT,
                            this.getSoundSource(), 0.3F, 0.8F);

                    // Become briefly invisible after teleporting
                    this.setInvisible(true);
                    isInvisible = true;
                    invisibilityTimer = 40; // 2 seconds
                    return;
                }
            }
        }
    }

    private boolean isValidTeleportLocation(BlockPos pos) {
        if (pos.getY() < -64 || pos.getY() > 320) return false;

        BlockState groundState = this.level.getBlockState(pos.below());
        BlockState feetState = this.level.getBlockState(pos);
        BlockState headState = this.level.getBlockState(pos.above());

        return groundState.isSolidRender(this.level, pos.below()) &&
                !feetState.isSolidRender(this.level, pos) &&
                !headState.isSolidRender(this.level, pos.above());
    }

    private boolean playerCanSeePosition(BlockPos pos) {
        if (trackedPlayer == null) return false;

        Vec3 playerEye = trackedPlayer.getEyePosition(1.0F);
        Vec3 targetPos = Vec3.atCenterOf(pos);

        return this.level.clip(new net.minecraft.world.level.ClipContext(
                playerEye, targetPos,
                net.minecraft.world.level.ClipContext.Block.VISUAL,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                trackedPlayer)).getType() == net.minecraft.world.phys.HitResult.Type.MISS;
    }

    public void teleportRandomly() {
        // Override to use the better teleport method
        teleportBehindPlayer();
    }

    public void enterPanicMode() {
        isPanicking = true;
        panicTimer = 100;

        // Always teleport when panicking
        teleportBehindPlayer();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SKELETON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.WARDEN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WARDEN_DEATH;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (super.hurt(source, amount * 0.6f)) {
            // Always teleport when hurt
            teleportBehindPlayer();
            enterPanicMode();
            return true;
        }
        return false;
    }

    @Override
    public MobType getMobType() {
        return MobType.UNDEAD;
    }

    @Override
    public boolean canBeLeashed(Player player) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected boolean canRide(net.minecraft.world.entity.Entity entity) {
        return false;
    }

    @Override
    public boolean shouldShowName() {
        return false;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (!isInvisible) {
            super.playStepSound(pos, state);
        }
    }

    // Getter methods for AI goals
    public boolean isPanicking() {
        return isPanicking;
    }

    public boolean isCurrentlyInvisible() {
        return isInvisible;
    }

    public Player getTrackedPlayer() {
        return trackedPlayer;
    }

    public boolean isBeingWatched() {
        return isBeingWatchedByPlayer();
    }
}