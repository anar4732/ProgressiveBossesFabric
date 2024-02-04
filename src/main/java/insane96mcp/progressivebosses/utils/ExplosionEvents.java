package insane96mcp.progressivebosses.utils;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;

public class ExplosionEvents {
    public static Event<OnExplode> EXPLODE = EventFactory.createArrayBacked(OnExplode.class, (listeners) -> (entity) -> {
        for (OnExplode listener : listeners) {
            listener.interact(entity);
        }
    });

    @FunctionalInterface
    public interface OnExplode {
        void interact(OnExplosionEvent explosion);
    }

    public static class OnExplosionEvent {
        private Explosion explosion;
        private boolean cancelled = false;

        public OnExplosionEvent(Explosion explosion) {
            this.explosion = explosion;
        }

        public Explosion getExplosion() {
            return this.explosion;
        }

        public World getWorld() {
            return this.explosion.world;
        }

        public boolean isCancelled() {
            return this.cancelled;
        }

        public void setCancelled() {
            this.cancelled = true;
        }

        public Vec3d getPosition() {
            return new Vec3d(this.explosion.x, this.explosion.y, this.explosion.z);
        }
    }
}