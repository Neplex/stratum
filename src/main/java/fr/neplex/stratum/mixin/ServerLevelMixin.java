package fr.neplex.stratum.mixin;

import fr.neplex.stratum.StratumPositionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {

    @Inject(method = "tickPrecipitation", at = @At("HEAD"))
    private void enterTickPrecipitation(BlockPos blockPos, CallbackInfo ci) {
        StratumPositionContext.push(blockPos);
    }

    @Inject(method = "tickPrecipitation", at = @At("RETURN"))
    private void exitTickPrecipitation(BlockPos blockPos, CallbackInfo ci) {
        StratumPositionContext.pop();
    }

    @Inject(method = "tickFluid", at = @At("HEAD"))
    private void enterTickFluid(BlockPos pos, Fluid fluid, CallbackInfo ci) {
        StratumPositionContext.push(pos);
    }

    @Inject(method = "tickFluid", at = @At("RETURN"))
    private void exitTickFluid(BlockPos pos, Fluid fluid, CallbackInfo ci) {
        StratumPositionContext.pop();
    }

    @Inject(method = "tickBlock", at = @At("HEAD"))
    private void enterTickBlock(BlockPos pos, Block block, CallbackInfo ci) {
        StratumPositionContext.push(pos);
    }

    @Inject(method = "tickBlock", at = @At("RETURN"))
    private void exitTickBlock(BlockPos pos, Block block, CallbackInfo ci) {
        StratumPositionContext.pop();
    }

    @Inject(method = "tickNonPassenger", at = @At("HEAD"))
    private void enterNonPassenger(Entity p_entity, CallbackInfo ci) {
        StratumPositionContext.push(p_entity.blockPosition());
    }

    @Inject(method = "tickNonPassenger", at = @At("RETURN"))
    private void exitNonPassenger(Entity p_entity, CallbackInfo ci) {
        StratumPositionContext.pop();
    }

    @Inject(method = "tickPassenger", at = @At("HEAD"))
    private void enterPassenger(Entity ridingEntity, Entity passengerEntity, CallbackInfo ci) {
        StratumPositionContext.push(ridingEntity.blockPosition());
    }

    @Inject(method = "tickPassenger", at = @At("RETURN"))
    private void exitPassenger(Entity ridingEntity, Entity passengerEntity, CallbackInfo ci) {
        StratumPositionContext.pop();
    }
}
