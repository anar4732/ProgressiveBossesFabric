package insane96mcp.progressivebosses.module.dragon.feature;

import java.util.List;

import insane96mcp.progressivebosses.ProgressiveBosses;
import insane96mcp.progressivebosses.module.dragon.entity.Larva;
import insane96mcp.progressivebosses.utils.DummyEvent;
import insane96mcp.progressivebosses.utils.IEntityExtraData;
import insane96mcp.progressivebosses.utils.Label;
import insane96mcp.progressivebosses.utils.LabelConfigGroup;
import insane96mcp.progressivebosses.utils.LivingEntityEvents;
import insane96mcp.progressivebosses.utils.MCUtils;
import insane96mcp.progressivebosses.utils.RandomHelper;
import insane96mcp.progressivebosses.utils.Strings;
import me.lortseam.completeconfig.api.ConfigEntries;
import me.lortseam.completeconfig.api.ConfigEntry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.EndPortalFeature;

@ConfigEntries(includeAll = true)
@Label(name = "Larva", description = "Mini things that are just annoying.")
public class LarvaFeature implements LabelConfigGroup {

	@ConfigEntry(nameKey = "Larva at Difficulty", comment = "At which difficulty the Ender Dragon starts spawning Larvae")
	@ConfigEntry.BoundedInteger(min = 1, max = Integer.MAX_VALUE)
	public int larvaAtDifficulty = 1;

	@ConfigEntry(nameKey = "Bonus Larva Every Difficulty", comment = "As the Wither starts spawning Minions, every how much difficulty the Wither will spawn one more Minion")
	@ConfigEntry.BoundedInteger(min = 1, max = Integer.MAX_VALUE)
	public int bonusLarvaEveryDifficulty = 1;

	@ConfigEntry(nameKey = "Max Larvae Spawned", comment = "Maximum Larva spawned by the Ender Dragon")
	@ConfigEntry.BoundedInteger(min = 1, max = Integer.MAX_VALUE)
	public int maxSpawned = 7;

	@ConfigEntry(nameKey = "Minimum Cooldown", comment = "Minimum ticks (20 ticks = 1 seconds) after Minions can spawn.")
	@ConfigEntry.BoundedInteger(min = 1, max = Integer.MAX_VALUE)
	public int minCooldown = 800;

	@ConfigEntry(nameKey = "Maximum Cooldown", comment = "Maximum ticks (20 ticks = 1 seconds) after Minions can spawn.")
	@ConfigEntry.BoundedInteger(min = 1, max = Integer.MAX_VALUE)
	public int maxCooldown = 1400;

	@ConfigEntry(nameKey = "Reduced Dragon Damage", comment = "If true, Larvae will take only 10% damage from the Ender Dragon.")
	public boolean reducedDragonDamage = true;

	public LarvaFeature(LabelConfigGroup config) {
		config.addConfigContainer(this);
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> this.onDragonSpawn(new DummyEvent(world, entity)));
		LivingEntityEvents.TICK.register((entity) -> this.update(new DummyEvent(entity.getWorld(), entity)));
	}

	public void onDragonSpawn(DummyEvent event) {
		if (event.getWorld().isClient)
			return;

		if (!(event.getEntity() instanceof EnderDragonEntity dragon))
			return;

		NbtCompound dragonTags = ((IEntityExtraData) dragon).getPersistentData();

		int cooldown = (int) (RandomHelper.getInt(dragon.getRandom(), this.minCooldown, this.maxCooldown) * 0.5d);
		dragonTags.putInt(Strings.Tags.DRAGON_LARVA_COOLDOWN, cooldown);
	}

	public void update(DummyEvent event) {
		if (event.getEntity().getWorld().isClient)
			return;

		if (!(event.getEntity() instanceof EnderDragonEntity dragon))
			return;

		World world = event.getEntity().getWorld();

		NbtCompound dragonTags = ((IEntityExtraData) dragon).getPersistentData();

		float difficulty = dragonTags.getFloat(Strings.Tags.DIFFICULTY);
		if (difficulty < this.larvaAtDifficulty)
			return;

		if (dragon.getHealth() <= 0)
			return;

		int cooldown = dragonTags.getInt(Strings.Tags.DRAGON_LARVA_COOLDOWN);
		if (cooldown > 0) {
			dragonTags.putInt(Strings.Tags.DRAGON_LARVA_COOLDOWN, cooldown - 1);
			return;
		}

		//If there is no player in the main island don't spawn larvae
		BlockPos centerPodium = dragon.getWorld().getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EndPortalFeature.offsetOrigin(BlockPos.ORIGIN));
		Box bb = new Box(centerPodium).expand(64d);
		List<ServerPlayerEntity> players = world.getNonSpectatingEntities(ServerPlayerEntity.class, bb);

		if (players.isEmpty())
			return;

		int minCooldown = this.minCooldown;
		int maxCooldown = this.maxCooldown;

		cooldown = RandomHelper.getInt(world.random, minCooldown, maxCooldown);
		dragonTags.putInt(Strings.Tags.DRAGON_LARVA_COOLDOWN, cooldown - 1);

		int larvaSpawnedCount = 0;
		for (int i = this.larvaAtDifficulty; i <= difficulty; i += this.bonusLarvaEveryDifficulty) {
			float angle = world.random.nextFloat() * (float) Math.PI * 2f;
			float x = (float) Math.floor(Math.cos(angle) * 3.33f);
			float z = (float) Math.floor(Math.sin(angle) * 3.33f);
			int y = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, new BlockPos((int) x, 255, (int) z)).getY();
			summonLarva(world, new Vec3d(x + 0.5, y, z + 0.5), difficulty);
			larvaSpawnedCount++;
			if (larvaSpawnedCount >= this.maxSpawned)
				break;
		}
	}

	public Larva summonLarva(World world, Vec3d pos, float difficulty) {
		Larva larva = new Larva(ProgressiveBosses.LARVA, world);
		NbtCompound minionTags = ((IEntityExtraData) larva).getPersistentData();

		minionTags.putBoolean("mobspropertiesrandomness:processed", true);
		//TODO Scaling health

		larva.setPosition(pos.x, pos.y, pos.z);
		larva.setPersistent();

		MCUtils.applyModifier(larva, EntityAttributes.GENERIC_ATTACK_DAMAGE, Strings.AttributeModifiers.ATTACK_DAMAGE_BONUS_UUID, Strings.AttributeModifiers.ATTACK_DAMAGE_BONUS, 0.35 * difficulty, EntityAttributeModifier.Operation.ADDITION);
		// MCUtils.applyModifier(larva, ForgeMod.SWIM_SPEED.get(), Strings.AttributeModifiers.SWIM_SPEED_BONUS_UUID, Strings.AttributeModifiers.SWIM_SPEED_BONUS, 2.5d, EntityAttributeModifier.Operation.MULTIPLY_BASE);

		world.spawnEntity(larva);
		return larva;
	}
}
