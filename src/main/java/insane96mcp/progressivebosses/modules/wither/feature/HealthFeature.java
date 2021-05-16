package insane96mcp.progressivebosses.modules.wither.feature;

import insane96mcp.insanelib.base.Feature;
import insane96mcp.insanelib.base.Label;
import insane96mcp.insanelib.base.Module;
import insane96mcp.progressivebosses.base.Strings;
import insane96mcp.progressivebosses.setup.Config;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.UUID;

@Label(name = "Health", description = "Bonus Health and Bonus regeneration. The feature even fixes the Wither health bar not updating on spawn.")
public class HealthFeature extends Feature {

	private final ForgeConfigSpec.ConfigValue<Double> bonusPerDifficultyConfig;
	private final ForgeConfigSpec.ConfigValue<Double> maximumBonusRegenConfig;
	private final ForgeConfigSpec.ConfigValue<Double> bonusRegenPerDifficultyConfig;

	public double bonusPerDifficulty = 10d;
	public double maxBonusRegen = 2d;
	public double bonusRegenPerDifficulty = 0.05d;

	private static final UUID WITHER_BONUS_HEALTH = UUID.fromString("fbc1822d-0491-4f6a-9195-c1f2cb783e82");

	public HealthFeature(Module module) {
		super(Config.builder, module);
		Config.builder.comment(this.getDescription()).push(this.getName());
		bonusPerDifficultyConfig = Config.builder
				.comment("Increase Wither's Health by this value per difficulty")
				.defineInRange("Health Bonus per Difficulty", bonusPerDifficulty, 0.0, Double.MAX_VALUE);
		maximumBonusRegenConfig = Config.builder
				.comment("Maximum bonus regeneration per second given by \"Bonus Regeneration per Difficulty\". Set to 0 to disable bonus health regeneration. This doesn't affect the natural regeneration of the Wither (1 Health per Second). It's not recommended to go over 1.0f without mods that adds stronger things to kill the Wither")
				.defineInRange("Maximum Bonus Regeneration", maxBonusRegen, 0.0, Double.MAX_VALUE);
		bonusRegenPerDifficultyConfig = Config.builder
				.comment("How many half hearts will the Wither regen more per difficulty. This doesn't affect the natural regeneration of the Wither (1 Health per Second). (E.g. By default, with 6 Withers spawned, the Wither will heal 1.3 health per second).")
				.defineInRange("Bonus Regeneration per Difficulty", bonusRegenPerDifficulty, 0.0, Double.MAX_VALUE);
		Config.builder.pop();
	}

	@Override
	public void loadConfig() {
		super.loadConfig();
		bonusPerDifficulty = bonusPerDifficultyConfig.get();
		maxBonusRegen = maximumBonusRegenConfig.get();
		bonusRegenPerDifficulty = bonusRegenPerDifficultyConfig.get();
	}

	@SubscribeEvent
	public void onSpawn(EntityJoinWorldEvent event) {
		if (!this.isEnabled())
			return;

		if (this.bonusPerDifficulty == 0d)
			return;

		if (!(event.getEntity() instanceof WitherEntity))
			return;

		WitherEntity wither = (WitherEntity) event.getEntity();

		if (wither.getAttribute(Attributes.MAX_HEALTH).getModifier(WITHER_BONUS_HEALTH) != null)
			return;

		CompoundNBT witherTags = wither.getPersistentData();
		double difficulty = witherTags.getFloat(Strings.Tags.DIFFICULTY);
		ModifiableAttributeInstance health = wither.getAttribute(Attributes.MAX_HEALTH);
		AttributeModifier modifier = new AttributeModifier(WITHER_BONUS_HEALTH, Strings.Tags.WITHER_BONUS_HEALTH, difficulty * this.bonusPerDifficulty, AttributeModifier.Operation.ADDITION);
		health.applyPersistentModifier(modifier);

		boolean hasInvulTicks = wither.getInvulTime() > 0;

		if (hasInvulTicks)
			wither.setHealth(Math.max(1, (float) health.getValue() - 200));
		else
			wither.setHealth((float) health.getValue());
	}

	@SubscribeEvent
	public void onUpdate(LivingEvent.LivingUpdateEvent event) {
		if (!this.isEnabled())
			return;

		if (!(event.getEntity() instanceof WitherEntity))
			return;

		WitherEntity wither = (WitherEntity) event.getEntity();

		fixInvulBossBar(wither);

		if (this.bonusRegenPerDifficulty == 0d || this.maxBonusRegen == 0d)
			return;

		bonusHeal(wither);
	}

	private void bonusHeal(WitherEntity wither) {
		if (wither.getInvulTime() > 0)
			return;

		CompoundNBT tags = wither.getPersistentData();

		float difficulty = tags.getFloat(Strings.Tags.DIFFICULTY);

		if (difficulty <= 0)
			return;

		if (wither.getHealth() <= 0f)
			return;

		float heal = (float) Math.min(difficulty * this.bonusRegenPerDifficulty, this.maxBonusRegen);

		heal /= 20f;

		wither.heal(heal);
	}

	private void fixInvulBossBar(WitherEntity wither) {
		if (wither.getInvulTime() == 0)
			return;

		wither.bossInfo.setPercent(wither.getHealth() / wither.getMaxHealth());
	}
}