package insane96mcp.progressivebosses.module.elderguardian.feature;

import insane96mcp.progressivebosses.utils.*;
import insane96mcp.progressivebosses.utils.ExplosionEvents.OnExplosionEvent;
import insane96mcp.progressivebosses.utils.LivingEntityEvents.OnLivingDeathEvent;
import me.lortseam.completeconfig.api.ConfigEntries;
import me.lortseam.completeconfig.api.ConfigEntry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

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

	public void onPlayerTick(Player player) {
		if (player.level.isClientSide)
			return;

		if (!this.adventure)
			return;

		if (player.tickCount % 20 != 0)
			return;

		if (!player.isAlive())
			return;

		ServerPlayer serverPlayer = (ServerPlayer) player;
		ServerLevel world = (ServerLevel) serverPlayer.level;

		CompoundTag nbt = ((IEntityExtraData) serverPlayer).getPersistentData();
		boolean previouslyNearElderGuardian = nbt.getBoolean(Strings.Tags.PREVIOUSLY_NEAR_ELDER_GUARDIAN);
		boolean adventureMessage = nbt.getBoolean(Strings.Tags.ADVENTURE_MESSAGE);

		boolean nearElderGuardian = !world.getEntitiesOfClass(ElderGuardian.class, serverPlayer.getBoundingBox().inflate(32d), entity -> true).isEmpty();
		nbt.putBoolean(Strings.Tags.PREVIOUSLY_NEAR_ELDER_GUARDIAN, nearElderGuardian);

		if (serverPlayer.gameMode.getGameModeForPlayer() == GameType.SURVIVAL && nearElderGuardian) {
			serverPlayer.gameMode.changeGameModeForPlayer(GameType.ADVENTURE);
			serverPlayer.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.CHANGE_GAME_MODE, (float)GameType.ADVENTURE.getId()));
			if (!adventureMessage) {
				serverPlayer.displayClientMessage(MutableComponent.create(new TranslatableContents(Strings.Translatable.APPROACHING_ELDER_GUARDIAN)), false);
				nbt.putBoolean(Strings.Tags.ADVENTURE_MESSAGE, true);
			}
		}
		else if (serverPlayer.gameMode.getGameModeForPlayer() == GameType.ADVENTURE && !nearElderGuardian && previouslyNearElderGuardian) {
			serverPlayer.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
			serverPlayer.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.CHANGE_GAME_MODE, (float)GameType.SURVIVAL.getId()));
		}
	}

	public void onPlayerDeath(OnLivingDeathEvent event) {
		if (!this.adventure)
			return;

		if (!(event.getEntity() instanceof ServerPlayer))
			return;

		ServerPlayer serverPlayer = (ServerPlayer) event.getEntity();

		CompoundTag nbt = ((IEntityExtraData) serverPlayer).getPersistentData();
		boolean previouslyNearElderGuardian = nbt.getBoolean(Strings.Tags.PREVIOUSLY_NEAR_ELDER_GUARDIAN);

		if (previouslyNearElderGuardian && serverPlayer.gameMode.getGameModeForPlayer() == GameType.ADVENTURE) {
			serverPlayer.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
			serverPlayer.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.CHANGE_GAME_MODE, (float)GameType.SURVIVAL.getId()));
		}
	}

	public void onExplosionDetonate(OnExplosionEvent event) {
		if (!this.adventure)
			return;

		if (event.getExplosion().getIndirectSourceEntity() == null)
			return;

		if (event.getExplosion().blockInteraction == Explosion.BlockInteraction.KEEP)
			return;

		boolean nearElderGuardian = !event.getWorld().getEntitiesOfClass(ElderGuardian.class, event.getExplosion().getIndirectSourceEntity().getBoundingBox().inflate(32d), e -> true).isEmpty();
		if (nearElderGuardian) {
			event.setCancelled();
			event.getWorld().explode(event.getExplosion().getIndirectSourceEntity(), event.getPosition().x, event.getPosition().y, event.getPosition().z, event.getExplosion().radius, event.getExplosion().fire, Level.ExplosionInteraction.MOB);
		}
	}

	public void onElderGuardianDeath(OnLivingDeathEvent event) {
		if (!(event.getEntity() instanceof ElderGuardian))
			return;

		ElderGuardian elderGuardian = (ElderGuardian) event.getEntity();

		int elderGuardiansNearby = elderGuardian.level.getEntities(elderGuardian, elderGuardian.getBoundingBox().inflate(48d), entity -> entity instanceof ElderGuardian).size();
		if (elderGuardiansNearby == 0)
			return;

		elderGuardian.playSound(SoundEvents.ELDER_GUARDIAN_CURSE, 2f, 0.5f);
	}

	public static int getDeadElderGuardians(ElderGuardian elderGuardian) {
		int elderGuardiansNearby = elderGuardian.level.getEntities(elderGuardian, elderGuardian.getBoundingBox().inflate(48d), entity -> entity instanceof ElderGuardian).size();
		return 2 - elderGuardiansNearby;
	}
}