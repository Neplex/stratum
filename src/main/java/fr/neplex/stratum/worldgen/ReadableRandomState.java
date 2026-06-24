package fr.neplex.stratum.worldgen;

import net.minecraft.core.HolderGetter;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public interface ReadableRandomState {
    HolderGetter<NormalNoise.NoiseParameters> stratum$getNoises();

    long stratum$getSeed();
}
