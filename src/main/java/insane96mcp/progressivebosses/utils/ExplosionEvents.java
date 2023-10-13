package insane96mcp.progressivebosses.utils;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

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
        private final Explosion explosion;
        private boolean cancelled = false;

        public OnExplosionEvent(Explosion explosion) {
            this.explosion = explosion;
        }

        public Explosion getExplosion() {
            return this.explosion;
        }

        public Level getWorld() {
            return this.explosion.level;
        }

        public boolean isCancelled() {
            return this.cancelled;
        }

        public void setCancelled() {
            this.cancelled = true;
        }

        public Vec3 getPosition() {
            return new Vec3(this.explosion.x, this.explosion.y, this.explosion.z);
        }
    }
}