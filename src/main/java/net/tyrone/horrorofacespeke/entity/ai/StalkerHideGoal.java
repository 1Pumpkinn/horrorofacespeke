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
    private boolean isPeeking;
    private static final int HIDE_DURATION = 60; // 3 seconds
    private static final int PEEK_DURATION = 40; // 2 seconds

    public StalkerHideGoal(StalkerEntity stalker) {
        this.stalker = stalker;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        this.targetPlayer = this.stalker.level.getNearestPlayer(this.stalker, 15.0);
        return this.targetPlayer != null && !this.targetPlayer.isCreative() && !this.targetPlayer.isSpectator();
    }

    @Override
    public boolean canContinueToUse() {
        return this.targetPlayer != null && this.targetPlayer.isAlive() &&
                this.stalker.distanceToSqr(this.targetPlayer) < 225.0; // 15 blocks
    }

    @Override
    public void start() {
        this.hideTicks = 0;
        this.peekTicks = 0;
        this.isPeeking = false;
    }

    @Override
    public void tick() {
        if (this.targetPlayer == null) return;

        double distanceToPlayer = this.stalker.distanceToSqr(this.targetPlayer);
        boolean playerCanSeeMe = canPlayerSeeEntity();

        if (distanceToPlayer > 16.0 && distanceToPlayer < 64.0) {
            // Medium range - stalk behavior
            if (!isPeeking && (hidePosition == null || !isGoodHidingSpot(hidePosition))) {
                findHidingSpot();
            }

            if (hidePosition != null) {
                if (!isPeeking) {
                    // Move to hiding spot
                    this.stalker.getNavigation().moveTo(hidePosition.getX(), hidePosition.getY(), hidePosition.getZ(), 1.0);

                    if (this.stalker.blockPosition().distSqr(hidePosition) < 4.0) {
                        hideTicks++;
                        if (hideTicks > HIDE_DURATION) {
                            isPeeking = true;
                            peekTicks = 0;
                        }
                    }
                } else {
                    // Peeking behavior
                    this.stalker.getLookControl().setLookAt(targetPlayer, 30.0F, 30.0F);
                    peekTicks++;

                    if (peekTicks > PEEK_DURATION || playerCanSeeMe) {
                        isPeeking = false;
                        hideTicks = 0;
                        hidePosition = null; // Find new hiding spot
                    }
                }
            }
        } else if (distanceToPlayer > 64.0) {
            // Far range - move closer stealthily
            moveTowardsPlayerStealth();
        } else if (distanceToPlayer < 16.0) {
            // Close range - retreat if seen, attack if not
            if (playerCanSeeMe) {
                retreatFromPlayer();
            } else {
                // Close enough for attack
                this.stalker.getNavigation().moveTo(targetPlayer, 1.2);
            }
        }
    }

    private void findHidingSpot() {
        Vec3 playerPos = targetPlayer.position();
        Vec3 stalkerPos = stalker.position();

        // Look for hiding spots in a circle around the player
        for (int attempts = 0; attempts < 20; attempts++) {
            double angle = stalker.getRandom().nextDouble() * Math.PI * 2;
            double distance = 8 + stalker.getRandom().nextDouble() * 8; // 8-16 blocks from player

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

        // Check if there's a solid block to hide behind
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos checkPos = pos.offset(dx, 0, dz);
                BlockState state = stalker.level.getBlockState(checkPos);
                if (state.isSolidRender(stalker.level, checkPos)) {
                    // Check if this block is between stalker and player
                    Vec3 playerPos = targetPlayer.position();
                    Vec3 blockPos = Vec3.atCenterOf(checkPos);
                    Vec3 stalkerToPlayer = playerPos.subtract(stalker.position()).normalize();
                    Vec3 stalkerToBlock = blockPos.subtract(stalker.position()).normalize();

                    double dot = stalkerToPlayer.dot(stalkerToBlock);
                    if (dot > 0.7) { // Block is roughly between stalker and player
                        return stalker.level.getBlockState(pos).isAir() &&
                                stalker.level.getBlockState(pos.above()).isAir();
                    }
                }
            }
        }
        return false;
    }

    private void moveTowardsPlayerStealth() {
        Vec3 playerPos = targetPlayer.position();
        Vec3 direction = playerPos.subtract(stalker.position()).normalize();

        // Move at an angle to avoid direct line of sight
        double angle = stalker.getRandom().nextDouble() * Math.PI / 3 - Math.PI / 6; // ±30 degrees
        double newX = direction.x * Math.cos(angle) - direction.z * Math.sin(angle);
        double newZ = direction.x * Math.sin(angle) + direction.z * Math.cos(angle);

        Vec3 movePos = stalker.position().add(newX * 10, 0, newZ * 10);
        this.stalker.getNavigation().moveTo(movePos.x, movePos.y, movePos.z, 0.8);
    }

    private void retreatFromPlayer() {
        Vec3 playerPos = targetPlayer.position();
        Vec3 retreatDirection = stalker.position().subtract(playerPos).normalize();
        Vec3 retreatPos = stalker.position().add(retreatDirection.scale(15));

        this.stalker.getNavigation().moveTo(retreatPos.x, retreatPos.y, retreatPos.z, 1.3);
    }

    private boolean canPlayerSeeEntity() {
        return targetPlayer.hasLineOfSight(stalker) &&
                isPlayerLookingAt(targetPlayer, stalker, 0.8);
    }

    private boolean isPlayerLookingAt(Player player, StalkerEntity entity, double precision) {
        Vec3 playerLook = player.getViewVector(1.0F).normalize();
        Vec3 playerToEntity = entity.position().subtract(player.position()).normalize();
        return playerLook.dot(playerToEntity) > precision;
    }
}