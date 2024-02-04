package insane96mcp.progressivebosses.utils;

import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public class MCUtils {
	/**
	 * Returns the current speed of the player compared to his normal speed
	 */
	public static double getMovementSpeedRatio(PlayerEntity player) {
		double baseMS = 0.1d;
		if (player.isSprinting())
			baseMS += 0.03f;
		double playerMS = player.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
		return playerMS / baseMS;
	}

	/**
	 * Different version of ItemStack#addAttributeModifiers that doesn't override
	 * the item's base modifiers
	 */
	public static void addAttributeModifierToItemStack(ItemStack itemStack, EntityAttribute attribute, EntityAttributeModifier modifier, EquipmentSlot modifierSlot) {
		if (itemStack.hasNbt() && !itemStack.getNbt().contains("AttributeModifiers", 9)) {
			for (Map.Entry<EntityAttribute, EntityAttributeModifier> entry : itemStack.getAttributeModifiers(modifierSlot).entries()) {
				itemStack.addAttributeModifier(entry.getKey(), entry.getValue(), modifierSlot);
			}
		}
		itemStack.addAttributeModifier(attribute, modifier, modifierSlot);
	}

	/**
	 * Applies a modifier to the Living Entity. If the attribute is max_health also
	 * sets entity's health to his max health
	 * 
	 * @return true if the modifier was applied
	 */
	public static boolean applyModifier(LivingEntity entity, EntityAttribute attribute, UUID uuid, String name, double amount, EntityAttributeModifier.Operation operation, boolean permanent) {
		EntityAttributeInstance attributeInstance = entity.getAttributeInstance(attribute);
		if (attributeInstance != null) {
			EntityAttributeModifier modifier = new EntityAttributeModifier(uuid, name, amount, operation);
			if (permanent)
				attributeInstance.addPersistentModifier(modifier);
			else
				attributeInstance.addTemporaryModifier(modifier);

			if (attribute == EntityAttributes.GENERIC_MAX_HEALTH)
				entity.setHealth(entity.getMaxHealth());
			return true;
		}
		return false;
	}

	/**
	 * Applies a permanent modifier to the Living Entity. If the attribute is
	 * max_health also sets entity's health to his max health
	 * 
	 * @return true if the modifier was applied
	 */
	public static boolean applyModifier(LivingEntity entity, EntityAttribute attribute, UUID uuid, String name, double amount, EntityAttributeModifier.Operation operation) {
		return applyModifier(entity, attribute, uuid, name, amount, operation, true);
	}

	/**
	 * Sets the value of an attribute
	 * 
	 * @return true if the override was successful
	 */
	public static boolean setAttributeValue(LivingEntity entity, EntityAttribute attribute, double value) {
		EntityAttributeInstance attributeInstance = entity.getAttributeInstance(attribute);
		if (attributeInstance != null) {
			attributeInstance.setBaseValue(value);

			if (attribute == EntityAttributes.GENERIC_MAX_HEALTH)
				entity.setHealth(entity.getMaxHealth());

			return true;
		}
		return false;
	}

	public static boolean hurtIgnoreInvuln(LivingEntity hurtEntity, DamageSource source, float amount) {
		int hurtResistantTime = hurtEntity.timeUntilRegen;
		hurtEntity.timeUntilRegen = 0;
		boolean attacked = hurtEntity.damage(source, amount);
		hurtEntity.timeUntilRegen = hurtResistantTime;
		return attacked;
	}
}
