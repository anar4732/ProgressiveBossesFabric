package insane96mcp.progressivebosses.utils;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public class DummyEvent {
    private final Level world;
    private final Entity entity;

    public DummyEvent(Level world, Entity entity) {
        this.world = world;
        this.entity = entity;
    }

    public Level getWorld() {
        return world;
    }

    public Entity getEntity() {
        return entity;
    }
}