package insane96mcp.progressivebosses.module.wither.feature;

import insane96mcp.progressivebosses.module.wither.ai.WitherChargeAttackGoal;
import insane96mcp.progressivebosses.module.wither.ai.WitherRangedAttackGoal;
import insane96mcp.progressivebosses.network.PacketManagerServer;
import insane96mcp.progressivebosses.utils.*;
import insane96mcp.progressivebosses.utils.LivingEntityEvents.OnLivingHurtEvent;
import me.lortseam.completeconfig.api.ConfigEntries;
import me.lortseam.completeconfig.api.ConfigEntry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.WitherSkull;
import java.util.ArrayList;
import java.util.UUID;

@ConfigEntries(includeAll = true)
@Label(name = "Attack", description = "Makes the Wither smarter (will no longer try to stand on the player's head ...), attack faster and hit harder")
public class AttackFeature implements LabelConfigGroup {

	@ConfigEntry(nameKey = "Wither Attack", comment = "Makes the Wither smarter (will no longer try to stand on the player's head ...), attack faster and hit harder")
	public boolean applyToVanillaWither = true;

	@ConfigEntry(nameKey = "Max Charge Attack Chance", comment = "Chance every time the Wither takes damage to start a charge attack. Less health = higher chance and more damage taken = more chance. This value is the chance at 0% health and when taking 10 damage.")
	@ConfigEntry.BoundedDouble(min = 0d, max = 1d)
	public double maxChargeAttackChance = 0.06d;

	@ConfigEntry(nameKey = "Increased Damage", comment = "Percentage bonus damage dealt by the Wither per difficulty.")
	@ConfigEntry.BoundedDouble(min = 0d, max = Double.MAX_VALUE)
	public double increasedDamage = 0.12d;

	@ConfigEntry(nameKey = "Barrage Attack", comment = "Chance (per difficulty) every time the Wither takes damage to start a barrage attack. Less health = higher chance and more damage taken = more chance. This value is the chance at 0% health and when taking 10 damage.")
	@ConfigEntry.BoundedDouble(min = 0d, max = 1d)
	public double maxBarrageChancePerDiff = 0.011d;

	@ConfigEntry(nameKey = "Min Barrage Duration", comment = "Min time (in ticks) for the duration of the barrage attack. Less health = longer barrage.")
	@ConfigEntry.BoundedInteger(min = 0, max = Integer.MAX_VALUE)
	public int minBarrageDuration = 20;

	@ConfigEntry(nameKey = "Max Barrage Duration", comment = "Max time (in ticks) for the duration of the barrage attack. Less health = longer barrage")
	@ConfigEntry.BoundedInteger(min = 0, max = Integer.MAX_VALUE)
	public int maxBarrageDuration = 150;

	@ConfigEntry(nameKey = "Skulls", comment = "Wither Skull Changes")
	@ConfigEntry.BoundedDouble(min = 0d, max = Double.MAX_VALUE)
	public double skullVelocityMultiplier = 2.75d;

	@ConfigEntry(nameKey = "Attack Speed", comment = "Attack Speed Changes")
	@ConfigEntry.BoundedInteger(min = 0, max = Integer.MAX_VALUE)
	public int attackInterval = 40;

	@ConfigEntry(nameKey = "Increase Attack Speed when Near", comment = "The middle head will attack faster (up to 40% of the attack speed) the nearer the target is to the Wither.")
	public boolean increaseAttackSpeedWhenNear = true;

