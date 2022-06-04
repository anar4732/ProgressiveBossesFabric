package insane96mcp.progressivebosses;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import insane96mcp.progressivebosses.module.dragon.entity.AreaEffectCloud3DEntity;
import insane96mcp.progressivebosses.module.dragon.entity.Larva;
import insane96mcp.progressivebosses.module.dragon.phase.CrystalRespawnPhase;
import insane96mcp.progressivebosses.module.wither.entity.WitherMinion;
import insane96mcp.progressivebosses.utils.AConfig;
import insane96mcp.progressivebosses.utils.Strings;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.registry.Registry;

public class ProgressiveBosses implements ModInitializer {
	public static final String MODID = "progressivebosses";
	public static final String RESOURCE_PREFIX = MODID + ":";
	public static final Logger LOGGER = LogManager.getLogger();

	public static final AConfig CONFIG = new AConfig();

	public static final EntityType<WitherMinion> WITHER_MINION = Registry.register(Registry.ENTITY_TYPE, MODID + ":" + "wither_minion", FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, WitherMinion::new)
			.dimensions(new EntityDimensions(0.55f, 1.5f, true))
			.fireImmune()
			.trackRangeChunks(8)
			.build());

	public static final EntityType<Larva> LARVA = Registry.register(Registry.ENTITY_TYPE, MODID + ":" + "larva", FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, Larva::new)
			.dimensions(new EntityDimensions(0.6f, 0.45f, true))
			.trackRangeChunks(8)
			.build());

	public static final EntityType<AreaEffectCloud3DEntity> AREA_EFFECT_CLOUD_3D = Registry.register(Registry.ENTITY_TYPE, MODID + ":" + "area_effect_cloud_3d",
			FabricEntityTypeBuilder.<AreaEffectCloud3DEntity>create(SpawnGroup.MISC, AreaEffectCloud3DEntity::new)
			.fireImmune()
			.dimensions(new EntityDimensions(6f, 0.5f, true))
			.trackRangeChunks(10)
			.trackedUpdateRate(Integer.MAX_VALUE)
			.build());

	public static final Item NETHER_STAR_SHARD = Registry.register(Registry.ITEM, MODID + ":" + Strings.Items.NETHER_STAR_SHARD, new Item(new Item.Settings().group(ItemGroup.MATERIALS)));
	public static final Item ELDER_GUARDIAN_SPIKE = Registry.register(Registry.ITEM, MODID + ":" + Strings.Items.ELDER_GUARDIAN_SPIKE, new Item(new Item.Settings().group(ItemGroup.MATERIALS)));		

	@Override
	public void onInitialize() {
		CrystalRespawnPhase.init();
		CONFIG.load2();

		FabricDefaultAttributeRegistry.register(LARVA, Larva.prepareAttributes());
		FabricDefaultAttributeRegistry.register(WITHER_MINION, WitherMinion.prepareAttributes());

		ServerPlayerEvents.COPY_FROM.register(OnPlayerClone::copyFromPlayer);
	}

	public class OnPlayerClone {
		public static void copyFromPlayer(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer, boolean alive) {
			AComponents.DF.maybeGet(oldPlayer).ifPresent(oldDifficulty -> AComponents.DF.maybeGet(newPlayer).ifPresent(newDifficulty -> {
				newDifficulty.setSpawnedWithers(oldDifficulty.getSpawnedWithers());
				newDifficulty.setKilledDragons(oldDifficulty.getKilledDragons());
				newDifficulty.setFirstDragon(oldDifficulty.getFirstDragon());
			 }));
		}
	}

}