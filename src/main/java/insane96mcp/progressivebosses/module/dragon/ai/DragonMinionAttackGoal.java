package insane96mcp.progressivebosses.module.dragon.ai;

import insane96mcp.progressivebosses.utils.DragonMinionHelper;
import insane96mcp.progressivebosses.utils.IEntityExtraData;
import insane96mcp.progressivebosses.utils.Strings;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.entity.projectile.ShulkerBulletEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.Difficulty;
import java.util.EnumSet;

public class DragonMinionAttackGoal extends Goal {

    private int attackTime;
    private final ShulkerEntity shulker;

    private final int baseAttackInterval;

    public DragonMinionAttackGoal(ShulkerEntity shulker, int attackInterval) {
        this.shulker = shulker;
        this.baseAttackInterval = attackInterval / 2;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    public boolean canStart() {
        LivingEntity livingentity = shulker.getTarget();
        if (livingentity != null && livingentity.isAlive()) {
            return shulker.getWorld().getDifficulty() != Difficulty.PEACEFUL;
        } else {
            return false;
        }
    }

    public void start() {
        this.attackTime = this.baseAttackInterval;
        shulker.setPeekAmount(100);
    }

    public void stop() {
        shulker.setPeekAmount(0);
    }

    public void tick() {
        if (shulker.getWorld().getDifficulty() == Difficulty.PEACEFUL)
            return;

        --this.attackTime;
        LivingEntity livingentity = shulker.getTarget();
        if (livingentity == null)
            return;
        shulker.getLookControl().lookAt(livingentity, 180.0F, 180.0F);
        double d0 = shulker.squaredDistanceTo(livingentity.getPos());
        if (d0 < 9216d) { //96 blocks
            if (this.attackTime <= 0) {
                this.attackTime = this.baseAttackInterval + shulker.getRandom().nextInt(10) * this.baseAttackInterval / 2;
                ShulkerBulletEntity bullet = new ShulkerBulletEntity(shulker.getWorld(), shulker, livingentity, shulker.getAttachedFace().getAxis());
                if (DragonMinionHelper.isBlindingMinion(this.shulker)) {
                    NbtCompound nbt = ((IEntityExtraData) bullet).getPersistentData();
                    nbt.putBoolean(Strings.Tags.BLINDNESS_BULLET, true);
                }
                shulker.getWorld().spawnEntity(bullet);
                shulker.playSound(SoundEvents.ENTITY_SHULKER_SHOOT, 2.0F, (shulker.getWorld().random.nextFloat() - shulker.getWorld().random.nextFloat()) * 0.2F + 1.0F);
            }
        } else {
            shulker.setTarget(null);
        }

        super.tick();
    }
}
