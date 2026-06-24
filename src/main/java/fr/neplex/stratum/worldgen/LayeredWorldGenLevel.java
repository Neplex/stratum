package fr.neplex.stratum.worldgen;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.LevelTickAccess;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class LayeredWorldGenLevel implements WorldGenLevel {

    private final WorldGenLevel delegate;
    private final WorldLayer layer;
    private final Map<ChunkPos, LayeredChunkAccess> chunkCache;

    public LayeredWorldGenLevel(WorldGenLevel delegate, WorldLayer layer) {
        this.delegate = delegate;
        this.layer = layer;
        this.chunkCache = new HashMap<>();
    }

    @Override
    public long getSeed() {
        return this.delegate.getSeed();
    }

    @Override
    public ServerLevel getLevel() {
        return this.delegate.getLevel();
    }

    @Override
    public long nextSubTickCount() {
        return this.delegate.nextSubTickCount();
    }

    @Override
    public LevelTickAccess<Block> getBlockTicks() {
        return this.delegate.getBlockTicks();
    }

    @Override
    public LevelTickAccess<Fluid> getFluidTicks() {
        return this.delegate.getFluidTicks();
    }

    @Override
    public LevelData getLevelData() {
        return this.delegate.getLevelData();
    }

    @Override
    public DifficultyInstance getCurrentDifficultyAt(BlockPos blockPos) {
        BlockPos globalPos = this.layer.translateToGlobal(blockPos);
        return this.delegate.getCurrentDifficultyAt(globalPos);
    }

    @Override
    @Nullable
    public MinecraftServer getServer() {
        return this.delegate.getServer();
    }

    @Override
    public ChunkSource getChunkSource() {
        return this.delegate.getChunkSource();
    }

    @Override
    public RandomSource getRandom() {
        return this.delegate.getRandom();
    }

    @Override
    public void playSound(@Nullable Player player, BlockPos blockPos, SoundEvent soundEvent, SoundSource soundSource, float v, float v1) {
        BlockPos globalPos = this.layer.translateToGlobal(blockPos);
        this.delegate.playSound(player, globalPos, soundEvent, soundSource, v, v1);
    }

    @Override
    public void addParticle(ParticleOptions particleData, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        double globalY = y + this.layer.yOffset();
        this.delegate.addParticle(particleData, x, globalY, z, xSpeed, ySpeed, zSpeed);
    }

    @Override
    public void levelEvent(@Nullable Player player, int type, BlockPos pos, int data) {
        BlockPos globalPos = this.layer.translateToGlobal(pos);
        this.delegate.levelEvent(player, type, globalPos, data);
    }

    @Override
    public void gameEvent(Holder<GameEvent> holder, Vec3 pos, GameEvent.Context context) {
        Vec3 globalPos = pos.add(0, this.layer.yOffset(), 0);
        this.delegate.gameEvent(holder, globalPos, context);
    }

    @Override
    public float getShade(Direction direction, boolean b) {
        return this.delegate.getShade(direction, b);
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return this.delegate.getLightEngine();
    }

    @Override
    public WorldBorder getWorldBorder() {
        return this.delegate.getWorldBorder();
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(BlockPos blockPos) {
        BlockPos globalPos = this.layer.translateToGlobal(blockPos);
        return this.delegate.getBlockEntity(globalPos);
    }

    @Override
    public BlockState getBlockState(BlockPos blockPos) {
        BlockPos globalPos = this.layer.translateToGlobal(blockPos);
        return this.delegate.getBlockState(globalPos);
    }

    @Override
    public FluidState getFluidState(BlockPos blockPos) {
        BlockPos globalPos = this.layer.translateToGlobal(blockPos);
        return this.delegate.getFluidState(globalPos);
    }

    @Override
    public List<Entity> getEntities(@Nullable Entity entity, AABB aabb, Predicate<? super Entity> predicate) {
        return this.delegate.getEntities(entity, aabb, predicate);
    }

    @Override
    public <T extends Entity> List<T> getEntities(EntityTypeTest<Entity, T> entityTypeTest, AABB aabb, Predicate<? super T> predicate) {
        return this.delegate.getEntities(entityTypeTest, aabb, predicate);
    }

    @Override
    public List<? extends Player> players() {
        return this.delegate.players();
    }

    @Override
    public ChunkAccess getChunk(int x, int z) {
        ChunkPos chunkPos = new ChunkPos(x, z);
        return this.chunkCache.computeIfAbsent(chunkPos, pos -> {
            ChunkAccess chunk = this.delegate.getChunk(x, z);
            return new LayeredChunkAccess(chunk, this.layer);
        });
    }

    @Override
    public ChunkAccess getChunk(int x, int z, ChunkStatus status, boolean require) {
        ChunkPos chunkPos = new ChunkPos(x, z);
        return this.chunkCache.computeIfAbsent(chunkPos, pos -> {
            ChunkAccess chunk = this.delegate.getChunk(x, z, status, require);
            return new LayeredChunkAccess(chunk, this.layer);
        });
    }

    @Override
    public int getHeight(Heightmap.Types heightmapType, int x, int z) {
        ChunkAccess chunk = this.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));
        return chunk.getHeight(heightmapType, x & 15, z & 15) + 1;
    }

    @Override
    public int getSkyDarken() {
        return this.delegate.getSkyDarken();
    }

    @Override
    public BiomeManager getBiomeManager() {
        return this.delegate.getBiomeManager();
    }

    @Override
    public Holder<Biome> getBiome(BlockPos pos) {
        BlockPos globalPos = this.layer.translateToGlobal(pos);
        return this.delegate.getBiome(globalPos);
    }

    @Override
    public Holder<Biome> getUncachedNoiseBiome(int x, int y, int z) {
        int globalBlockY = this.layer.translateToGlobal(y);
        BiomeSource biomeSource = this.layer.generator().getBiomeSource();
        Climate.Sampler sampler = this.delegate.getLevel().getChunkSource().randomState().sampler();
        return biomeSource.getNoiseBiome(x, globalBlockY, z, sampler);
    }

    @Override
    public boolean isClientSide() {
        return this.delegate.isClientSide();
    }

    @Override
    public int getSeaLevel() {
        return this.delegate.getSeaLevel();
    }

    @Override
    public DimensionType dimensionType() {
        return this.layer.dimensionType();
    }

    @Override
    public RegistryAccess registryAccess() {
        return this.delegate.registryAccess();
    }

    @Override
    public FeatureFlagSet enabledFeatures() {
        return this.delegate.enabledFeatures();
    }

    @Override
    public boolean isStateAtPosition(BlockPos blockPos, Predicate<BlockState> predicate) {
        BlockPos globalPos = this.layer.translateToGlobal(blockPos);
        return this.delegate.isStateAtPosition(globalPos, predicate);
    }

    @Override
    public boolean isFluidAtPosition(BlockPos blockPos, Predicate<FluidState> predicate) {
        BlockPos globalPos = this.layer.translateToGlobal(blockPos);
        return this.delegate.isFluidAtPosition(globalPos, predicate);
    }

    @Override
    public boolean setBlock(BlockPos pos, BlockState state, int flags, int recursionLeft) {
        BlockPos globalPos = this.layer.translateToGlobal(pos);
        return this.delegate.setBlock(globalPos, state, flags, recursionLeft);
    }

    @Override
    public boolean removeBlock(BlockPos pos, boolean isMoving) {
        BlockPos globalPos = this.layer.translateToGlobal(pos);
        return this.delegate.removeBlock(globalPos, isMoving);
    }

    @Override
    public boolean destroyBlock(BlockPos pos, boolean dropBlock, @Nullable Entity entity, int recursionLeft) {
        BlockPos globalPos = this.layer.translateToGlobal(pos);
        return this.delegate.destroyBlock(globalPos, dropBlock, entity, recursionLeft);
    }

    @Override
    public int getMinBuildHeight() {
        return this.layer.accessor().getMinBuildHeight();
    }

    @Override
    public int getMaxBuildHeight() {
        return this.layer.accessor().getMaxBuildHeight();
    }
}
