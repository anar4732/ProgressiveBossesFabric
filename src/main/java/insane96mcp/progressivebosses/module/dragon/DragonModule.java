package insane96mcp.progressivebosses.module.dragon;

import java.util.ArrayList;
import java.util.Collection;

import insane96mcp.progressivebosses.module.dragon.feature.AttackFeature;
import insane96mcp.progressivebosses.module.dragon.feature.CrystalFeature;
import insane96mcp.progressivebosses.module.dragon.feature.DifficultyFeature;
import insane96mcp.progressivebosses.module.dragon.feature.HealthFeature;
import insane96mcp.progressivebosses.module.dragon.feature.LarvaFeature;
import insane96mcp.progressivebosses.module.dragon.feature.MinionFeature;
import insane96mcp.progressivebosses.module.dragon.feature.ResistancesFeature;
import insane96mcp.progressivebosses.module.dragon.feature.RewardFeature;
import insane96mcp.progressivebosses.utils.Label;
import insane96mcp.progressivebosses.utils.LabelConfigGroup;
import me.lortseam.completeconfig.api.ConfigContainer;
import org.jetbrains.annotations.Nullable;

@Label(name = "Dragon")
public class DragonModule implements LabelConfigGroup {

	private ArrayList<ConfigContainer> configs = new ArrayList<>();

	public DifficultyFeature difficulty;
	public HealthFeature health;
	public AttackFeature attack;
	public RewardFeature reward;
	public MinionFeature minion;
	public LarvaFeature larva;
	public ResistancesFeature resistances;
	public CrystalFeature crystal;

	@Override
	public void addConfigContainer(ConfigContainer config) {
		this.configs.add(config);
	}

	@Override
    public @Nullable Collection<ConfigContainer> getTransitives() {
		return this.configs;
    }

	public DragonModule() {
		difficulty = new DifficultyFeature(this);
		health = new HealthFeature(this);
		attack = new AttackFeature(this);
		reward = new RewardFeature(this);
		minion = new MinionFeature(this);
		larva = new LarvaFeature(this);
		resistances = new ResistancesFeature(this);
		crystal = new CrystalFeature(this);
	}
}