	public AttackFeature(LabelConfigGroup parent) {
		parent.addConfigContainer(this);
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> this.onSpawn(new DummyEvent(world, entity)));
		LivingEntityEvents.TICK.register((entity) -> this.onUpdate(new DummyEvent(entity.level, entity)));
		LivingEntityEvents.HURT.register((event) -> this.onDamaged(event));
		LivingEntityEvents.HURT.register((event) -> this.onDamageDealt(event));
	}

	public void onSpawn(DummyEvent event) {
		witherSkullSpeed(event.getEntity());

		if (event.getWorld().isClientSide)
			return;

		if (!(event.getEntity() instanceof WitherBoss wither))
			return;

		CompoundTag compoundNBT = ((IEntityExtraData) wither).getPersistentData();
		if ((!compoundNBT.contains(Strings.Tags.DIFFICULTY) || compoundNBT.getFloat(Strings.Tags.DIFFICULTY) == 0f) && !this.applyToVanillaWither)
			return;

		setWitherAI(wither);
	}

	private void witherSkullSpeed(Entity entity) {
		if (!(entity instanceof WitherSkull witherSkullEntity))
			return;

		if (this.skullVelocityMultiplier == 0d)
			return;

		if (Math.abs(witherSkullEntity.xPower) > 10 || Math.abs(witherSkullEntity.yPower) > 10 || Math.abs(witherSkullEntity.zPower) > 10) {
			entity.kill();
			return;
		}

		witherSkullEntity.xPower *= this.skullVelocityMultiplier;
		witherSkullEntity.yPower *= this.skullVelocityMultiplier;
		witherSkullEntity.zPower *= this.skullVelocityMultiplier;
	}

	public void onUpdate(DummyEvent event) {
		if (!event.getEntity().isAlive())
			return;

		if (!(event.getEntity() instanceof WitherBoss wither))
			return;

		tickCharge(wither);

		if (event.getEntity().level.isClientSide)
			return;

		chargeUnseen(wither);
	}

	private void tickCharge(WitherBoss wither) {
		if (this.maxChargeAttackChance == 0d)
			return;
		byte chargeTick = ((IEntityExtraData) wither).getPersistentData().getByte(Strings.Tags.CHARGE_ATTACK);
		// System.out.println(chargeTick);
		// ((IEntityExtraData) wither).getPersistentData().putByte(Strings.Tags.CHARGE_ATTACK, (byte) 100);
		// When in charge attack remove the vanilla health regeneration when he's invulnerable and add 1% health regeneration of the missing health per second
		if (chargeTick > 0){
			if (wither.tickCount % 10 == 0) {
				float missingHealth = wither.getMaxHealth() - wither.getHealth();
				wither.setHealth(wither.getHealth() + (missingHealth * 0.005f));
			}
			((IEntityExtraData) wither).getPersistentData().putByte(Strings.Tags.CHARGE_ATTACK, (byte) (chargeTick - 1));
		}
	}

	private void chargeUnseen(WitherBoss wither) {
		CompoundTag witherTags = ((IEntityExtraData) wither).getPersistentData();

		if (witherTags.getByte(Strings.Tags.CHARGE_ATTACK) <= 0 && wither.tickCount % 20 == 0) {
			doCharge(wither, witherTags.getInt(Strings.Tags.UNSEEN_PLAYER_TICKS) / 20f);
		}
	}

	public void onDamageDealt(OnLivingHurtEvent event) {
		if (event.getEntity().level.isClientSide)
			return;

		if (this.increasedDamage == 0d)
			return;
		// System.out.println(event.getAmount());
		WitherBoss wither;
		if (event.getSource().getEntity() instanceof WitherBoss)
			wither = (WitherBoss) event.getSource().getEntity();
		else if (event.getSource().getDirectEntity() instanceof WitherBoss)
			wither = (WitherBoss) event.getSource().getDirectEntity();
		else
			return;

		CompoundTag compoundNBT = ((IEntityExtraData) wither).getPersistentData();
		float difficulty = compoundNBT.getFloat(Strings.Tags.DIFFICULTY);

		event.setAmount(event.getAmount() * (float)(1d + (this.increasedDamage * difficulty)));
	}

	public void onDamaged(OnLivingHurtEvent event) {
		if (event.getEntity().level.isClientSide)
			return;

		if (!event.getEntity().isAlive())
			return;

		if (!(event.getEntity() instanceof WitherBoss wither))
			return;

		doBarrage(wither, event.getAmount());
		doCharge(wither, event.getAmount());
	}

	private void doBarrage(WitherBoss wither, float damageTaken) {
		if (this.maxBarrageChancePerDiff == 0d)
			return;

		CompoundTag witherTags = ((IEntityExtraData) wither).getPersistentData();
		float difficulty = witherTags.getFloat(Strings.Tags.DIFFICULTY);

		double missingHealthPerc = 1d - wither.getHealth() / wither.getMaxHealth();

		double chance = (this.maxBarrageChancePerDiff * difficulty) * missingHealthPerc;
		chance *= (damageTaken / 10f);
		double r = wither.getRandom().nextDouble();
		if (r < chance) {
			int duration = (int) (((this.maxBarrageDuration - this.minBarrageDuration) * missingHealthPerc) + this.minBarrageDuration);
			witherTags.putInt(Strings.Tags.BARRAGE_ATTACK, duration);
		}
	}

	private void doCharge(WitherBoss wither, float damageTaken) {
		if (this.maxChargeAttackChance == 0d)
			return;
		if (((IEntityExtraData) wither).getPersistentData().getByte(Strings.Tags.CHARGE_ATTACK) > 0)
			return;

		double missingHealthPerc = 1d - wither.getHealth() / wither.getMaxHealth();
		double chance = this.maxChargeAttackChance * missingHealthPerc;
		chance *= (damageTaken / 10f);
		double r = wither.getRandom().nextDouble();
		if (r < chance) {
			initCharging(wither);
		}
	}

	public void setWitherAI(WitherBoss wither) {
		ArrayList<Goal> toRemove = new ArrayList<>();
		wither.goalSelector.availableGoals.forEach(goal -> {
			if (goal.getGoal() instanceof RangedAttackGoal)
				toRemove.add(goal.getGoal());
			if (goal.getGoal() instanceof WitherBoss.WitherDoNothingGoal)
				toRemove.add(goal.getGoal());
		});

		toRemove.forEach(wither.goalSelector::removeGoal);

		wither.goalSelector.addGoal(1, new WitherChargeAttackGoal(wither));
		wither.goalSelector.addGoal(2, new WitherRangedAttackGoal(wither,  this.attackInterval, 24.0f, this.increaseAttackSpeedWhenNear));

		MCUtils.applyModifier(wither, Attributes.FOLLOW_RANGE, UUID.randomUUID(), "Wither Glasses", 48d, AttributeModifier.Operation.ADDITION);
	}

	public static class Consts {
		public static final int CHARGE_ATTACK_TICK_START = 90;
		public static final int CHARGE_ATTACK_TICK_CHARGE = 30;
	}

	public static void initCharging(WitherBoss wither) {
		((IEntityExtraData) wither).getPersistentData().putByte(Strings.Tags.CHARGE_ATTACK, (byte) Consts.CHARGE_ATTACK_TICK_START);
		for (Player player : wither.level.players()) {
			PacketManagerServer.MessageWitherSync((ServerPlayer) player, wither, (byte) Consts.CHARGE_ATTACK_TICK_START);
		}
	}

	public static void stopCharging(WitherBoss wither) {
		((IEntityExtraData) wither).getPersistentData().putByte(Strings.Tags.CHARGE_ATTACK, (byte) 0);
		for (Player player : wither.level.players()) {
			PacketManagerServer.MessageWitherSync((ServerPlayer) player, wither, (byte) 0);
		}
	}
}