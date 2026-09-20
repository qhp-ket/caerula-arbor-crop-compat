package io.github.caerulacropcompat.mixin;

import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = {
        "net.mcreator.caerulaarbor.block.PlantedViviparousLilyBlock",
        "net.mcreator.caerulaarbor.block.NetherseaPotatoPlantBlock",
        "net.mcreator.caerulaarbor.block.NetherseaWheatBlock"
}, remap = false)
abstract class CropAgePropertyMixin {
    @Redirect(
            method = "<clinit>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/properties/IntegerProperty;m_61631_(Ljava/lang/String;II)Lnet/minecraft/world/level/block/state/properties/IntegerProperty;"
            ),
            remap = false
    )
    private static IntegerProperty caerulaCropCompat$useAgeProperty(String ignoredName, int min, int max) {
        return IntegerProperty.m_61631_("age", min, max);
    }
}
