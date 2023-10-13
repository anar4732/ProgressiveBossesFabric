package insane96mcp.progressivebosses.module.wither.feature;

import com.google.common.util.concurrent.AtomicDouble;
import dev.onyxstudios.cca.internal.entity.CardinalEntityInternals;
import insane96mcp.progressivebosses.AComponents;
import insane96mcp.progressivebosses.utils.*;
import me.lortseam.completeconfig.api.ConfigEntries;
import me.lortseam.completeconfig.api.ConfigEntry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.phys.AABB;
import java.util.Arrays;
import java.util.List;

@ConfigEntries(includeAll = true)
@Label(name = "Difficulty Settings", description = "How difficulty is handled for the Wither.")
public class DifficultyFeature implements LabelConfigGroup {

	@ConfigEntries.Exclude
	private static final List<String> defaultEntityBlacklist = Arrays.asList("botania:pink_wither");

	@ConfigEntry(
		nameKey = "Spawn Radius Player Check",
		comment = "How much blocks from wither will be scanned for players to check for difficult")
	@ConfigEntry.BoundedInteger(min = 16, max = Integer.MAX_VALUE)
	public int spawnRadiusPlayerCheck = 128;

	@ConfigEntry(
		nameKey = "Sum Spawned Wither Difficulty",
		comment = "If false and there's more than 1 player around the Wither, difficulty will be the average of all the players' difficulty instead of summing them.")
	public boolean sumSpawnedWitherDifficulty = false;

	@ConfigEntry(
		nameKey = "Bonus Difficulty per Player",
		comment = "Percentage bonus difficulty added to the Wither when more than one player is present. Each player past the first one will add this percentage to the difficulty.")
	@ConfigEntry.BoundedDouble(min = 0d, max = 24d)
	public double bonusDifficultyPerPlayer = 0.25d;

	@ConfigEntry(
		nameKey = "Max Difficulty",
		comment = "The Maximum difficulty (times spawned) reachable by Wither.")
	@ConfigEntry.BoundedInteger(min = 1, max = Integer.MAX_VALUE)
	public int maxDifficulty = 8;

	@ConfigEntry(
		nameKey = "Starting Difficulty",
		comment = "How much difficulty will players start with when joining a world? Note that this will apply when the first Wither is spawned so if the player has already spawned one this will not apply.")
	@ConfigEntry.BoundedInteger(min = 0, max = Integer.MAX_VALUE)
	public int startingDifficulty = 0;

	@ConfigEntry(
		nameKey = "Show First Summoned Wither Message",
		comment = "Set to false to disable the first Wither summoned message.")
	public boolean showFirstSummonedWitherMessage = true;

	@ConfigEntry(
		nameKey = "Entity Blacklist",
		comment = "Entities that extend the vanilla Wither but shouldn't be taken into account by the mod (e.g. Botania's Pink Wither).")
	public List<String> entityBlacklist = defaultEntityBlacklist;

	public DifficultyFeature(LabelConfigGroup config) {
		config.addConfigContainer(this);
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> this.onSpawn(new DummyEvent(world, entity)));
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> this.setPlayerData(new DummyEvent(world, entity)));
	}

	public void onSpawn(DummyEvent event) {
		if (event.getWorld().isClientSide)
			return;

		if (!(event.getEntity() instanceof WitherBoss wither))
			return;

		if (this.entityBlacklist.contains(BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()).toString()))
			return;

		CompoundTag witherTags = ((IEntityExtraData) wither).getPersistentData();
		if (witherTags.contains(Strings.Tags.DIFFICULTY))
			return;

		BlockPos pos1 = wither.blockPosition().offset(-this.spawnRadiusPlayerCheck, -this.spawnRadiusPlayerCheck, -this.spawnRadiusPlayerCheck);
		BlockPos pos2 = wither.blockPosition().offset(this.spawnRadiusPlayerCheck, this.spawnRadiusPlayerCheck, this.spawnRadiusPlayerCheck);
		AABB bb = new AABB(pos1, pos2);

		List<ServerPlayer> players = event.getWorld().getEntitiesOfClass(ServerPlayer.class, bb, player -> player.isAlive());
		if (players.size() == 0)
			return;

		final AtomicDouble witherDifficulty = new AtomicDouble(0d);

		for (ServerPlayer player : players) {
			AComponents.DF.maybeGet(player).ifPresent(difficulty -> {
				witherDifficulty.addAndGet(difficulty.getSpawnedWithers());
				if (difficulty.getSpawnedWithers() >= this.maxDifficulty)
					return;
				if (difficulty.getSpawnedWithers() <= this.startingDifficulty && this.showFirstSummonedWitherMessage)
					player.displayClientMessage(MutableComponent.create(new TranslatableContents(Strings.Translatable.FIRST_WITHER_SUMMON)), false);
				difficulty.addSpawnedWithers(1);
				System.out.println("[Progressive Bosses] Player " + player.getName().getString() + " spawned a Wither. Difficulty: " + difficulty.getSpawnedWithers());
			});
		}

		if (!this.sumSpawnedWitherDifficulty)
			witherDifficulty.set(witherDifficulty.get() / players.size());

		if (players.size() > 1)
			witherDifficulty.set(witherDifficulty.get() * (1d + ((players.size() - 1) * this.bonusDifficultyPerPlayer)));

		witherTags.putFloat(Strings.Tags.DIFFICULTY, (float) witherDifficulty.get());
	}

	public void setPlayerData(DummyEvent event) {
		if (event.getWorld().isClientSide)
			return;

		if (!(event.getEntity() instanceof ServerPlayer))
			return;

		ServerPlayer player = (ServerPlayer) event.getEntity();

		if (!AComponents.DF.maybeGet(player).isPresent()) {
			CardinalEntityInternals.createEntityComponentContainer(player);
		}

		AComponents.DF.maybeGet(player).ifPresent(difficulty -> {
			if (difficulty.getSpawnedWithers() < this.startingDifficulty) {
				difficulty.setSpawnedWithers(this.startingDifficulty);
				LogHelper.info("[Progressive Bosses] %s spawned withers counter was below the set 'Starting Difficulty', Has been increased to match 'Starting Difficulty'", player.getName().getString());
			}
			System.out.println("[Progressive Bosses] Player " + player.getName().getString() + " spawned a Wither. Difficulty: " + difficulty.getSpawnedWithers());
		});
	}
}