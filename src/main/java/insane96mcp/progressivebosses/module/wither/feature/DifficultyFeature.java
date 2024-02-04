package insane96mcp.progressivebosses.module.wither.feature;

import com.google.common.util.concurrent.AtomicDouble;
import dev.onyxstudios.cca.internal.entity.CardinalEntityInternals;
import insane96mcp.progressivebosses.AComponents;
import insane96mcp.progressivebosses.utils.*;
import me.lortseam.completeconfig.api.ConfigEntries;
import me.lortseam.completeconfig.api.ConfigEntry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.Arrays;
import java.util.List;

import static insane96mcp.progressivebosses.utils.Strings.Translatable.FIRST_WITHER_SUMMON;

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
		if (event.getWorld().isClient)
			return;

		if (!(event.getEntity() instanceof WitherEntity wither))
			return;

		if (this.entityBlacklist.contains(Registries.ENTITY_TYPE.getId(event.getEntity().getType()).toString()))
			return;

		NbtCompound witherTags = ((IEntityExtraData) wither).getPersistentData();
		if (witherTags.contains(Strings.Tags.DIFFICULTY))
			return;

		BlockPos pos1 = wither.getBlockPos().add(-this.spawnRadiusPlayerCheck, -this.spawnRadiusPlayerCheck, -this.spawnRadiusPlayerCheck);
		BlockPos pos2 = wither.getBlockPos().add(this.spawnRadiusPlayerCheck, this.spawnRadiusPlayerCheck, this.spawnRadiusPlayerCheck);
		Box bb = new Box(pos1, pos2);

		List<ServerPlayerEntity> players = event.getWorld().getEntitiesByClass(ServerPlayerEntity.class, bb, player -> player.isAlive());
		if (players.size() == 0)
			return;

		final AtomicDouble witherDifficulty = new AtomicDouble(0d);

		for (ServerPlayerEntity player : players) {
			AComponents.DF.maybeGet(player).ifPresent(difficulty -> {
				witherDifficulty.addAndGet(difficulty.getSpawnedWithers());
				if (difficulty.getSpawnedWithers() >= this.maxDifficulty)
					return;
				if (difficulty.getSpawnedWithers() <= this.startingDifficulty && this.showFirstSummonedWitherMessage)
					player.sendMessage(MutableText.of(new TranslatableTextContent(Strings.Translatable.FIRST_WITHER_SUMMON, "translate error at FIRST_WITHER_SUMMON", new Object[]{})), false);
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
		if (event.getWorld().isClient)
			return;

		if (!(event.getEntity() instanceof ServerPlayerEntity))
			return;

		ServerPlayerEntity player = (ServerPlayerEntity) event.getEntity();

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