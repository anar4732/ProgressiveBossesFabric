package insane96mcp.progressivebosses.utils;

import me.lortseam.completeconfig.api.ConfigContainer;
import me.lortseam.completeconfig.api.ConfigGroup;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public interface LabelConfigGroup extends ConfigGroup {
    @Override
    default String getId() {
        return getClass().getAnnotation(Label.class).name();
    }

    @Override
    @Environment(EnvType.CLIENT)
    default String getDescriptionKey() {
        return getClass().getAnnotation(Label.class).description();
    }

    default void addConfigContainer(ConfigContainer config) {
    
    }

    default boolean isEnabled() {
        return true;
    }
}