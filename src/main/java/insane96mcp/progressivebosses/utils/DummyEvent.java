package insane96mcp.progressivebosses.utils;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;

public class DummyEvent {
    private World world;
    private Entity entity;

    public DummyEvent(World world, Entity entity) {
        this.world = world;
        this.entity = entity;
    }

    public World getWorld() {
        return world;
    }

    public Entity getEntity() {
        return entity;
    }
}