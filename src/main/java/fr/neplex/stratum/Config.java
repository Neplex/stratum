package fr.neplex.stratum;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuration class for Stratum mod.
 *
 * <p>This class defines all configurable options for the Stratum mod using NeoForge's
 * configuration system. Configuration values are automatically synchronized between
 * server and client when running in a multiplayer environment.</p>
 */
public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue DEBUG_LOGGING = BUILDER
            .comment("Enable debug logging for Stratum. Useful for troubleshooting layer issues.")
            .define("debugLogging", false);

    public static final ModConfigSpec.IntValue MAX_LAYERS = BUILDER
            .comment("Maximum number of layers that can be stacked in a single dimension.",
                    "Increase this if you need more than the default number of layers.")
            .defineInRange("maxLayers", 4, 1, 64);

    static final ModConfigSpec SPEC = BUILDER.build();
}
