package insane96mcp.progressivebosses.module.wither;

import java.util.ArrayList;
import java.util.Collection;

import insane96mcp.progressivebosses.module.wither.feature.AttackFeature;
import insane96mcp.progressivebosses.module.wither.feature.DifficultyFeature;
import insane96mcp.progressivebosses.module.wither.feature.HealthFeature;
import insane96mcp.progressivebosses.module.wither.feature.MinionFeature;
import insane96mcp.progressivebosses.module.wither.feature.MiscFeature;
import insane96mcp.progressivebosses.module.wither.feature.ResistancesFeature;
import insane96mcp.progressivebosses.module.wither.feature.RewardFeature;
import insane96mcp.progressivebosses.utils.Label;
import insane96mcp.progressivebosses.utils.LabelConfigGroup;
import me.lortseam.completeconfig.api.ConfigContainer;
import org.jetbrains.annotations.Nullable;

@Label(name = "Wither")
public class WitherModule implements LabelConfigGroup {

	private ArrayList<ConfigContainer> configs = new ArrayList<>();

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