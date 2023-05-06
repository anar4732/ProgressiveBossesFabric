package insane96mcp.progressivebosses.utils;

import insane96mcp.progressivebosses.ProgressiveBosses;
import insane96mcp.progressivebosses.mixin.TranslationKeyMixin;
import insane96mcp.progressivebosses.module.Modules;
import me.lortseam.completeconfig.api.ConfigContainer;
import me.lortseam.completeconfig.data.Config;
import me.lortseam.completeconfig.text.TranslationBase;
import me.lortseam.completeconfig.text.TranslationKey;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public class AConfig extends Config implements ConfigContainer {
    private static Modules modules = new Modules();;

    public AConfig() {
        super(ProgressiveBosses.MODID, modules);
    }
    
    public void load2() {
        modules.init();
        super.load();
    }
	
	@Override
	@Environment(EnvType.CLIENT)
	public Text getName() {
		return Text.empty();
	}
}