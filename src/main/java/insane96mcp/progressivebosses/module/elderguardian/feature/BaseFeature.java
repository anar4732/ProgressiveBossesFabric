package insane96mcp.progressivebosses.module.elderguardian.feature;

import insane96mcp.progressivebosses.utils.*;
import insane96mcp.progressivebosses.utils.ExplosionEvents.OnExplosionEvent;
import insane96mcp.progressivebosses.utils.LivingEntityEvents.OnLivingDeathEvent;
import me.lortseam.completeconfig.api.ConfigEntries;
import me.lortseam.completeconfig.api.ConfigEntry;
import net.minecraft.entity.mob.ElderGuardianEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;

@ConfigEntries(includeAll = true)
@Label(name = "Base", description = "Base feature for the Elder Guardian harder fights. Disabling this feature will disable the added sound when an Elder Guardian is killed.")
public class BaseFeature implements LabelConfigGroup {

	@ConfigEntry(nameKey = "Adventure mode", comment = "If true, the player will not be able to break blocks when an Elder Guardian is nearby.")
	public boolean adventure = true;
	
	public BaseFeature(LabelConfigGroup parent) {
		parent.addConfigContainer(this);
		PlayerEntityEvents.TICK.register((player) -> this.onPlayerTick(player));
		LivingEntityEvents.DEATH.register((event) -> this.onPlayerDeath(event));
		ExplosionEvents.EXPLODE.register((event) -> this.onExplosionDetonate(event));
		LivingEntityEvents.DEATH.register((event) -> this.onElderGuardianDeath(event));
	}

	public void onPlayerTick(PlayerEntity player) {
		if (player.getWorld().isClient)
			return;

		if (!this.adventure)
			return;

		if (player.age % 20 != 0)
			return;

		if (!player.isAlive())
			return;

		ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
		ServerWorld world = (ServerWorld) serverPlayer.getWorld();

		NbtCompound nbt = ((IEntityExtraData) serverPlayer).getPersistentData();
		boolean previouslyNearElderGuardian = nbt.getBoolean(Strings.Tags.PREVIOUSLY_NEAR_ELDER_GUARDIAN);
		boolean adventureMessage = nbt.getBoolean(Strings.Tags.ADVENTURE_MESSAGE);

		boolean nearElderGuardian = !world.getEntitiesByClass(ElderGuardianEntity.class, serverPlayer.getBoundingBox().expand(32d), entity -> true).isEmpty();
		nbt.putBoolean(Strings.Tags.PREVIOUSLY_NEAR_ELDER_GUARDIAN, nearElderGuardian);

		if (serverPlayer.interactionManager.getGameMode() == GameMode.SURVIVAL && nearElderGuardian) {
			serverPlayer.interactionManager.changeGameMode(GameMode.ADVENTURE);
			serverPlayer.networkHandler.sendPacket(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.GAME_MODE_CHANGED, (float)GameMode.ADVENTURE.getId()));
			if (!adventureMessage) {
				serverPlayer.sendMessage(MutableText.of(new TranslatableTextContent(Strings.Translatable.APPROACHING_ELDER_GUARDIAN, "translate error at APPROACHING_ELDER_GUARDIAN", new Object[]{})), false);
				nbt.putBoolean(Strings.Tags.ADVENTURE_MESSAGE, true);
			}
		}
		else if (serverPlayer.interactionManager.getGameMode() == GameMode.ADVENTURE && !nearElderGuardian && previouslyNearElderGuardian) {
			serverPlayer.interactionManager.changeGameMode(GameMode.SURVIVAL);
			serverPlayer.networkHandler.sendPacket(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.GAME_MODE_CHANGED, (float)GameMode.SURVIVAL.getId()));
		}
	}

	public void onPlayerDeath(OnLivingDeathEvent event) {
		if (!this.adventure)
			return;

		if (!(event.getEntity() instanceof ServerPlayerEntity))
			return;

		ServerPlayerEntity serverPlayer = (ServerPlayerEntity) event.getEntity();

		NbtCompound nbt = ((IEntityExtraData) serverPlayer).getPersistentData();
		boolean previouslyNearElderGuardian = nbt.getBoolean(Strings.Tags.PREVIOUSLY_NEAR_ELDER_GUARDIAN);

		if (previouslyNearElderGuardian && serverPlayer.interactionManager.getGameMode() == GameMode.ADVENTURE) {
			serverPlayer.interactionManager.changeGameMode(GameMode.SURVIVAL);
			serverPlayer.networkHandler.sendPacket(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.GAME_MODE_CHANGED, (float)GameMode.SURVIVAL.getId()));
		}
	}

	public void onExplosionDetonate(OnExplosionEvent event) {
		if (!this.adventure)
			return;

		if (event.getExplosion().getCausingEntity() == null)
			return;

		if (event.getExplosion().destructionType == Explosion.DestructionType.KEEP)
			return;

		boolean nearElderGuardian = !event.getWorld().getEntitiesByClass(ElderGuardianEntity.class, event.getExplosion().getCausingEntity().getBoundingBox().expand(32d), e -> true).isEmpty();
		if (nearElderGuardian) {
			event.setCancelled();
			event.getWorld().createExplosion(event.getExplosion().getCausingEntity(), event.getPosition().x, event.getPosition().y, event.getPosition().z, event.getExplosion().power, event.getExplosion().createFire, World.ExplosionSourceType.MOB);
		}
	}

	public void onElderGuardianDeath(OnLivingDeathEvent event) {
		if (!(event.getEntity() instanceof ElderGuardianEntity))
			return;

		ElderGuardianEntity elderGuardian = (ElderGuardianEntity) event.getEntity();

		int elderGuardiansNearby = elderGuardian.getWorld().getOtherEntities(elderGuardian, elderGuardian.getBoundingBox().expand(48d), entity -> entity instanceof ElderGuardianEntity).size();
		if (elderGuardiansNearby == 0)
			return;

		elderGuardian.playSound(SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, 2f, 0.5f);
	}

	public static int getDeadElderGuardians(ElderGuardianEntity elderGuardian) {
		int elderGuardiansNearby = elderGuardian.getWorld().getOtherEntities(elderGuardian, elderGuardian.getBoundingBox().expand(48d), entity -> entity instanceof ElderGuardianEntity).size();
		return 2 - elderGuardiansNearby;
	}
}