package insane96mcp.progressivebosses.module.elderguardian;

import java.util.ArrayList;
import java.util.Collection;

import insane96mcp.progressivebosses.module.elderguardian.feature.AttackFeature;
import insane96mcp.progressivebosses.module.elderguardian.feature.BaseFeature;
import insane96mcp.progressivebosses.module.elderguardian.feature.HealthFeature;
import insane96mcp.progressivebosses.module.elderguardian.feature.MinionFeature;
import insane96mcp.progressivebosses.module.elderguardian.feature.ResistancesFeature;
import insane96mcp.progressivebosses.module.elderguardian.feature.RewardFeature;
import insane96mcp.progressivebosses.utils.Label;
import insane96mcp.progressivebosses.utils.LabelConfigGroup;
import me.lortseam.completeconfig.api.ConfigContainer;
import org.jetbrains.annotations.Nullable;

@Label(name = "Elder Guardian")
public class ElderGuardianModule implements LabelConfigGroup {

	private ArrayList<ConfigContainer> configs = new ArrayList<>();

	public HealthFeature health;
	public BaseFeature base;
	public ResistancesFeature resistances;
	public AttackFeature attack;
	public MinionFeature minion;
	public RewardFeature reward;

	@Override
	public void addConfigContainer(ConfigContainer config) {
		this.configs.add(config);
	}

	@Override
	public @Nullable Collection<ConfigContainer> getTransitives() {
		return this.configs;
    }

	public ElderGuardianModule() {
		health = new HealthFeature(this);
		base = new BaseFeature(this);
		resistances = new ResistancesFeature(this);
		attack = new AttackFeature(this);
		minion = new MinionFeature(this);
		reward = new RewardFeature(this);
	}

}