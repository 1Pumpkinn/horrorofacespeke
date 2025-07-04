package net.tyrone.horrorofacespeke.entity.ai;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.tyrone.horrorofacespeke.entity.custom.StalkerEntity;

import java.util.EnumSet;

public class StalkerStalkGoal extends Goal {
    private final StalkerEntity stalker;
    private Player targetPlayer;
    private int stalkCooldown;
    private int jumpscareBuildup;
    private int observedTicks;
    private int retreatCooldown;
    private boolean isPreparingJumpscare;
    private boolean isRetreating;
    private static final int JUMPSCARE_BUILDUP_TIME = 60; // 3 seconds
    private static final int MAX_OBSERVED_TIME = 40; // 2 seconds before reacting
    private static final int RETREAT_DURATION = 80; // 4 seconds

    public StalkerStalkGoal(StalkerEntity stalker) {
        this.stalker = stalker;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        this.targetPlayer = this.stalker.level.getNearestPlayer(this.stalker, 25.0);
        return this.targetPlayer != null && !this.targetPlayer.isCreative() &&
                !this.targetPlayer.isSpectator() && stalkCooldown <= 0;
    }

    @Override
    public boolean canContinueToUse() {
        return this.targetPlayer != null && this.targetPlayer.isAlive() &&
                this.stalker.distanceToSqr(this.targetPlayer) < 625.0; // 25 blocks
    }

    @Override
    public void start() {
        this.stalkCooldown = 0;
        this.jumpscareBuildup = 0;
        this.observedTicks = 0;
        this.retreatCooldown = 0;
        this.isPreparingJumpscare = false;
        this.isRetreating = false;
    }

    @Override
    public void stop() {
        this.stalkCooldown = 100; // 5 second cooldown
        this.isPreparingJumpscare = false;
        this.isRetreating = false;
        this.jumpscareBuildup = 0;
        this.observedTicks = 0;
        this.retreatCooldown = 0;
    }

    @Override
    public void tick() {
        if (this.targetPlayer == null) return;

        if (stalkCooldown > 0) {
            stalkCooldown--;
            return;
        }

        if (retreatCooldown > 0) {
            retreatCooldown--;
        }

        double distanceToPlayer = this.stalker.distanceToSqr(this.targetPlayer);
        boolean playerCanSeeMe = canPlayerSeeEntity();

        // Handle being observed
        if (playerCanSeeMe) {
            observedTicks++;

            // If observed for too long, react aggressively
            if (observedTicks > MAX_OBSERVED_TIME && !isRetreating) {
                triggerObservedReaction();
                return;
            }

            // Freeze when being looked at
            this.stalker.getNavigation().stop();

            // Stare back menacingly
            this.stalker.getLookControl().setLookAt(targetPlayer, 30.0F, 30.0F);

        } else {
            // Reset observation counter when not being watched
            observedTicks = Math.max(0, observedTicks - 2);
        }

        // Handle retreat behavior
        if (isRetreating) {
            if (retreatCooldown <= 0) {
                isRetreating = false;
            } else {
                executeRetreat();
                return;
            }
        }

        // Normal stalking behavior when not being observed
        if (!playerCanSeeMe && !isRetreating) {
            // Very close range - jumpscare preparation
            if (distanceToPlayer < 9.0) { // 3 blocks
                if (!isPreparingJumpscare) {
                    isPreparingJumpscare = true;
                    jumpscareBuildup = 0;
                    this.stalker.getNavigation().stop();
                }

                jumpscareBuildup++;
                this.stalker.getLookControl().setLookAt(targetPlayer, 30.0F, 30.0F);

                if (jumpscareBuildup >= JUMPSCARE_BUILDUP_TIME) {
                    this.stalker.jumpscare(targetPlayer);
                    this.stop();
                }
            } else if (distanceToPlayer < 36.0) { // 6 blocks
                // Close range - move in for attack
                this.stalker.getNavigation().moveTo(targetPlayer, 1.2);
                isPreparingJumpscare = false;
                jumpscareBuildup = 0;

                // Random chance for immediate attack when close
                if (distanceToPlayer < 16.0 && stalker.getRandom().nextFloat() < 0.05f) {
                    this.stalker.jumpscare(targetPlayer);
                    this.stop();
                }
            } else {
                // Medium range - handled by other goals
                isPreparingJumpscare = false;
                jumpscareBuildup = 0;
            }
        }
    }

    private void triggerObservedReaction() {
        isRetreating = true;
        retreatCooldown = RETREAT_DURATION;

        // 70% chance to apply blindness effect
        if (stalker.getRandom().nextFloat() < 0.7f) {
            targetPlayer.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.BLINDNESS, 60, 0)); // 3 seconds

            // Play scary sound
            targetPlayer.playSound(net.minecraft.sounds.SoundEvents.WARDEN_AMBIENT, 1.0f, 0.6f);
        }

        // 50% chance to teleport away immediately
        if (stalker.getRandom().nextFloat() < 0.5f) {
            stalker.teleportRandomly();
            this.stop();
        }
    }

    private void executeRetreat() {
        Vec3 playerPos = targetPlayer.position();
        Vec3 retreatDirection = stalker.position().subtract(playerPos).normalize();

        // Add some randomness to retreat direction
        double angle = stalker.getRandom().nextDouble() * Math.PI / 2 - Math.PI / 4; // ±45 degrees
        double newX = retreatDirection.x * Math.cos(angle) - retreatDirection.z * Math.sin(angle);
        double newZ = retreatDirection.x * Math.sin(angle) + retreatDirection.z * Math.cos(angle);

        Vec3 retreatPos = stalker.position().add(newX * 20, 0, newZ * 20);
        this.stalker.getNavigation().moveTo(retreatPos.x, retreatPos.y, retreatPos.z, 1.5);

        // 30% chance to teleport away during retreat
        if (stalker.getRandom().nextFloat() < 0.3f) {
            stalker.teleportRandomly();
            this.stop();
        }
    }

    private boolean canPlayerSeeEntity() {
        return targetPlayer.hasLineOfSight(stalker) &&
                isPlayerLookingAt(targetPlayer, stalker, 0.6);
    }

    private boolean isPlayerLookingAt(Player player, StalkerEntity entity, double precision) {
        Vec3 playerLook = player.getViewVector(1.0F).normalize();
        Vec3 playerToEntity = entity.position().subtract(player.position()).normalize();
        return playerLook.dot(playerToEntity) > precision;
    }
}