package fr.neplex.stratum.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import fr.neplex.stratum.Config;
import fr.neplex.stratum.Stratum;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class StratumChunkGenerator extends ChunkGenerator {
    public static final MapCodec<StratumChunkGenerator> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    LevelStem.CODEC.listOf().fieldOf("layers").forGetter(g -> g.layerStems)
            ).apply(instance, instance.stable(StratumChunkGenerator::new))
    );

    private final List<LevelStem> layerStems;
    private final LayerStack layerStack;
    private final ConcurrentHashMap<WorldLayer, RandomState> localRandomStates;
    private final Map<ChunkAccess, Map<WorldLayer, LayeredChunkAccess>> layeredViews = new HashMap<>();

    public StratumChunkGenerator(List<LevelStem> layerStems) {
        this(List.copyOf(layerStems), LayerStack.fromLevelStems(layerStems));
    }

    private StratumChunkGenerator(
            List<LevelStem> layerStems,
            LayerStack layerStack
    ) {
        super(new LayeredBiomeSource(layerStack));

        if (layerStack.isEmpty()) throw new IllegalArgumentException("No layers specified");
        if (layerStack.size() > Config.MAX_LAYERS.get())
            throw new IllegalArgumentException("Too many layers specified");

        this.layerStems = layerStems;
        this.layerStack = layerStack;
        this.localRandomStates = new ConcurrentHashMap<>();

        Stratum.LOGGER.warn("Chunk bounds: minY={} maxY={} height={}", this.getMinY(), this.getMaxY(), this.getGenDepth());
    }

    public Holder<DimensionType> getDimensionTypeForY(int y) {
        DimensionType dimensionType = this.layerStack.dimensionTypeForY(y);
        return Holder.direct(dimensionType);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return MAP_CODEC;
    }

    @Override
    public int getMinY() {
        return this.layerStack.getMinY();
    }

    public int getMaxY() {
        return this.layerStack.getMaxY();
    }

    @Override
    public int getGenDepth() {
        return this.layerStack.getHeight();
    }

    @Override
    public int getSeaLevel() {
        return this.layerStack.getSeaLevel();
    }

    @Override
    public void addDebugScreenInfo(List<String> text, RandomState state, BlockPos pos) {
        WorldLayer layer = this.layerStack.layerForGlobalY(pos.getY());
        text.add("Stratum: layer@" + layer.globalMinY());
        BlockPos local = new BlockPos(pos.getX(), pos.getY() - layer.yOffset(), pos.getZ());
        layer.generator().addDebugScreenInfo(text, this.getLocalRandomState(layer, state), local);
    }

    @Override
    public CompletableFuture<ChunkAccess> createBiomes(RandomState state, Blender blender, StructureManager structures, ChunkAccess chunk) {
        Map<WorldLayer, LayeredChunkAccess> layeredViews = this.layeredViews.computeIfAbsent(chunk, this::createLayeredViews);

        return CompletableFuture.supplyAsync(Util.wrapThreadWithTaskName("stratum$init_biomes", () -> {
            for (WorldLayer layer : this.layerStack) {
                RandomState localState = this.getLocalRandomState(layer, state);
                LayeredChunkAccess layeredAccess = layeredViews.get(layer);
                layer.generator().createBiomes(localState, blender, structures, layeredAccess).join();
            }
            return chunk;
        }), Util.backgroundExecutor());
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState state, StructureManager structures, ChunkAccess chunk) {
        Map<WorldLayer, LayeredChunkAccess> layeredViews = this.layeredViews.computeIfAbsent(chunk, this::createLayeredViews);

        return CompletableFuture.supplyAsync(Util.wrapThreadWithTaskName("stratum$wgen_fill_noise", () -> {
            for (WorldLayer layer : this.layerStack) {
                RandomState localState = this.getLocalRandomState(layer, state);
                LayeredChunkAccess layeredAccess = layeredViews.get(layer);
                layer.generator().fillFromNoise(blender, localState, structures, layeredAccess).join();
            }
            return chunk;
        }), Util.backgroundExecutor());
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structures, RandomState state, ChunkAccess chunk) {
        Map<WorldLayer, LayeredChunkAccess> layeredViews = this.layeredViews.computeIfAbsent(chunk, this::createLayeredViews);

        this.layerStack
                .forEach(layer -> {
                    RandomState localState = this.getLocalRandomState(layer, state);
                    LayeredChunkAccess layeredAccess = layeredViews.get(layer);
                    layer.generator().buildSurface(region, structures, localState, layeredAccess);
                });
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState state, BiomeManager biomes, StructureManager structures, ChunkAccess chunk, GenerationStep.Carving stage) {
        Map<WorldLayer, LayeredChunkAccess> layeredViews = this.layeredViews.computeIfAbsent(chunk, this::createLayeredViews);

        this.layerStack
                .forEach(layer -> {
                    RandomState localState = this.getLocalRandomState(layer, state);
                    LayeredChunkAccess layeredAccess = layeredViews.get(layer);
                    layer.generator().applyCarvers(region, seed, localState, biomes, structures, layeredAccess, stage);
                });
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structures) {
        Map<WorldLayer, LayeredChunkAccess> layeredViews = this.layeredViews.computeIfAbsent(chunk, this::createPrimedLayeredViews);

        this.layerStack
                .forEach(layer -> {
                    LayeredWorldGenLevel layeredLevel = new LayeredWorldGenLevel(level, layer);
                    LayeredChunkAccess layeredAccess = layeredViews.get(layer);
                    try {
                        layer.generator().applyBiomeDecoration(layeredLevel, layeredAccess, structures);
                    } catch (Exception e) {
                        Stratum.LOGGER.error(e.getMessage());
                    }
                });
        this.layeredViews.remove(chunk);
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
        this.layerStack.forEach(l -> l.generator().spawnOriginalMobs(region));
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor height, RandomState state) {
        int highest = height.getMinBuildHeight();
        for (WorldLayer layer : this.layerStack) {
            int local = layer.generator().getBaseHeight(x, z, type, layer.accessor(), this.getLocalRandomState(layer, state));
            highest = Math.max(highest, local + layer.yOffset());
        }
        return highest;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor height, RandomState state) {
        WorldLayer target = null;
        int highest = Integer.MIN_VALUE;

        for (WorldLayer layer : this.layerStack) {
            RandomState localState = this.getLocalRandomState(layer, state);
            int local = layer.generator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, layer.accessor(), localState);
            int global = local + layer.yOffset();
            if (global >= highest) {
                highest = global;
                target = layer;
            }
        }

        NoiseColumn col = target.generator().getBaseColumn(x, z, target.accessor(), this.getLocalRandomState(target, state));
        BlockState[] states = new BlockState[target.accessor().getHeight()];
        for (int i = 0; i < states.length; i++) {
            states[i] = col.getBlock(target.accessor().getMinBuildHeight() + i);
        }
        return new NoiseColumn(target.accessor().getMinBuildHeight() + target.yOffset(), states);
    }

    private RandomState getLocalRandomState(WorldLayer layer, RandomState masterState) {
        return this.localRandomStates.computeIfAbsent(layer, l -> l.getLocalRandomState(masterState));
    }

    private Map<WorldLayer, LayeredChunkAccess> createLayeredViews(ChunkAccess masterChunk) {
        Map<WorldLayer, LayeredChunkAccess> views = new HashMap<>();
        for (WorldLayer layer : this.layerStack) {
            views.put(layer, new LayeredChunkAccess(masterChunk, layer));
        }
        return views;
    }

    private Map<WorldLayer, LayeredChunkAccess> createPrimedLayeredViews(ChunkAccess masterChunk) {
        Map<WorldLayer, LayeredChunkAccess> views = new HashMap<>();
        for (WorldLayer layer : this.layerStack) {
            views.put(layer, new LayeredChunkAccess(masterChunk, layer, true));
        }
        return views;
    }

}
