package net.tyrone.horrorofacespeke.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.tyrone.horrorofacespeke.entity.custom.StalkerEntity;

import java.util.EnumSet;

public class StalkerHideGoal extends Goal {
    private final StalkerEntity stalker;
    private Player targetPlayer;
    private BlockPos hidePosition;
    private int hideTicks;
    private int peekTicks;
    private int observedTicks;
    private boolean isPeeking;
    private boolean isHiding;
    private static final int HIDE_DURATION = 60; // 3 seconds
    private static final int PEEK_DURATION = 40; // 2 seconds
    private static final int MAX_OBSERVED_HIDING = 30; // 1.5 seconds before fleeing

    public StalkerHideGoal(StalkerEntity stalker) {
        this.stalker = stalker;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        this.targetPlayer = this.stalker.level.getNearestPlayer(this.stalker, 20.0);
        return this.targetPlayer != null && !this.targetPlayer.isCreative() &&
                !this.targetPlayer.isSpectator() && !this.stalker.isPanicking();
    }

    @Override
    public boolean canContinueToUse() {
        return this.targetPlayer != null && this.targetPlayer.isAlive() &&
                this.stalker.distanceToSqr(this.targetPlayer) < 400.0 && // 20 blocks
                !this.stalker.isPanicking();
    }

    @Override
    public void start() {
        this.hideTicks = 0;
        this.peekTicks = 0;
        this.observedTicks = 0;
        this.isPeeking = false;
        this.isHiding = false;
    }

    @Override
    public void stop() {
        this.hidePosition = null;
        this.isPeeking = false;
        this.isHiding = false;
        this.observedTicks = 0;
    }

    @Override
    public void tick() {
        if (this.targetPlayer == null) return;

        double distanceToPlayer = this.stalker.distanceToSqr(this.targetPlayer);
        boolean playerCanSeeMe = canPlayerSeeEntity();

        // Handle being observed while hiding
        if (playerCanSeeMe && (isHiding || isPeeking)) {
            observedTicks++;

            if (observedTicks > MAX_OBSERVED_HIDING) {
                // Player has been staring too long, flee!
                fleeFromPlayer();
                return;
            }
        } else {
            observedTicks = Math.max(0, observedTicks - 1);
        }

        if (distanceToPlayer > 25.0 && distanceToPlayer < 256.0) { // 5-16 blocks
            // Medium range - stalk behavior
            if (!isPeeking && !isHiding && (hidePosition == null || !isGoodHidingSpot(hidePosition))) {
                findHidingSpot();
            }

            if (hidePosition != null) {
                if (!isPeeking && !isHiding) {
                    // Move to hiding spot
                    this.stalker.getNavigation().moveTo(hidePosition.getX(), hidePosition.getY(), hidePosition.getZ(), 1.2);

                    if (this.stalker.blockPosition().distSqr(hidePosition) < 4.0) {
                        isHiding = true;
                        hideTicks = 0;
                        // Stop moving when hiding
                        this.stalker.getNavigation().stop();
                    }
                } else if (isHiding && !isPeeking) {
                    // Hiding phase
                    hideTicks++;
                    if (hideTicks > HIDE_DURATION) {
                        isPeeking = true;
                        isHiding = false;
                        peekTicks = 0;
                    }
                } else if (isPeeking) {
                    // Peeking behavior
                    this.stalker.getLookControl().setLookAt(targetPlayer, 30.0F, 30.0F);
                    peekTicks++;

                    if (peekTicks > PEEK_DURATION || playerCanSeeMe) {
                        isPeeking = false;
                        isHiding = false;
                        hideTicks = 0;
                        hidePosition = null; // Find new hiding spot
                    }
                }
            }
        } else if (distanceToPlayer > 256.0) {
            // Far range - move closer stealthily
            moveTowardsPlayerStealth();
        } else if (distanceToPlayer < 25.0) {
            // Close range - let other goals handle this
            this.stop();
        }
    }

