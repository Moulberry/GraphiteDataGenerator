package com.moulberry.graphite;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import com.moulberry.graphite.generators.*;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.Util;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class Generator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator generator) {
		MinecraftServer server = null;
		try {
			server = DummyServer.create();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		FabricDataGenerator.Pack pack = generator.createPack();
		pack.addProvider(this.createProvider(new BlockGenerator()));
		pack.addProvider(this.createProvider(new FluidGenerator()));
		pack.addProvider(this.createProvider(new BuiltinGenerator(server.overworld())));
		pack.addProvider(this.createProvider(new EntityGenerator(server.overworld())));
		pack.addProvider(this.createProvider(new TypesGenerator()));
		pack.addProvider(this.createProvider(new ItemsGenerator()));
		pack.addProvider(this.createProvider(new ParticlesGenerator()));
	}

	public DataProvider.Factory<?> createProvider(DataGenerator generator) {
		return packOutput -> {
			PackOutput.PathProvider pathProvider = packOutput.createPathProvider(PackOutput.Target.DATA_PACK, "graphite");
			Path path = pathProvider.json(ResourceLocation.fromNamespaceAndPath("graphite", generator.name()));

			return new DataProvider() {
				@Override
				public CompletableFuture<?> run(CachedOutput cachedOutput) {
					return CompletableFuture.runAsync(() -> {
						try {
							ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
							HashingOutputStream hashingOutputStream = new HashingOutputStream(Hashing.sha1(), byteArrayOutputStream);
							JsonWriter jsonWriter = new JsonWriter(new OutputStreamWriter(hashingOutputStream, StandardCharsets.UTF_8));

							try {
								jsonWriter.setSerializeNulls(false);
								jsonWriter.setIndent("  ");
								GsonHelper.writeValue(jsonWriter, generator.run(), null);
							} catch (Throwable var9) {
								try {
									jsonWriter.close();
								} catch (Throwable var8) {
									var9.addSuppressed(var8);
								}

								throw var9;
							}

							jsonWriter.close();
							cachedOutput.writeIfNeeded(path, byteArrayOutputStream.toByteArray(), hashingOutputStream.hash());
						} catch (IOException var10) {
							LOGGER.error("Failed to save file to {}", path, var10);
						}
					}, Util.backgroundExecutor().forName("saveStable"));
				}

				@Override
				public String getName() {
					return generator.name();
				}
			};
		};
	}

}
