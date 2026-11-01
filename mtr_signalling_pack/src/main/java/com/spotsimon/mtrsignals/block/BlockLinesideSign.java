package com.spotsimon.mtrsignals.block;

import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/**
 * A thin lineside sign on a post. The sign faces the player who places it; the picture is on the
 * model's north side and the model is rotated by the blockstate.
 */
public class BlockLinesideSign extends Block {

	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

	// Boxes in the north-facing orientation: {x1, y1, z1, x2, y2, z2} in 1/16 block units
	private static final double[][] BOXES = {
			{7, 0, 7, 9, 10, 9},   // post
			{2, 4, 6.5, 14, 16, 7.5}   // plate
	};
	private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

	static {
		SHAPES.put(Direction.NORTH, shape(0));
		SHAPES.put(Direction.EAST, shape(1));
		SHAPES.put(Direction.SOUTH, shape(2));
		SHAPES.put(Direction.WEST, shape(3));
	}

	public BlockLinesideSign(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	/**
	 * @param quarterTurns clockwise 90-degree turns seen from above (north = 0, east = 1, ...)
	 */
	private static VoxelShape shape(int quarterTurns) {
		VoxelShape shape = Shapes.empty();
		for (final double[] box : BOXES) {
			double x1 = box[0];
			double z1 = box[2];
			double x2 = box[3];
			double z2 = box[5];
			for (int i = 0; i < quarterTurns; i++) {
				final double newX1 = 16 - z2;
				final double newX2 = 16 - z1;
				final double newZ1 = x1;
				final double newZ2 = x2;
				x1 = newX1;
				x2 = newX2;
				z1 = newZ1;
				z2 = newZ2;
			}
			shape = Shapes.or(shape, Block.box(x1, box[1], z1, x2, box[4], z2));
		}
		return shape;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(FACING));
	}
}