    private void fleeFromPlayer() {
        // Apply minor blindness effect
        if (stalker.getRandom().nextFloat() < 0.4f) {
            targetPlayer.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.BLINDNESS, 40, 0)); // 2 seconds
        }

        // Play spooky sound
        targetPlayer.playSound(net.minecraft.sounds.SoundEvents.AMBIENT_CAVE, 1.0f, 0.8f);

        // High chance to teleport away
        if (stalker.getRandom().nextFloat() < 0.7f) {
            stalker.teleportRandomly();
        } else {
            // Fast retreat
            Vec3 playerPos = targetPlayer.position();
            Vec3 retreatDirection = stalker.position().subtract(playerPos).normalize();
            Vec3 retreatPos = stalker.position().add(retreatDirection.scale(25));
            this.stalker.getNavigation().moveTo(retreatPos.x, retreatPos.y, retreatPos.z, 1.8);
        }

        this.stop();
    }

    private void findHidingSpot() {
        Vec3 playerPos = targetPlayer.position();
        Vec3 stalkerPos = stalker.position();

        // Look for hiding spots in a circle around the player
        for (int attempts = 0; attempts < 25; attempts++) {
            double angle = stalker.getRandom().nextDouble() * Math.PI * 2;
            double distance = 8 + stalker.getRandom().nextDouble() * 10; // 8-18 blocks from player

            double x = playerPos.x + Math.cos(angle) * distance;
            double z = playerPos.z + Math.sin(angle) * distance;

            BlockPos testPos = new BlockPos(x, playerPos.y, z);

            if (isGoodHidingSpot(testPos)) {
                hidePosition = testPos;
                break;
            }
        }
    }

    private boolean isGoodHidingSpot(BlockPos pos) {
        if (pos == null) return false;

        // Check if the position is safe and has cover
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx == 0 && dz == 0) continue;

                BlockPos checkPos = pos.offset(dx, 0, dz);
                BlockState state = stalker.level.getBlockState(checkPos);
                if (state.isSolidRender(stalker.level, checkPos)) {
                    // Check if this block provides cover from player
                    Vec3 playerPos = targetPlayer.position();
                    Vec3 blockPos = Vec3.atCenterOf(checkPos);
                    Vec3 stalkerToPlayer = playerPos.subtract(Vec3.atCenterOf(pos)).normalize();
                    Vec3 stalkerToBlock = blockPos.subtract(Vec3.atCenterOf(pos)).normalize();

                    double dot = stalkerToPlayer.dot(stalkerToBlock);
                    if (dot > 0.5) { // Block provides some cover
                        return stalker.level.getBlockState(pos).isAir() &&
                                stalker.level.getBlockState(pos.above()).isAir() &&
                                stalker.level.getBlockState(pos.below()).isSolidRender(stalker.level, pos.below());
                    }
                }
            }
        }
        return false;
    }

    private void moveTowardsPlayerStealth() {
        Vec3 playerPos = targetPlayer.position();
        Vec3 direction = playerPos.subtract(stalker.position()).normalize();

        // Move at a more random angle to avoid detection
        double angle = stalker.getRandom().nextDouble() * Math.PI / 2 - Math.PI / 4; // ±45 degrees
        double newX = direction.x * Math.cos(angle) - direction.z * Math.sin(angle);
        double newZ = direction.x * Math.sin(angle) + direction.z * Math.cos(angle);

        Vec3 movePos = stalker.position().add(newX * 12, 0, newZ * 12);
        this.stalker.getNavigation().moveTo(movePos.x, movePos.y, movePos.z, 0.9);
    }

    private boolean canPlayerSeeEntity() {
        return targetPlayer.hasLineOfSight(stalker) &&
                isPlayerLookingAt(targetPlayer, stalker, 0.75);
    }

    private boolean isPlayerLookingAt(Player player, StalkerEntity entity, double precision) {
        Vec3 playerLook = player.getViewVector(1.0F).normalize();
        Vec3 playerToEntity = entity.position().subtract(player.position()).normalize();
        return playerLook.dot(playerToEntity) > precision;
    }
}