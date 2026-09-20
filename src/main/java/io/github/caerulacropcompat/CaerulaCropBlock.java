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
import net.minecraft.world.level.pathfinder.PathComputationType;

/**
 * Runtime superclass bridge for the six transformed Caerula Arbor plants.
 * CropBlock methods that would change their original behavior are deliberately
 * neutralized here; growth and placement remain owned by the target classes.
 */
public class CaerulaCropBlock extends CropBlock {
    public CaerulaCropBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected IntegerProperty m_7959_() {
        // Use the property actually registered by the transformed target class.
        return (IntegerProperty) m_49965_().m_61081_("age");
    }

    @Override
    public int m_7419_() {
        return 2;
    }

    @Override
    protected void m_7926_(StateDefinition.Builder<Block, BlockState> builder) {
        // The target class registers its renamed 0..2 property. Adding
        // CropBlock.AGE here would register an incompatible second property.
    }

    @Override
    public void m_213898_(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Route CropBlock's random tick through the target class's growth tick.
        m_213897_(state, level, pos, random);
    }

    @Override
    public boolean m_6724_(BlockState state) {
        return f_60445_;
    }

    @Override
    public boolean m_7898_(BlockState state, LevelReader level, BlockPos pos) {
        return true;
    }

    @Override
    public BlockState m_7417_(BlockState state, net.minecraft.core.Direction direction,
            BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state;
    }

    @Override
    public void m_7892_(BlockState state, Level level, BlockPos pos, Entity entity) {
        // Preserve the original non-trampling behavior instead of CropBlock's
        // ravager collision handling.
    }

    @Override
    protected boolean m_6266_(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public boolean m_7357_(BlockState state, BlockGetter level, BlockPos pos,
            PathComputationType type) {
        if (type == PathComputationType.WATER) {
            return level.m_6425_(pos).m_205070_(FluidTags.f_13131_);
        }
        if (type == PathComputationType.LAND || type == PathComputationType.AIR) {
            return !state.m_60838_(level, pos);
        }
        return false;
    }

    @Override
    protected ItemLike m_6404_() {
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
        return super.m_6404_();
    }
}
