package insane96mcp.progressivebosses.module.wither.feature;

import insane96mcp.progressivebosses.module.wither.dispenser.WitherSkullDispenseBehavior;
import insane96mcp.progressivebosses.utils.*;
import insane96mcp.progressivebosses.utils.ExplosionEvents.OnExplosionEvent;
import insane96mcp.progressivebosses.utils.LivingEntityEvents.OnLivingHurtEvent;
import me.lortseam.completeconfig.api.ConfigEntries;
import me.lortseam.completeconfig.api.ConfigEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;

@ConfigEntries(includeAll = true)
@Label(name = "Misc", description = "Handles various small features, such as the explosion")
public class MiscFeature implements LabelConfigGroup {
	
	@ConfigEntry(nameKey = "Explosion Power Bonus", comment = "How much explosion power (after the invulnerability) will the Wither gain for each difficulty point. Explosion Radius is capped to 13. Base Wither Explosion Power is 7.0. Setting this to 0 will not increase the Wither Explosion Power.")
	@ConfigEntry.BoundedDouble(min = 0, max = 4d)
	public double explosionPowerBonus = 1d;
	
	@ConfigEntry(nameKey = "Explosion Causes Fire at Difficulty", comment = "At this difficulty the Wither Explosion will cause fire. Set to -1 to disable.")
	@ConfigEntry.BoundedInteger(min = -1, max = Integer.MAX_VALUE)
	public int explosionCausesFireAtDifficulty = 5;
	
	@ConfigEntry(nameKey = "Faster Breaking Blocks", comment = "The Wither will no longer wait 1.0 seconds before breaking blocks when he's hit, instead just 0.5s")
	public boolean fasterBlockBreaking = true;
	
	@ConfigEntry(nameKey = "Bigger Breaking Blocks", comment = "The Wither will break even blocks below him when hit.")
	public boolean biggerBlockBreaking = true;
	
	@ConfigEntry(nameKey = "Ignore Witherproof Blocks", comment = "If true the Wither will break even blocks that are witherproof. Unbreakable blocks will still be unbreakable, so it's really useful with other mods as in vanilla Wither Proof Blocks are all the unbreakable blocks.")
	public boolean ignoreWitherProofBlocks = false;
	
	@ConfigEntry(nameKey = "Wither Nether Only", comment = "The wither can only be spawned in the Nether.\nNote that this feature completely disables Wither Skulls from begin placed nearby Soul Sand when not in the Nether or when on the Nether Roof.\nRequires Minecraft restart.")
	public boolean witherNetherOnly = false;
	
	@ConfigEntries.Exclude
	private boolean behaviourRegistered = false;
	
	public MiscFeature(LabelConfigGroup parent) {
		parent.addConfigContainer(this);
		if (this.witherNetherOnly && !behaviourRegistered) {
			DispenserBlock.registerBehavior(Items.WITHER_SKELETON_SKULL, new WitherSkullDispenseBehavior());
			behaviourRegistered = true;
		}
		LivingEntityEvents.TICK.register((entity) -> this.onUpdate(new DummyEvent(entity.level, entity)));
		ExplosionEvents.EXPLODE.register((event) -> this.onExplosion(event));
		LivingEntityEvents.HURT.register((event) -> this.onWitherDamage(event));
		
	}
	
