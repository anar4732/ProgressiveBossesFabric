package insane96mcp.progressivebosses;

import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistryV3;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentInitializer;
import dev.onyxstudios.cca.api.v3.entity.RespawnCopyStrategy;
import insane96mcp.progressivebosses.capability.Difficulty;

public class AComponents implements EntityComponentInitializer {
    public static final ComponentKey<Difficulty> DF = ComponentRegistryV3.INSTANCE.getOrCreate(Difficulty.IDENTIFIER, Difficulty.class);

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        registry.registerForPlayers(DF, player -> {
            return new Difficulty();
        }, RespawnCopyStrategy.ALWAYS_COPY);
    }
}