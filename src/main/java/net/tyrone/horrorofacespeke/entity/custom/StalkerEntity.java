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
                .add(Attributes.MOVEMENT_SPEED, 0.28) // Faster than default
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.FOLLOW_RANGE, 32.0) // Can detect players from far away
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5); // Harder to knock back
    }

    public void jumpscare(Player player) {
        if (player != null && !player.level.isClientSide) {
            // Play multiple scary sounds
            player.playSound(SoundEvents.WARDEN_ROAR, 2.0f, 0.8f);
            player.playSound(SoundEvents.SCULK_SHRIEKER_SHRIEK, 1.5f, 1.2f);

            // Deal damage
            player.hurt(DamageSource.mobAttack(this), 6.0f);

            // Apply brief blindness/slowness effect
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.BLINDNESS, 60, 0)); // 3 seconds
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40, 2)); // 2 seconds

            // Knockback player
            double dx = player.getX() - this.getX();
            double dz = player.getZ() - this.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance > 0) {
                dx /= distance;
                dz /= distance;
                player.push(dx * 2.0, 0.5, dz * 2.0);
            }

            jumpscareCounter++;

            // Teleport away after jumpscare
            teleportRandomly();
        }
    }

    private void teleportRandomly() {
        for (int i = 0; i < 16; i++) {
            double x = this.getX() + (this.random.nextDouble() - 0.5) * 32.0;
            double y = this.getY() + (this.random.nextInt(16) - 8);
            double z = this.getZ() + (this.random.nextDouble() - 0.5) * 32.0;

            if (this.randomTeleport(x, y, z, true)) {
                this.level.playSound(null, this.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                        this.getSoundSource(), 1.0F, 1.0F);
                break;
            }
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.level.isClientSide) {
            // Handle invisibility
            if (invisibilityTimer > 0) {
                invisibilityTimer--;
                if (invisibilityTimer <= 0) {
                    this.setInvisible(false);
                    isInvisible = false;
                }
            }

            // Random chance to become invisible when not seen
            Player closest = this.level.getNearestPlayer(this, 20);
            if (closest != null && !isInvisible && this.random.nextFloat() < 0.001f) {
                if (!closest.hasLineOfSight(this)) {
                    this.setInvisible(true);
                    isInvisible = true;
                    invisibilityTimer = 100; // 5 seconds
                }
            }

            // Make scary ambient sounds occasionally
            if (closest != null && this.distanceToSqr(closest) < 256 && this.random.nextFloat() < 0.002f) {
                this.playSound(getAmbientSound(), 0.5f, 0.8f);
            }
        }
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
        // Take less damage and teleport away when hurt
        if (super.hurt(source, amount * 0.7f)) {
            if (this.random.nextFloat() < 0.6f) {
                teleportRandomly();
            }
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

    // Make it harder to detect with name tags
    @Override
    public boolean shouldShowName() {
        return false;
    }

    // Don't make footstep sounds when invisible
    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        if (!isInvisible) {
            super.playStepSound(pos, state);
        }
    }
}