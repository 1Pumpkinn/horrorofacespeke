package net.tyrone.horrorofacespeke.entity.custom;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class StalkerEntity extends Monster {
    public StalkerEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.ATTACK_DAMAGE, 4.0);
    }

    public void jumpscare(Player player) {
        if (player != null && !player.level().isClientSide) {
            player.playSound(SoundEvents.WARDEN_ROAR, 1.5f, 1f);
            player.hurt(DamageSource.mobAttack(this), 4.0f);
            // Add more effects here if desired
        }
    }

    public void randomJumpscareChance(Player player, RandomSource random) {
        if (random.nextFloat() < 0.005f) { // 0.5% chance per tick
            jumpscare(player);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide) {
            Player closest = this.level().getNearestPlayer(this, 10);
            if (closest != null) {
                this.randomJumpscareChance(closest, this.level().getRandom());
            }
        }
    }


    @Override
    public MobType getMobType() {
        return MobType.UNDEAD;
    }
}
