package insane96mcp.progressivebosses.utils;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;

public interface LivingEntityEvents {
    Event<OnLivingTick> TICK = EventFactory.createArrayBacked(OnLivingTick.class, (listeners) -> (entity) -> {
        for (OnLivingTick listener : listeners) {
            listener.interact(entity);
        }
    });

    @FunctionalInterface
    public interface OnLivingTick {
        void interact(LivingEntity entity);
    }

    Event<OnLivingHurt> HURT = EventFactory.createArrayBacked(OnLivingHurt.class, (listeners) -> (event) -> {
        for (OnLivingHurt listener : listeners) {
            listener.interact(event);
        }
    });

    @FunctionalInterface
    public interface OnLivingHurt {
        void interact(OnLivingHurtEvent event);
    }

    public static class OnLivingHurtEvent {
        public final DamageSource source;
        public float amount;
        private LivingEntity entity;

        public OnLivingHurtEvent(LivingEntity entity, DamageSource source, float amount) {
            this.source = source;
            this.amount = amount;
            this.entity = entity;
        }

        public float getAmount() {
            return amount;
        }

        public DamageSource getSource() {
            return source;
        }

        public void setAmount(float amount) {
            this.amount = amount;
        }

        public Entity getEntity() {
            return entity;
        }
    }

    Event<OnLivingDeath> DEATH = EventFactory.createArrayBacked(OnLivingDeath.class, (listeners) -> (event) -> {
        for (OnLivingDeath listener : listeners) {
            listener.interact(event);
        }
    });

    @FunctionalInterface
    public interface OnLivingDeath {
        void interact(OnLivingDeathEvent event);
    }

    public static class OnLivingDeathEvent {
        public final LivingEntity entity;
        public final DamageSource source;

        public OnLivingDeathEvent(LivingEntity entity, DamageSource source) {
            this.entity = entity;
            this.source = source;
        }

        public LivingEntity getEntity() {
            return entity;
        }

        public DamageSource getSource() {
            return source;
        }
    }
}