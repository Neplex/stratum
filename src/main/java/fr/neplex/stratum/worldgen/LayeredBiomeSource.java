package fr.neplex.stratum.worldgen;

import com.mojang.serialization.MapCodec;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class LayeredBiomeSource extends BiomeSource {
    private final LayerStack layerStack;

    LayeredBiomeSource(LayerStack layerStack) {
        this.layerStack = layerStack;
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return MapCodec.unit(this);
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return this.layerStack.stream().flatMap(l -> l.biomeSource().possibleBiomes().stream());
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        int globalBlockY = QuartPos.toBlock(y);
        WorldLayer layer = this.layerStack.layerForGlobalY(globalBlockY);
        return layer.biomeSource().getNoiseBiome(x, QuartPos.fromBlock(globalBlockY - layer.yOffset()), z, sampler);
    }
}
