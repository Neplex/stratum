package fr.neplex.stratum.mixin;

import fr.neplex.stratum.Stratum;
import fr.neplex.stratum.StratumPositionContext;
import fr.neplex.stratum.worldgen.StratumChunkGenerator;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelMixin {

    @Shadow
    public abstract boolean isClientSide();

    @Unique
    protected ServerLevel stratum$getServerLevel() {
        if (this.isClientSide()) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return null;

            ClientLevel clientLevel = (ClientLevel) (Object) this;
            return server.getLevel(clientLevel.dimension());
        }

        return (ServerLevel) (Object) this;
    }

    @Unique
    protected Holder<DimensionType> stratum$getDimensionType() {
        ServerLevel serverLevel = this.stratum$getServerLevel();
        if (serverLevel == null) {
            Stratum.LOGGER.error("Could not get server level for dimension type");
            return null;
        }

        var chunkSource = serverLevel.getChunkSource();
        if (chunkSource == null) {
            Stratum.LOGGER.error("Could not get chunk source for dimension type");
            return null;
        }

        var generator = chunkSource.getGenerator();
        if (!(generator instanceof StratumChunkGenerator stratumGen)) return null;

        Integer y = StratumPositionContext.get();
        if (y == null) return null;

        return stratumGen.getDimensionTypeForY(y);
    }

    @Inject(method = "dimensionType", at = @At("HEAD"), cancellable = true)
    private void dimensionType(CallbackInfoReturnable<DimensionType> cir) {
        Holder<DimensionType> holder = this.stratum$getDimensionType();
        if (holder != null) {
            cir.setReturnValue(holder.value());
        }
    }

    @Inject(method = "dimensionTypeRegistration", at = @At("HEAD"), cancellable = true)
    private void dimensionTypeRegistration(CallbackInfoReturnable<Holder<DimensionType>> cir) {
        Holder<DimensionType> holder = this.stratum$getDimensionType();
        if (holder != null) {
            cir.setReturnValue(holder);
        }
    }
}
