package insane96mcp.progressivebosses.module.dragon;

import insane96mcp.progressivebosses.module.dragon.feature.*;
import insane96mcp.progressivebosses.utils.Label;
import insane96mcp.progressivebosses.utils.LabelConfigGroup;
import me.lortseam.completeconfig.api.ConfigContainer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

@Label(name = "Dragon")
public class DragonModule implements LabelConfigGroup {

	private final ArrayList<ConfigContainer> configs = new ArrayList<>();

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