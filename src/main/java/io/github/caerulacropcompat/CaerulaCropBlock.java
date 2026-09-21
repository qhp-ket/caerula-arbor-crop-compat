package io.github.caerulacropcompat;

import net.mcreator.caerulaarbor.block.NetherseaPotatoPlantBlock;
import net.mcreator.caerulaarbor.block.NetherseaWheatBlock;
import net.mcreator.caerulaarbor.block.PlantedCellBlock;
import net.mcreator.caerulaarbor.block.PlantedFakeEggBlock;
import net.mcreator.caerulaarbor.block.PlantedViviparousLilyBlock;
import net.mcreator.caerulaarbor.block.TentaclePlantBlock;
import net.mcreator.caerulaarbor.init.CaerulaArborModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.pathfinder.PathComputationType;

/**
 * Runtime superclass bridge for the six transformed Caerula Arbor plants.
 * CropBlock methods that would change their original behavior are deliberately
 * neutralized here; growth and placement remain owned by the target classes.
 */
public class CaerulaCropBlock extends CropBlock {
    public CaerulaCropBlock(BlockBehaviour.Properties properties) {
        super(properties);
        // Block constructs its StateDefinition before this superclass
        // constructor returns, so invalid future schemas fail during registry
        // initialization instead of during a later harvest attempt.
        getAgeProperty();
    }

    @Override
    protected IntegerProperty getAgeProperty() {
        // Use the property actually registered by the transformed target class.
        Property<?> property = getStateDefinition().getProperty("blockstate");
        if (!(property instanceof IntegerProperty ageProperty)) {
            throw new IllegalStateException("Expected integer blockstate age property on " + this);
        }
        if (!ageProperty.getPossibleValues().contains(0)) {
            throw new IllegalStateException("blockstate age property must contain 0 on " + this);
        }
        return ageProperty;
    }

    @Override
    public int getMaxAge() {
        return getAgeProperty().getPossibleValues().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElseThrow(() -> new IllegalStateException("blockstate age property has no values"));
    }

    /**
     * Public, allocation-free access for the optional Harvest With Ease bridge.
     * Normal callers should use the standard CropBlock API.
     */
    public final IntegerProperty getCompatibilityAgeProperty() {
        return getAgeProperty();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        // The target class registers its native blockstate property. Adding
        // CropBlock.AGE here would register an incompatible second property.
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Route CropBlock's random tick through the target class's growth tick.
        tick(state, level, pos, random);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        // The original blocks are random-ticking throughout their lifecycle;
        // their own tick method decides whether growth may proceed.
        return true;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return true;
    }

    @Override
    public BlockState updateShape(BlockState state, net.minecraft.core.Direction direction,
            BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        // Preserve the original non-trampling behavior instead of CropBlock's
        // ravager collision handling.
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos,
            PathComputationType type) {
        if (type == PathComputationType.WATER) {
            return level.getFluidState(pos).is(FluidTags.WATER);
        }
        if (type == PathComputationType.LAND || type == PathComputationType.AIR) {
            return !state.isCollisionShapeFullBlock(level, pos);
        }
        return false;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        if (PlantedViviparousLilyBlock.class.isInstance(this)) {
            return CaerulaArborModItems.PLANTED_VIVIPAROUS_LILY.get();
        }
        if (NetherseaPotatoPlantBlock.class.isInstance(this)) {
            return CaerulaArborModItems.NETHERSEA_POTATO.get();
        }
        if (NetherseaWheatBlock.class.isInstance(this)) {
            return CaerulaArborModItems.NETHERSEA_WHEAT.get();
        }
        if (TentaclePlantBlock.class.isInstance(this)) {
            return CaerulaArborModItems.OCEAN_PEDUNCLE.get();
        }
        if (PlantedCellBlock.class.isInstance(this)) {
            return CaerulaArborModItems.OCEAN_CELL.get();
        }
        if (PlantedFakeEggBlock.class.isInstance(this)) {
            return CaerulaArborModItems.FAKE_EGG.get();
        }
        return super.getBaseSeedId();
    }
}
