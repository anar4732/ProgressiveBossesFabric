package insane96mcp.progressivebosses.utils;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.player.PlayerEntity;

public class PlayerEntityEvents {
    public static Event<OnPlayerTick> TICK = EventFactory.createArrayBacked(OnPlayerTick.class, (listeners) -> (player) -> {
        for (OnPlayerTick listener : listeners) {
            listener.interact(player);
        }
    });

    @FunctionalInterface
    public interface OnPlayerTick {
        void interact(PlayerEntity player);
    }
}