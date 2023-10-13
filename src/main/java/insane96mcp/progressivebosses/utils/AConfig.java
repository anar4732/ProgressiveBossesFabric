package insane96mcp.progressivebosses.utils;

import insane96mcp.progressivebosses.ProgressiveBosses;
import insane96mcp.progressivebosses.module.Modules;
import me.lortseam.completeconfig.api.ConfigContainer;
import me.lortseam.completeconfig.data.Config;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;

public class AConfig extends Config implements ConfigContainer {
    private static final Modules modules = new Modules();

    public AConfig() {
        super(ProgressiveBosses.MODID, modules);
    }
    
    public void load2() {
        modules.init();
        super.load();
    }
	
	@Override
	@Environment(EnvType.CLIENT)
	public Component getName() {
		return Component.empty();
	}
}