package fr.neplex.stratum;

import com.mojang.logging.LogUtils;
import fr.neplex.stratum.worldgen.StratumChunkGenerator;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.util.Objects;

import static net.neoforged.neoforge.common.NeoForge.EVENT_BUS;

@Mod(Stratum.MODID)
public final class Stratum {
    public static final String MODID = "stratum";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static volatile RegistryAccess registryAccess;

    public Stratum(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        modEventBus.addListener(this::onRegister);
        modEventBus.addListener(this::onConfigLoad);
        EVENT_BUS.addListener(this::onServerStopped);
    }

    @Nonnull
    public static RegistryAccess registryAccess() {
        if (registryAccess != null) return registryAccess;
        MinecraftServer server = Objects.requireNonNull(ServerLifecycleHooks.getCurrentServer());
        return server.registryAccess();
    }

    private void onRegister(RegisterEvent event) {
        event.register(Registries.CHUNK_GENERATOR,
                ResourceLocation.fromNamespaceAndPath(MODID, "layered_dimensions"),
                () -> StratumChunkGenerator.MAP_CODEC);
    }

    private void onConfigLoad(ModConfigEvent event) {
        if (!event.getConfig().getSpec().equals(Config.SPEC)) return;

        Level level = Config.DEBUG_LOGGING.get() ? Level.DEBUG : Level.INFO;
        Configurator.setLevel(Stratum.class.getName(), level);
    }

    private void onServerStopped(ServerStoppedEvent event) {
        registryAccess = null;
    }
}
