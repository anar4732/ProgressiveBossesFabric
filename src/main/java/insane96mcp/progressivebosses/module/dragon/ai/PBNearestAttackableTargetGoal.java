package insane96mcp.progressivebosses.module.dragon.ai;

import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Difficulty;

public class PBNearestAttackableTargetGoal extends ActiveTargetGoal<PlayerEntity> {
    public PBNearestAttackableTargetGoal(MobEntity mob) {
        super(mob, PlayerEntity.class, 0, false, false, null);
        //allowUnseeable
        this.targetPredicate.ignoreVisibility();
    }

    public boolean canStart() {
        return this.mob.getWorld().getDifficulty() != Difficulty.PEACEFUL && super.canStart();
    }


    protected Box getTargetableArea(double targetDistance) {
        Direction direction = ((ShulkerEntity) this.mob).getAttachedFace();

        if (direction.getAxis() == Direction.Axis.X) {
            return this.mob.getBoundingBox().expand(4.0D, targetDistance, targetDistance);
        } else {
            return direction.getAxis() == Direction.Axis.Z ? this.mob.getBoundingBox().expand(targetDistance, targetDistance, 4.0D) : this.mob.getBoundingBox().expand(targetDistance, 4.0D, targetDistance);
        }
    }
}
