package io.github.caerulacropcompat;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(CaerulaCropCompat.MOD_ID)
public final class CaerulaCropCompat {
    public static final String MOD_ID = "caerula_crop_compat";

    public CaerulaCropCompat() {
        CropConfig.load();
        FMLJavaModLoadingContext.get().getModEventBus().addListener(CropConfig::addDataPack);
    }

    /**
     * Optional HWE bridge. Null means that HWE should keep its original lookup.
     */
    public static net.minecraft.world.level.block.state.properties.IntegerProperty
            getConfiguredAgeProperty(net.minecraft.world.level.block.state.BlockState state) {
        return CropConfig.agePropertyFor(state);
    }
}
