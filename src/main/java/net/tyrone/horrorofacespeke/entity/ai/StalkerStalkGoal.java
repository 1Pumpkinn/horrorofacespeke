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
    private boolean isPreparingJumpscare;
    private static final int JUMPSCARE_BUILDUP_TIME = 60; // 3 seconds

    public StalkerStalkGoal(StalkerEntity stalker) {
        this.stalker = stalker;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        this.targetPlayer = this.stalker.level.getNearestPlayer(this.stalker, 20.0);
        return this.targetPlayer != null && !this.targetPlayer.isCreative() &&
                !this.targetPlayer.isSpectator() && stalkCooldown <= 0;
    }

    @Override
    public boolean canContinueToUse() {
        return this.targetPlayer != null && this.targetPlayer.isAlive() &&
                this.stalker.distanceToSqr(this.targetPlayer) < 400.0; // 20 blocks
    }

    @Override
    public void start() {
        this.stalkCooldown = 0;
        this.jumpscareBuildup = 0;
        this.isPreparingJumpscare = false;
    }

    @Override
    public void stop() {
        this.stalkCooldown = 100; // 5 second cooldown
        this.isPreparingJumpscare = false;
        this.jumpscareBuildup = 0;
    }

    @Override
    public void tick() {
        if (this.targetPlayer == null) return;

        if (stalkCooldown > 0) {
            stalkCooldown--;
            return;
        }

        double distanceToPlayer = this.stalker.distanceToSqr(this.targetPlayer);
        boolean playerCanSeeMe = canPlayerSeeEntity();

        // Very close range - jumpscare preparation
        if (distanceToPlayer < 9.0 && !playerCanSeeMe) { // 3 blocks
            if (!isPreparingJumpscare) {
                isPreparingJumpscare = true;
                jumpscareBuildup = 0;
                // Stop moving and prepare for jumpscare
                this.stalker.getNavigation().stop();
            }

            jumpscareBuildup++;

            // Look at player menacingly
            this.stalker.getLookControl().setLookAt(targetPlayer, 30.0F, 30.0F);

            if (jumpscareBuildup >= JUMPSCARE_BUILDUP_TIME) {
                // Execute jumpscare
                this.stalker.jumpscare(targetPlayer);
                this.stop();
            }
        } else if (distanceToPlayer < 25.0) { // 5 blocks
            // Close range - move in for attack when not seen
            if (!playerCanSeeMe) {
                this.stalker.getNavigation().moveTo(targetPlayer, 1.4);

                // Random chance for immediate attack when very close
                if (distanceToPlayer < 4.0 && stalker.getRandom().nextFloat() < 0.1f) {
                    this.stalker.jumpscare(targetPlayer);
                    this.stop();
                }
            } else {
                // Player is looking, freeze or slowly back away
                this.stalker.getNavigation().stop();
                if (stalker.getRandom().nextFloat() < 0.3f) {
                    // Sometimes slowly back away
                    Vec3 retreatDirection = stalker.position().subtract(targetPlayer.position()).normalize();
                    Vec3 retreatPos = stalker.position().add(retreatDirection.scale(3));
                    this.stalker.getNavigation().moveTo(retreatPos.x, retreatPos.y, retreatPos.z, 0.5);
                }
            }
        } else {
            // Medium range - stalk behavior handled by StalkerHideGoal
            isPreparingJumpscare = false;
            jumpscareBuildup = 0;
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