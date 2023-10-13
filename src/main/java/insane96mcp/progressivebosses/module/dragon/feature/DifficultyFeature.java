package insane96mcp.progressivebosses.module.dragon.feature;

import com.google.common.util.concurrent.AtomicDouble;
import insane96mcp.progressivebosses.AComponents;
import insane96mcp.progressivebosses.utils.*;
import insane96mcp.progressivebosses.utils.LivingEntityEvents.OnLivingDeathEvent;
import me.lortseam.completeconfig.api.ConfigEntries;
import me.lortseam.completeconfig.api.ConfigEntry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@ConfigEntries(includeAll = true)
@Label(name = "Difficulty Settings", description = "How difficulty is handled for the Dragon.")
public class DifficultyFeature implements LabelConfigGroup {

	@ConfigEntry(nameKey = "Sum Killed Dragons Difficulty", comment = "If false and there's more than 1 player around the Dragon, difficulty will be the average of all the players' difficulty instead of summing them.")
	public boolean sumKilledDragonDifficulty = false;

	@ConfigEntry(nameKey = "Bonus Difficulty per Player", comment = "Percentage bonus difficulty added to the Dragon when more than one player is present. Each player past the first one will add this percentage to the difficulty.")
	@ConfigEntry.BoundedDouble(min = 0d, max = 1d)
	public double bonusDifficultyPerPlayer = 0.25d;
	
	@ConfigEntry(nameKey = "Max Difficulty", comment = "The Maximum difficulty (times killed) reachable by Ender Dragon. By default is set to 24 because it's the last spawning end gate.")
	@ConfigEntry.BoundedInteger(min = 1, max = Integer.MAX_VALUE)
	public int maxDifficulty = 8;

	@ConfigEntry(nameKey = "Starting Difficulty", comment = "How much difficulty will players start with when joining a world? Note that this will apply when the player joins the world if the current player difficulty is below this value.")
	@ConfigEntry.BoundedInteger(min = 0, max = Integer.MAX_VALUE)
	public int startingDifficulty = 0;

	@ConfigEntry(nameKey = "Show First Killed Dragon Message", comment = "Set to false to disable the first Dragon killed message.")
	public boolean showFirstKilledDragonMessage = true;

	public DifficultyFeature(LabelConfigGroup config) {
		config.addConfigContainer(this);
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> this.onSpawn(new DummyEvent(world, entity)));
		LivingEntityEvents.DEATH.register((event) -> this.onDeath(event));
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> this.setPlayerData(new DummyEvent(world, entity)));
	}

	public void onSpawn(DummyEvent event) {
		if (event.getWorld().isClientSide)
			return;

		// if (!event.getWorld().getRegistryKey().getValue().equals(DimensionType.THE_END_REGISTRY_KEY.getValue()))
		// 	return;

		if (!(event.getEntity() instanceof EnderDragon dragon))
			return;

		if (dragon.getDragonFight() == null)
			return;

		CompoundTag dragonTags = ((IEntityExtraData) dragon).getPersistentData();
		if (dragonTags.contains(Strings.Tags.DIFFICULTY))
			return;

		int radius = 256;
		BlockPos pos1 = new BlockPos(-radius, -radius, -radius);
		BlockPos pos2 = new BlockPos(radius, radius, radius);
		AABB bb = new AABB(pos1, pos2);

		List<ServerPlayer> players = event.getWorld().getEntitiesOfClass(ServerPlayer.class, bb, (entity) -> true);

		if (players.size() == 0)
			return;

		AtomicInteger playersFirstDragon = new AtomicInteger(0);
		final AtomicDouble dragonDifficulty = new AtomicDouble(0d);

		for (ServerPlayer player : players) {
			AComponents.DF.maybeGet(player).ifPresent(difficulty -> {
				dragonDifficulty.addAndGet(difficulty.getKilledDragons());
				if (difficulty.getFirstDragon() == (byte) 1) {
					playersFirstDragon.incrementAndGet();
					difficulty.setFirstDragon((byte) 2);
				}
			});
		}

		dragonTags.putInt(Strings.Tags.EGGS_TO_DROP, playersFirstDragon.get());

		if (!this.sumKilledDragonDifficulty)
			dragonDifficulty.set(dragonDifficulty.get() / players.size());

		if (players.size() > 1)
			dragonDifficulty.set(dragonDifficulty.get() * (1d + ((players.size() - 1) * this.bonusDifficultyPerPlayer)));

		dragonTags.putFloat(Strings.Tags.DIFFICULTY, (float) dragonDifficulty.get());
	}

	//Increase Player Difficulty
	public void onDeath(OnLivingDeathEvent event) {
		if (event.getEntity().level.isClientSide)
			return;

		if (!(event.getEntity() instanceof EnderDragon dragon))
			return;

		int radius = 256;
		BlockPos pos1 = new BlockPos(-radius, -radius, -radius);
		BlockPos pos2 = new BlockPos(radius, radius, radius);
		AABB bb = new AABB(pos1, pos2);

		List<ServerPlayer> players = dragon.level.getEntitiesOfClass(ServerPlayer.class, bb);
		//If no players are found in the "Spawn Radius Player Check", try to get the nearest player
		if (players.isEmpty()) {
			ServerPlayer nearestPlayer = (ServerPlayer) dragon.level.getNearestPlayer(dragon.getX(), dragon.getY(), dragon.getZ(), Double.MAX_VALUE, true);
			players.add(nearestPlayer);
		}

		for (ServerPlayer player : players) {
			AComponents.DF.maybeGet(player).ifPresent(difficulty -> {
				if (difficulty.getKilledDragons() <= this.startingDifficulty && this.showFirstKilledDragonMessage)
					player.displayClientMessage(Component.translatable(Strings.Translatable.FIRST_DRAGON_KILL), true);
				if (difficulty.getKilledDragons() < this.maxDifficulty)
					difficulty.addKilledDragons(1);
			});
		}
	}

	public void setPlayerData(DummyEvent event) {
		if (event.getWorld().isClientSide)
			return;

		if (!(event.getEntity() instanceof ServerPlayer player))
			return;

		AComponents.DF.maybeGet(player).ifPresent(difficulty -> {
			if (difficulty.getKilledDragons() < this.startingDifficulty) {
				difficulty.setKilledDragons(this.startingDifficulty);
				LogHelper.info("[Progressive Bosses] %s killed dragons counter was below the set 'Starting Difficulty', Has been increased to match 'Starting Difficulty'", player.getName().getString());
			}
			if (difficulty.getKilledDragons() > this.maxDifficulty) {
				difficulty.setKilledDragons(this.maxDifficulty);
				LogHelper.info("[Progressive Bosses] %s killed dragons counter was above the 'Max Difficulty', Has been decreased to match 'Max Difficulty'", player.getName().getString());
			}

			if (difficulty.getFirstDragon() == 0) {
				difficulty.setFirstDragon((byte) 1);
				LogHelper.info("[Progressive Bosses] %s first spawned. Set First Dragon to 1", player.getName().getString());
			}
		});
	}
}