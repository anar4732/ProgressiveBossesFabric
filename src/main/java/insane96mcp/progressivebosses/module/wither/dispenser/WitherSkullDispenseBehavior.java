package insane96mcp.progressivebosses.module.wither.dispenser;

import insane96mcp.progressivebosses.module.wither.feature.MiscFeature;
import net.minecraft.block.Blocks;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.SkullBlock;
import net.minecraft.block.WitherSkullBlock;
import net.minecraft.block.dispenser.FallibleItemDispenserBehavior;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class WitherSkullDispenseBehavior extends FallibleItemDispenserBehavior {

	public ItemStack dispenseSilently(BlockPointer source, ItemStack stack) {
		World world = source.getWorld();
		Direction direction = source.getBlockState().get(DispenserBlock.FACING);
		BlockPos blockpos = source.getPos().offset(direction);
		if (world.isAir(blockpos) && WitherSkullBlock.canDispense(world, blockpos, stack) && MiscFeature.canPlaceSkull(world, blockpos)) {
			world.setBlockState(blockpos, Blocks.WITHER_SKELETON_SKULL.getDefaultState().with(SkullBlock.ROTATION, direction.getAxis() == Direction.Axis.Y ? 0 : direction.getOpposite().getHorizontal() * 4), 3);
			BlockEntity tileentity = world.getBlockEntity(blockpos);
			if (tileentity instanceof SkullBlockEntity) {
				WitherSkullBlock.onPlaced(world, blockpos, (SkullBlockEntity)tileentity);
			}

			stack.decrement(1);
			this.setSuccess(true);
		} else {
			this.setSuccess(ArmorItem.dispenseArmor(source, stack));
		}

		return stack;
	}
}
