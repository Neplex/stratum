package fr.neplex.stratum.mixin;

import fr.neplex.stratum.StratumPositionContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin extends LevelMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "effects", at = @At("HEAD"), cancellable = true)
    private void effects(CallbackInfoReturnable<DimensionSpecialEffects> cir) {
        Player player = this.minecraft.player;
        if (player == null) return;

        StratumPositionContext.push(player.blockPosition());
        Holder<DimensionType> holder = this.stratum$getDimensionType();
        if (holder != null) {
            cir.setReturnValue(DimensionSpecialEffects.forType(holder.value()));
        }
        StratumPositionContext.pop();
    }
}
