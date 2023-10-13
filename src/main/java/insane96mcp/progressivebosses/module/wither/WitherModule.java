package insane96mcp.progressivebosses.module.wither;

import insane96mcp.progressivebosses.module.wither.feature.*;
import insane96mcp.progressivebosses.utils.Label;
import insane96mcp.progressivebosses.utils.LabelConfigGroup;
import me.lortseam.completeconfig.api.ConfigContainer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

@Label(name = "Wither")
public class WitherModule implements LabelConfigGroup {

	private final ArrayList<ConfigContainer> configs = new ArrayList<>();

	public DifficultyFeature difficulty;
	public MiscFeature misc;
	public HealthFeature health;
	public ResistancesFeature resistances;
	public RewardFeature reward;
	public MinionFeature minion;
	public AttackFeature attack;

	@Override
	public void addConfigContainer(ConfigContainer config) {
		this.configs.add(config);
	}

	@Override
	public @Nullable Collection<ConfigContainer> getTransitives() {
		return this.configs;
    }

	public WitherModule() {
		//Must be the first one to be initialized, otherwise the other modules will not get the correct difficulty settings
		difficulty = new DifficultyFeature(this);
		misc = new MiscFeature(this);
		health = new HealthFeature(this);
		resistances = new ResistancesFeature(this);
		reward = new RewardFeature(this);
		minion = new MinionFeature(this);
		attack = new AttackFeature(this);
	}

}