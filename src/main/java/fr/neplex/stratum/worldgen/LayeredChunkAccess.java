package fr.neplex.stratum.worldgen;

import fr.neplex.stratum.Stratum;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.ticks.TickContainerAccess;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * A chunk access that exposes one vertical layer of a larger stacked chunk.
 *
 * <p>All block and section coordinates are translated into the layer's local
 * height range before they are read from or written to the delegate chunk.</p>
 *
 * <p><b>Important:</b> This class extends {@link ChunkAccess} and cannot be used where
 * a {@link net.minecraft.world.level.chunk.ProtoChunk} is required. Specifically, vanilla
 * carver generation requires the concrete ProtoChunk instance, so this class should NOT
 * be used for the {@code applyCarvers} method. For carving, pass the original chunk directly.</p>
 *
 * @see WorldLayer
 * @see StratumChunkGenerator
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class LayeredChunkAccess extends ProtoChunk {
    /**
     * The underlying chunk access that stores the actual data.
     */
    private final ChunkAccess delegate;
    /**
     * The runtime layer this view represents.
     */
    private final WorldLayer layer;

    private final int startSection;
    private final int nbSections;

    /**
     * Creates a new layered chunk view for the specified layer.
     *
     * <p>This view provides a localized coordinate system for a single layer within
     * a stacked dimension. All operations are translated to the appropriate global
     * coordinates before being delegated to the underlying chunk.</p>
     *
     * @param delegate The underlying chunk access containing the actual chunk data
     * @param layer    The runtime layer defining the vertical range of this view
     */
    public LayeredChunkAccess(ChunkAccess delegate, WorldLayer layer) {
        this(delegate, layer, false);
    }

    public LayeredChunkAccess(ChunkAccess delegate, WorldLayer layer, boolean primeHeightmaps) {
        super(
                delegate.getPos(),
                delegate.getUpgradeData(),
                layer.accessor(),
                Stratum.registryAccess().registryOrThrow(Registries.BIOME),
                delegate.getBlendingData()
        );

        this.delegate = delegate;
        this.layer = layer;
        this.startSection = delegate.getSectionIndex(layer.globalMinY());
        this.nbSections = delegate.getSectionIndex(layer.globalMaxY()) - this.startSection;

        if (primeHeightmaps) {
            Heightmap.primeHeightmaps(this, Set.of(
                    Heightmap.Types.WORLD_SURFACE_WG,
                    Heightmap.Types.OCEAN_FLOOR_WG
            ));
        }
    }

    @Override
    @Nullable
    public BlockState setBlockState(BlockPos pos, BlockState state, boolean isMoving) {
        BlockPos globalPos = this.layer.translateToGlobal(pos);
        return this.delegate.setBlockState(globalPos, state, isMoving);
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        BlockPos globalPos = this.layer.translateToGlobal(pos);
        return this.delegate.getBlockState(globalPos);
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        BlockPos globalPos = this.layer.translateToGlobal(pos);
        return this.delegate.getFluidState(globalPos);
    }

    @Override
    @Nullable
    public BlockEntity getBlockEntity(BlockPos pos) {
        BlockPos globalPos = this.layer.translateToGlobal(pos);
        return this.delegate.getBlockEntity(globalPos);
    }

    @Override
    public void setBlockEntity(BlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        BlockPos globalPos = this.layer.translateToGlobal(pos);
        MutableBlockEntity mutable = (MutableBlockEntity) blockEntity;
        mutable.stratum$setWorldPosition(globalPos);
        this.delegate.setBlockEntity(blockEntity);
    }

    @Override
    public void addEntity(Entity entity) {
        double globalY = entity.getY() + this.layer.yOffset();
        entity.moveTo(entity.getX(), globalY, entity.getZ());
        this.delegate.addEntity(entity);
    }

    @Override
    public ChunkStatus getPersistedStatus() {
        return this.delegate.getPersistedStatus();
    }

    @Override
    public void removeBlockEntity(BlockPos pos) {
        BlockPos globalPos = this.layer.translateToGlobal(pos);
        this.delegate.removeBlockEntity(globalPos);
    }

    @Override
    @Nullable
    public CompoundTag getBlockEntityNbt(BlockPos pos) {
        BlockPos globalPos = this.layer.translateToGlobal(pos);
        return this.delegate.getBlockEntityNbt(globalPos);
    }

    @Override
    @Nullable
    public CompoundTag getBlockEntityNbtForSaving(BlockPos pos, HolderLookup.Provider registries) {
        BlockPos globalPos = this.layer.translateToGlobal(pos);
        return this.delegate.getBlockEntityNbtForSaving(globalPos, registries);
    }

    @Override
    public TickContainerAccess<Block> getBlockTicks() {
        return this.delegate.getBlockTicks();
    }

    @Override
    public TickContainerAccess<Fluid> getFluidTicks() {
        return this.delegate.getFluidTicks();
    }

    @Override
    public TicksToSave getTicksForSerialization() {
        return this.delegate.getTicksForSerialization();
    }

    @Override
    public void markPosForPostprocessing(BlockPos pos) {
        BlockPos globalPos = this.layer.translateToGlobal(pos);
        this.delegate.markPosForPostprocessing(globalPos);
    }

    @Override
    public void setUnsaved(boolean unsaved) {
        this.delegate.setUnsaved(unsaved);
    }

    @Override
    public boolean isUnsaved() {
        return this.delegate.isUnsaved();
    }

    @Override
    public void setLightCorrect(boolean lightCorrect) {
        this.delegate.setLightCorrect(lightCorrect);
    }

    @Override
    public long getInhabitedTime() {
        return this.delegate.getInhabitedTime();
    }

    @Override
    public void incrementInhabitedTime(long amount) {
        this.delegate.incrementInhabitedTime(amount);
    }

    @Override
    public void setInhabitedTime(long inhabitedTime) {
        this.delegate.setInhabitedTime(inhabitedTime);
    }

    @Override
    @Nullable
    public Level getLevel() {
        return this.delegate.getLevel();
    }

    @Override
    public LevelChunkSection[] getSections() {
        return Arrays.copyOfRange(
                delegate.getSections(),
                this.startSection,
                this.startSection + this.nbSections
        );
    }

    @Override
    @Nullable
    public CarvingMask getCarvingMask(GenerationStep.Carving step) {
        return this.delegate instanceof ProtoChunk proto ? proto.getCarvingMask(step) : null;
    }

    @Override
    public CarvingMask getOrCreateCarvingMask(GenerationStep.Carving step) {
        if (this.delegate instanceof ProtoChunk proto) {
            return proto.getOrCreateCarvingMask(step);
        }
        // Fallback if the delegate is a LevelChunk instead of a ProtoChunk
        return super.getOrCreateCarvingMask(step);
    }

    public Heightmap getOrCreateHeightmapUnprimed(Heightmap.Types type) {
        return this.heightmaps.computeIfAbsent(type, (t) -> new Heightmap(this, t));
    }

    public boolean hasPrimedHeightmap(Heightmap.Types type) {
        return this.heightmaps.get(type) != null;
    }

    public int getHeight(Heightmap.Types type, int x, int z) {
        Heightmap heightmap = this.heightmaps.get(type);
        if (heightmap == null) {
            Heightmap.primeHeightmaps(this, EnumSet.of(type));
            heightmap = this.heightmaps.get(type);
        }

        return heightmap.getFirstAvailable(x & 15, z & 15) - 1;
    }

    @Override
    @Nullable
    public StructureStart getStartForStructure(Structure structure) {
        return this.delegate.getStartForStructure(structure);
    }

    @Override
    public void setStartForStructure(Structure structure, StructureStart structureStart) {
        this.delegate.setStartForStructure(structure, structureStart);
    }

    @Override
    public LongSet getReferencesForStructure(Structure structure) {
        return this.delegate.getReferencesForStructure(structure);
    }

    @Override
    public void addReferenceForStructure(Structure structure, long reference) {
        this.delegate.addReferenceForStructure(structure, reference);
    }

    @Override
    public boolean hasAnyStructureReferences() {
        return this.delegate.hasAnyStructureReferences();
    }

    @Override
    public Map<Structure, LongSet> getAllReferences() {
        return this.delegate.getAllReferences();
    }

    @Override
    public void setAllReferences(Map<Structure, LongSet> structureReferencesMap) {
        this.delegate.setAllReferences(structureReferencesMap);
    }
}
