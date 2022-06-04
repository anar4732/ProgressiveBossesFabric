package insane96mcp.progressivebosses.utils;

import insane96mcp.progressivebosses.ProgressiveBosses;
import insane96mcp.progressivebosses.module.Modules;
import me.lortseam.completeconfig.api.ConfigContainer;
import me.lortseam.completeconfig.data.Config;

public class AConfig extends Config implements ConfigContainer {
    private static Modules modules = new Modules();;

    public AConfig() {
        super(ProgressiveBosses.MODID, modules);
    }
    
    public void load2() {
        modules.init();
        super.load();
    }
}