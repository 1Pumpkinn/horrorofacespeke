// Create this file: src/main/java/net/tyrone/horrorofacespeke/entity/ai/StalkerTeleportGoal.java

package net.tyrone.horrorofacespeke.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.tyrone.horrorofacespeke.entity.custom.StalkerEntity;

import java.util.EnumSet;

public class StalkerTeleportGoal extends Goal {
    private final StalkerEntity stalker;
    private Player targetPlayer;
    private int teleportCooldown;
    private static final int TELEPORT_COOLDOWN = 200; // 10 seconds
    private static final double TELEPORT_RANGE = 30.0;

    public StalkerTeleportGoal(StalkerEntity stalker) {
        this.stalker = stalker;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        this.targetPlayer = this.stalker.level.getNearestPlayer(this.stalker, TELEPORT_RANGE);
        return this.targetPlayer != null && !this.targetPlayer.isCreative() &&
                !this.targetPlayer.isSpectator() && teleportCooldown <= 0 &&
                this.stalker.distanceToSqr(this.targetPlayer) > 64.0; // Only when far away
    }

    @Override
    public boolean canContinueToUse() {
        return false; // One-time use
    }

    @Override
    public void start() {
        if (attemptTeleport()) {
            teleportCooldown = TELEPORT_COOLDOWN;
        } else {
            teleportCooldown = 40; // Shorter cooldown if teleport failed
        }
    }

    @Override
    public void tick() {
        if (teleportCooldown > 0) {
            teleportCooldown--;
        }
    }

    private boolean attemptTeleport() {
        if (targetPlayer == null) return false;

        // Try to teleport behind the player
        Vec3 playerPos = targetPlayer.position();
        Vec3 playerLook = targetPlayer.getViewVector(1.0F).normalize();

        // Try multiple positions behind the player
        for (int attempts = 0; attempts < 16; attempts++) {
            double distance = 8 + stalker.getRandom().nextDouble() * 8; // 8-16 blocks behind
            double angle = stalker.getRandom().nextDouble() * Math.PI / 2 - Math.PI / 4; // ±45 degrees

            // Calculate position behind player
            Vec3 behindDirection = playerLook.scale(-1).yRot((float) angle);
            Vec3 teleportPos = playerPos.add(behindDirection.scale(distance));

            BlockPos targetPos = new BlockPos(teleportPos.x, teleportPos.y, teleportPos.z);

            if (isValidTeleportLocation(targetPos)) {
                // Play teleport sound at old position
                stalker.level.playSound(null, stalker.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                        stalker.getSoundSource(), 0.5F, 1.0F);

                // Teleport
                stalker.teleportTo(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5);

                // Play teleport sound at new position
                stalker.level.playSound(null, targetPos, SoundEvents.ENDERMAN_TELEPORT,
                        stalker.getSoundSource(), 0.5F, 1.0F);

                return true;
            }
        }

        return false;
    }

    private boolean isValidTeleportLocation(BlockPos pos) {
        // Check if the position is safe for teleporting
        if (pos.getY() < 0 || pos.getY() > 255) return false;

        BlockState groundState = stalker.level.getBlockState(pos.below());
        BlockState feetState = stalker.level.getBlockState(pos);
        BlockState headState = stalker.level.getBlockState(pos.above());

        // Need solid ground and air space for entity
        return groundState.isSolidRender(stalker.level, pos.below()) &&
                !feetState.isSolidRender(stalker.level, pos) &&
                !headState.isSolidRender(stalker.level, pos.above()) &&
                !playerCanSeePosition(pos);
    }

    private boolean playerCanSeePosition(BlockPos pos) {
        if (targetPlayer == null) return false;

        Vec3 playerEye = targetPlayer.getEyePosition(1.0F);
        Vec3 targetPos = Vec3.atCenterOf(pos);

        // Check if player has line of sight to teleport position
        return stalker.level.clip(new net.minecraft.world.level.ClipContext(
                playerEye, targetPos,
                net.minecraft.world.level.ClipContext.Block.VISUAL,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                targetPlayer)).getType() == net.minecraft.world.phys.HitResult.Type.MISS;
    }
}