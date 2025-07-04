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
import net.tyrone.horrorofacespeke.entity.ai.StalkerHideGoal;
import net.tyrone.horrorofacespeke.entity.ai.StalkerStalkGoal;
import net.tyrone.horrorofacespeke.entity.ai.StalkerTeleportGoal;

public class StalkerEntity extends Monster {
    private int jumpscareCounter = 0;
    private boolean isInvisible = false;
    private int invisibilityTimer = 0;
    private int panicTimer = 0;
    private boolean isPanicking = false;

    public StalkerEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setCanPickUpLoot(false);
    }

    @Override
    protected void registerGoals() {
        // Priority 0: Don't drown
        this.goalSelector.addGoal(0, new FloatGoal(this));

        // Priority 1: Teleport goal (highest priority for repositioning)
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
                .add(Attributes.MOVEMENT_SPEED, 0.32) // Even faster movement
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.FOLLOW_RANGE, 35.0) // Longer detection range
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7); // Higher knockback resistance
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
                    net.minecraft.world.effect.MobEffects.BLINDNESS, 80, 0)); // 4 seconds
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 60, 2)); // 3 seconds
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.CONFUSION, 100, 0)); // 5 seconds nausea

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
            teleportRandomly();
        }
    }

    public void teleportRandomly() {
        for (int i = 0; i < 20; i++) {
            double x = this.getX() + (this.random.nextDouble() - 0.5) * 40.0;
            double y = this.getY() + (this.random.nextInt(16) - 8);
            double z = this.getZ() + (this.random.nextDouble() - 0.5) * 40.0;

            if (this.randomTeleport(x, y, z, true)) {
                this.level.playSound(null, this.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                        this.getSoundSource(), 1.0F, 1.0F);

                // Become invisible briefly after teleporting
                this.setInvisible(true);
                isInvisible = true;
                invisibilityTimer = 60; // 3 seconds
                break;
            }
        }
    }

    public void enterPanicMode() {
        isPanicking = true;
        panicTimer = 100; // 5 seconds

        // Chance to teleport immediately
        if (this.random.nextFloat() < 0.8f) {
            teleportRandomly();
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.level.isClientSide) {
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

            Player closest = this.level.getNearestPlayer(this, 25);
            if (closest != null) {
                double distanceToPlayer = this.distanceToSqr(closest);
                boolean playerCanSeeMe = closest.hasLineOfSight(this) &&
                        isPlayerLookingAt(closest, 0.7);

                // Enhanced invisibility logic
                if (!isInvisible && this.random.nextFloat() < 0.002f) {
                    if (!playerCanSeeMe || distanceToPlayer > 100) {
                        this.setInvisible(true);
                        isInvisible = true;
                        invisibilityTimer = 120; // 6 seconds
                    }
                }

                // Panic if player gets too close while being stared at
                if (playerCanSeeMe && distanceToPlayer < 25 && !isPanicking) {
                    if (this.random.nextFloat() < 0.3f) {
                        enterPanicMode();
                    }
                }

                // Make more frequent scary sounds when close
                if (distanceToPlayer < 400 && this.random.nextFloat() < 0.003f) {
                    this.playSound(getAmbientSound(), 0.6f, 0.7f);
                }
            }
        }
    }

    private boolean isPlayerLookingAt(Player player, double precision) {
        net.minecraft.world.phys.Vec3 playerLook = player.getViewVector(1.0F).normalize();
        net.minecraft.world.phys.Vec3 playerToEntity = this.position().subtract(player.position()).normalize();
        return playerLook.dot(playerToEntity) > precision;
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
        // Take less damage and always teleport away when hurt
        if (super.hurt(source, amount * 0.6f)) {
            // Always teleport when hurt
            teleportRandomly();

            // Enter panic mode
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
    protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
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
}