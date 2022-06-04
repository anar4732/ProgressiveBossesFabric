package insane96mcp.progressivebosses.module;

import insane96mcp.progressivebosses.module.dragon.DragonModule;
import insane96mcp.progressivebosses.module.elderguardian.ElderGuardianModule;
import insane96mcp.progressivebosses.module.wither.WitherModule;
import insane96mcp.progressivebosses.utils.AEC3DFeature;
import insane96mcp.progressivebosses.utils.Label;
import insane96mcp.progressivebosses.utils.LabelConfigGroup;
import me.lortseam.completeconfig.api.ConfigContainer;

@Label(name = "Modules")
public class Modules implements LabelConfigGroup {
	public static WitherModule wither;
	public static DragonModule dragon;
	public static ElderGuardianModule elderGuardian;

	public static AEC3DFeature aec3d;

	public void init() {
		wither = new WitherModule();
		dragon = new DragonModule();
		elderGuardian = new ElderGuardianModule();
		aec3d = new AEC3DFeature(this);
	}

	@Override
    public ConfigContainer[] getTransitives() {
        return new ConfigContainer[]{wither, dragon, elderGuardian, aec3d};
    }
}