	public void onUpdate(DummyEvent event) {
		if (event.getEntity().level.isClientSide)
			return;
		
		if (!this.biggerBlockBreaking)
			return;
		
		if (!(event.getEntity() instanceof WitherBoss wither))
			return;
		
		if (!wither.isAlive())
			return;
		
		// Overrides the block breaking in wither's updateAI since LivingUpdateEvent is
		// called before
		if (wither.destroyBlocksTick == 1) {
			--wither.destroyBlocksTick;
			// if
			// (net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(wither.world,
			// wither)) {
			int i1 = Mth.floor(wither.getY());
			int l1 = Mth.floor(wither.getX());
			int i2 = Mth.floor(wither.getZ());
			boolean flag = false;
			
			int yOffsetLow = -1;
			if (wither.isPowered())
				yOffsetLow = 0;
			
			for (int k2 = -1; k2 <= 1; ++k2) {
				for (int l2 = -1; l2 <= 1; ++l2) {
					for (int j = yOffsetLow; j <= 4; ++j) {
						int i3 = l1 + k2;
						int k = i1 + j;
						int l = i2 + l2;
						BlockPos blockpos = new BlockPos(i3, k, l);
						BlockState blockstate = wither.level.getBlockState(blockpos);
						if (canWitherDestroy(wither, blockpos, blockstate)) {
							flag = wither.level.destroyBlock(blockpos, true, wither) || flag;
						}
					}
				}
			}
			
			if (flag) {
				wither.level.levelEvent(null, 1022, wither.blockPosition(), 0);
			}
		}
		// }
	}
	
	private boolean canWitherDestroy(WitherBoss wither, BlockPos pos, BlockState state) {
		if (this.ignoreWitherProofBlocks)
			return !state.isAir() && state.getDestroySpeed(wither.level, pos) >= 0f;
		 else
		    return WitherBoss.canDestroy(state);
	}
	
	public void onExplosion(OnExplosionEvent event) {
		if (this.explosionCausesFireAtDifficulty == -1 && this.explosionPowerBonus == 0d)
			return;
		
		if (!(event.getExplosion().getIndirectSourceEntity() instanceof WitherBoss wither))
			return;
		
		// Check if the explosion is the one from the wither
		if (event.getExplosion().radius != 7f)
			return;
		
		CompoundTag tags = ((IEntityExtraData) wither).getPersistentData();
		
		float difficulty = tags.getFloat(Strings.Tags.DIFFICULTY);
		
		if (difficulty <= 0f)
			return;
		
		float explosionPower = (float) (event.getExplosion().radius + (this.explosionPowerBonus * difficulty));
		
		if (explosionPower > 13f)
			explosionPower = 13f;
		
		event.getExplosion().radius = explosionPower;
		
		event.getExplosion().fire = difficulty >= this.explosionCausesFireAtDifficulty;
	}
	
	public void onWitherDamage(OnLivingHurtEvent event) {
		if (event.getEntity().level.isClientSide)
			return;
		
		if (!this.fasterBlockBreaking)
			return;
		
		if (!event.getEntity().isAlive())
			return;
		
		if (!(event.getEntity() instanceof WitherBoss wither))
			return;
		
		wither.destroyBlocksTick = 10;
	}
	
	// @SubscribeEvent
	// public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
	
	// 	if (!this.witherNetherOnly)
	// 		return;
	
	// 	if (event.getItemStack().getItem() == Items.WITHER_SKELETON_SKULL && !canPlaceSkull(event.getWorld(), event.getPos().offset(event.getFace().getNormal()))) {
	// 		event.setCanceled(true);
	// 	}
	// }
	
	/**
	 * Returns true if at the specified position a Wither Skull can be placed
	 */
	public static boolean canPlaceSkull(Level world, BlockPos pos) {
		boolean isNether = world.dimension().location().equals(BuiltinDimensionTypes.NETHER_EFFECTS);
		
		boolean hasSoulSandNearby = false;
		for (Direction dir : Direction.values()) {
			if (world.getBlockState(pos.offset(dir.getNormal())).getBlock().equals(Blocks.SOUL_SAND) || world.getBlockState(pos.offset(dir.getNormal())).getBlock().equals(Blocks.SOUL_SOIL)) {
				hasSoulSandNearby = true;
				break;
			}
		}
		
		// If it's not the nether or if it is but it's on the Nether roof and there's
		// soulsand nearby
		return (isNether && pos.getY() <= 127) || !hasSoulSandNearby;
	}
}