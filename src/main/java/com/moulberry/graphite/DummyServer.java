package com.moulberry.graphite;

import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.serialization.Lifecycle;
import net.minecraft.Util;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.WorldStem;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.dedicated.DedicatedServerSettings;
import net.minecraft.server.level.progress.LoggerChunkProgressListener;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.Difficulty;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;

import java.io.File;
import java.net.Proxy;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DummyServer {

    public static MinecraftServer create() throws Exception {
        System.out.println("Running server at " + new File(".").getAbsolutePath());
        LevelStorageSource source = LevelStorageSource.createDefault(new File(".").toPath());
        LevelStorageSource.LevelStorageAccess access = source.createAccess("dummy");
        PackRepository packRepository = ServerPacksSource.createPackRepository(access);

        packRepository.reload();

        var featureFlags = FeatureFlags.REGISTRY.allFlags();
        GameRules gameRules = new GameRules(featureFlags);

        WorldDataConfiguration worldDataConfiguration = new WorldDataConfiguration(new DataPackConfig(List.of(), List.of()), featureFlags);
        LevelSettings levelSettings = new LevelSettings("Dummy", GameType.SPECTATOR, false, Difficulty.NORMAL, true, gameRules, worldDataConfiguration);
        WorldLoader.PackConfig packConfig = new WorldLoader.PackConfig(packRepository, worldDataConfiguration, false, true);
        WorldLoader.InitConfig initConfig = new WorldLoader.InitConfig(packConfig, Commands.CommandSelection.DEDICATED, 4);

        WorldStem worldStem = Util.blockUntilDone(executor -> WorldLoader.load(initConfig, dataLoadContext -> {
            Registry<LevelStem> registry = new MappedRegistry<>(Registries.LEVEL_STEM, Lifecycle.stable()).freeze();

            Holder.Reference<Biome> plains = dataLoadContext.datapackWorldgen().lookupOrThrow(Registries.BIOME).get(Biomes.PLAINS).get();
            Holder.Reference<DimensionType> overworld = dataLoadContext.datapackWorldgen().lookupOrThrow(Registries.DIMENSION_TYPE).get(BuiltinDimensionTypes.OVERWORLD).get();

            FlatLevelGeneratorSettings generatorSettings = new FlatLevelGeneratorSettings(Optional.empty(), plains, new ArrayList<>());
            WorldDimensions worldDimensions = new WorldDimensions(Map.of(LevelStem.OVERWORLD, new LevelStem(overworld, new FlatLevelSource(generatorSettings))));
            WorldDimensions.Complete complete = worldDimensions.bake(registry);

            return new WorldLoader.DataLoadOutput<>(new PrimaryLevelData(levelSettings, new WorldOptions(0L, false, false),
                    complete.specialWorldProperty(), complete.lifecycle()), complete.dimensionsRegistryAccess());
        }, WorldStem::new, Util.backgroundExecutor(), executor)).get();

        Path path2 = Paths.get("server.properties");
        DedicatedServerSettings dedicatedServerSettings = new DedicatedServerSettings(path2);

        File file = new File(".");
        Services services = Services.create(new YggdrasilAuthenticationService(Proxy.NO_PROXY), file);

        var server = MinecraftServer.spin((threadx) -> {
            DedicatedServer dedicatedServer = new DedicatedServer(threadx, access, packRepository, worldStem, dedicatedServerSettings, DataFixers.getDataFixer(),
                    services, LoggerChunkProgressListener::createFromGameruleRadius);
            dedicatedServer.setPort(25565);
            dedicatedServer.setDemo(false);
            return dedicatedServer;
        });

        while (!server.isReady()) {
            Thread.sleep(50L);
        }

        return server;
    }

}
