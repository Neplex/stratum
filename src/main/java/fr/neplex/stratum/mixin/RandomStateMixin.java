package fr.neplex.stratum.mixin;

import fr.neplex.stratum.worldgen.ReadableRandomState;
import net.minecraft.core.HolderGetter;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RandomState.class)
public class RandomStateMixin implements ReadableRandomState {

    @Final
    @Shadow
    private HolderGetter<NormalNoise.NoiseParameters> noises;

    @Unique
    private long stratum$capturedSeed;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void captureSeed(NoiseGeneratorSettings settings, HolderGetter<NormalNoise.NoiseParameters> noises, long levelSeed, CallbackInfo ci) {
        this.stratum$capturedSeed = levelSeed;
    }

    @Override
    public HolderGetter<NormalNoise.NoiseParameters> stratum$getNoises() {
        return this.noises;
    }

    @Override
    public long stratum$getSeed() {
        return this.stratum$capturedSeed;
    }
}
