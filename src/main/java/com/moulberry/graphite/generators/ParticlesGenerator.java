package com.moulberry.graphite.generators;

import com.google.gson.JsonObject;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ParticlesGenerator implements DataGenerator {
    @Override
    public String name() {
        return "particles";
    }

    @Override
    public JsonObject run() {
        JsonObject allParticlesJson = new JsonObject();

        Map<ParticleType, String> optionType = new HashMap<>();
        for (Field declaredField : ParticleTypes.class.getDeclaredFields()) {
            try {
                if (!declaredField.getType().isAssignableFrom(ParticleType.class)) {
                    continue;
                }

                declaredField.trySetAccessible();
                ParticleType particleType = (ParticleType) declaredField.get(null);
                var typeArguments = ((ParameterizedType)declaredField.getGenericType()).getActualTypeArguments();
                if (typeArguments.length > 0) {
                    Type genericType = typeArguments[0];
                    String[] splitName = genericType.getTypeName().split("\\.");
                    String name = splitName[splitName.length - 1];
                    name = name.replace("ParticleOptions", "")
                            .replace("ParticleOption", "")
                            .replace("Options", "")
                            .replace("Option", "");
                    name = name.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
                    optionType.put(particleType, name);
                }
            } catch (Exception ignored) {
            }
        }

        for (ParticleType<?> particle : BuiltInRegistries.PARTICLE_TYPE) {
            JsonObject particleJson = new JsonObject();

            if (optionType.containsKey(particle)) {
                particleJson.addProperty("particle_options", optionType.get(particle));
            }

            String key = DataGenerator.simplifyResourceLocation(BuiltInRegistries.PARTICLE_TYPE.getKey(particle));
            allParticlesJson.add(key, particleJson);
        }

        return allParticlesJson;
    }

}
