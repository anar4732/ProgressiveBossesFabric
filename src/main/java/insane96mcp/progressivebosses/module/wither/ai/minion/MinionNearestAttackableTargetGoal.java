package insane96mcp.progressivebosses.module.wither.ai.minion;

import org.jetbrains.annotations.Nullable;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import java.util.function.Predicate;

public class MinionNearestAttackableTargetGoal extends ActiveTargetGoal<PlayerEntity> {
	public MinionNearestAttackableTargetGoal(MobEntity goalOwner, Class<PlayerEntity> targetClass, int targetChance, boolean checkSight, boolean nearbyOnly, @Nullable Predicate<LivingEntity> targetPredicate) {
		super(goalOwner, targetClass, targetChance, checkSight, nearbyOnly, targetPredicate);
		this.targetPredicate = TargetPredicate.DEFAULT.setBaseMaxDistance(this.getFollowRange()).ignoreVisibility().setPredicate(targetPredicate);
	}
}
