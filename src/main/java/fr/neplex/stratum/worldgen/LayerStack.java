package fr.neplex.stratum.worldgen;

import fr.neplex.stratum.Stratum;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;

import java.util.ArrayList;
import java.util.List;

public class LayerStack extends ArrayList<WorldLayer> {

    public int getMinY() {
        return this.getLast().globalMinY();
    }

    public int getMaxY() {
        return this.getFirst().globalMaxY();
    }

    public int getHeight() {
        return this.getMaxY() - this.getMinY();
    }

    public int getSeaLevel() {
        return this.getFirst().generator().getSeaLevel();
    }

    public WorldLayer layerForGlobalY(int y) {
        if (y >= this.getFirst().globalMinY()) return this.getFirst();

        for (WorldLayer layer : this) {
            if (layer.containsGlobalY(y)) return layer;
        }

        return this.getLast();
    }

    public DimensionType dimensionTypeForY(int y) {
        return this.layerForGlobalY(y).dimensionType();
    }

    public static LayerStack fromLevelStems(List<LevelStem> levelStems) {
        ChunkGenerator firstGen = levelStems.getFirst().generator();

        int currentTop = firstGen.getMinY() + firstGen.getGenDepth();
        int minY = firstGen.getMinY();
        for (int i = 1; i < levelStems.size(); i++) {
            minY -= levelStems.get(i).generator().getGenDepth();
        }
        int totalHeight = currentTop - minY;

        LayerStack resolved = new LayerStack();
        for (int i = 0; i < levelStems.size(); i++) {
            DimensionType levelDimensionType = levelStems.get(i).type().value();
            DimensionType dimensionType = adaptDimensionType(levelDimensionType, minY, totalHeight);
            ChunkGenerator generator = levelStems.get(i).generator();

            int height = generator.getGenDepth();
            int currentBottom = currentTop - height;
            int yOffset = currentBottom - generator.getMinY();

            WorldLayer worldLayer = new WorldLayer(dimensionType, generator, currentBottom, height, yOffset);
            resolved.add(worldLayer);

            Stratum.LOGGER.warn("Resolved layer {}: [Y: {} to Y: {}] (Offset: {})", i, currentBottom, currentTop, yOffset);

            // The bottom of this layer becomes the top of the next
            currentTop = currentBottom;
        }

        return resolved;
    }

    private static DimensionType adaptDimensionType(DimensionType dimensionType, int minY, int height) {
        return new DimensionType(
                dimensionType.fixedTime(),
                dimensionType.hasSkyLight(),
                dimensionType.hasCeiling(),
                dimensionType.ultraWarm(),
                dimensionType.natural(),
                1.0,
                dimensionType.bedWorks(),
                dimensionType.respawnAnchorWorks(),
                minY,
                height,
                height,
                dimensionType.infiniburn(),
                dimensionType.effectsLocation(),
                dimensionType.ambientLight(),
                dimensionType.monsterSettings()
        );
    }
}
