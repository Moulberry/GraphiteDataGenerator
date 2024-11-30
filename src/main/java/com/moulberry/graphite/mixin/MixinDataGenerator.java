package com.moulberry.graphite.mixin;

import net.minecraft.data.DataGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DataGenerator.class)
public class MixinDataGenerator {

    @Inject(method = "run", at = @At("RETURN"))
    public void run(CallbackInfo ci) {
        System.exit(0);
    }

}
