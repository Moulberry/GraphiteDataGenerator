package com.moulberry.graphite.generators;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

public interface DataGenerator {
    String name();
    JsonObject run();

    static String simplifyResourceLocation(ResourceLocation resourceLocation) {
        if (resourceLocation.getNamespace().equals("minecraft")) {
            return resourceLocation.getPath();
        } else {
            return resourceLocation.toString();
        }
    }
}
