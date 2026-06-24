package fr.neplex.stratum.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * Represents a world layer in a stacked dimension configuration.
 *
 * <p>A layer defines a vertical slice of the world that corresponds to a specific
 * dimension. It tracks the global Y range where the dimension's content should be placed
 * and the offset needed to translate between local and global coordinates.</p>
 *
 * <p>This is used by {@link LayeredChunkAccess} to provide a localized coordinate system
 * for world generation within a specific layer.</p>
 *
 * @param dimensionType The dimensionType for this layer
 * @param generator     The chunk generator for this layer
 * @param globalMinY    The minimum global Y coordinate (inclusive)
 * @param height        The absolute height span of this layer in blocks.
 * @param yOffset       The Y offset for coordinate translation
 */
public record WorldLayer(
        DimensionType dimensionType,
        ChunkGenerator generator,
        int globalMinY,
        int height,
        int yOffset
) {

    public LevelHeightAccessor accessor() {
        return LevelHeightAccessor.create(this.generator().getMinY(), this.generator().getGenDepth());
    }

    public BiomeSource biomeSource() {
        return this.generator().getBiomeSource();
    }

    public int globalMaxY() {
        return this.globalMinY + this.height;
    }

    /**
     * Gets the minimum native Y coordinate the child generator expects to work with.
     * (e.g., if a Nether layer is placed globally at -128, but natively generates from 0 to 128,
     * with a yOffset of -128: 0 = -128 - (-128))
     */
    public int nativeMinY() {
        return this.globalMinY - this.yOffset;
    }

    public int nativeMaxY() {
        return this.globalMaxY() - this.yOffset;
    }

    /**
     * Checks if a global Y coordinate falls within this layer's range.
     *
     * @param y The global Y coordinate to check
     * @return true if the Y coordinate is within this layer's range
     */
    public boolean containsGlobalY(int y) {
        return y >= this.globalMinY && y < this.globalMaxY();
    }

    public RandomState getLocalRandomState(RandomState masterState) {
        if (this.generator() instanceof NoiseBasedChunkGenerator noiseGen) {
            ReadableRandomState mixin = (ReadableRandomState) ((Object) masterState);
            return RandomState.create(
                    noiseGen.generatorSettings().value(),
                    mixin.stratum$getNoises(),
                    mixin.stratum$getSeed()
            );
        }
        return masterState;
    }

    /**
     * Translates a local block position to the corresponding global position.
     *
     * <p>Adds the layer's Y offset to convert from local layer coordinates to
     * global world coordinates.</p>
     *
     * @param pos The local position within the layer
     * @return The corresponding global position
     */
    public BlockPos translateToGlobal(BlockPos pos) {
        return pos.offset(0, this.yOffset(), 0);
    }

    public int translateToGlobal(int y) {
        return Math.addExact(y, this.yOffset());
    }
}
