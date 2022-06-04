package insane96mcp.progressivebosses.utils;

import me.lortseam.completeconfig.api.ConfigContainer;
import me.lortseam.completeconfig.api.ConfigGroup;

public interface LabelConfigGroup extends ConfigGroup {
    @Override
    default String getId() {
        return ((Label)getClass().<Label>getAnnotation(Label.class)).name();
    }

    @Override
    default String[] getTooltipTranslationKeys() {
        return new String[]{
            ((Label)getClass().<Label>getAnnotation(Label.class)).description()
        };
    }

    default void addConfigContainer(ConfigContainer config) {
        
    }

    default boolean isEnabled() {
        return true;
    }
}