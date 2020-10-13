package net.johnvictorfs.simple_utilities.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.johnvictorfs.simple_utilities.hud.GameInfoHud;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(value = InGameHud.class)
public abstract class GameInfoMixin {
    private GameInfoHud hudInfo;

    @Inject(method = "<init>(Lnet/minecraft/client/MinecraftClient;)V", at = @At(value = "RETURN"))
    private void onInit(MinecraftClient client, CallbackInfo ci) {
        // Start Mixin
        System.out.println("Init Coordinates Mixin");
        this.hudInfo = new GameInfoHud(client);
    }

    @Inject(method = "renderStatusEffectOverlay", at = @At(value = "HEAD"))
    private void onDraw(MatrixStack matrices, CallbackInfo ci) {
        // Draw Game info on every GameHud render
        this.hudInfo.draw(matrices);
    }
}