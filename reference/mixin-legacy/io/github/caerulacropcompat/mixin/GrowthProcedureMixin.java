package io.github.caerulacropcompat.mixin;

import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = {
        "net.mcreator.caerulaarbor.procedures.PlantGrowUpProcedure",
        "net.mcreator.caerulaarbor.procedures.CanContinueToGrowProcedure",
        "net.mcreator.caerulaarbor.procedures.BoneBoostPlantProcedure"
}, remap = false)
abstract class GrowthProcedureMixin {
    @Redirect(
            method = "*",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/StateDefinition;m_61081_(Ljava/lang/String;)Lnet/minecraft/world/level/block/state/properties/Property;"
            ),
            remap = false
    )
    private static Property<?> caerulaCropCompat$resolveRenamedAge(
            StateDefinition<?, ?> definition,
            String name
    ) {
        Property<?> property = definition.m_61081_(name);
        if (property == null && "blockstate".equals(name)) {
            return definition.m_61081_("age");
        }
        return property;
    }
}
