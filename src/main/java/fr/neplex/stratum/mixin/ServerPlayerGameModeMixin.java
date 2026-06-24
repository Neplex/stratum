package fr.neplex.stratum.mixin;

import fr.neplex.stratum.StratumPositionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameModeMixin {

    @Inject(method = "useItem", at = @At("HEAD"))
    private void enterUseItem(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        StratumPositionContext.push(player.blockPosition());
    }

    @Inject(method = "useItem", at = @At("RETURN"))
    private void exitUseItem(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        StratumPositionContext.pop();
    }

    @Inject(method = "useItemOn", at = @At("HEAD"))
    private void enterUseItemOn(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        StratumPositionContext.push(hitResult.getBlockPos());
    }

    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void exitUseItemOn(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        StratumPositionContext.pop();
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void enterDestroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        StratumPositionContext.push(pos);
    }

    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void exitDestroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        StratumPositionContext.pop();
    }

}
