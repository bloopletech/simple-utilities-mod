package net.johnvictorfs.simple_utilities.hud;

import net.johnvictorfs.simple_utilities.helpers.Colors;
import com.google.common.collect.Lists;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.johnvictorfs.simple_utilities.mixin.GameClientMixin;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import net.minecraft.client.util.math.MatrixStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class GameInfoHud {
    private final MinecraftClient client;
    private final TextRenderer textRenderer;
    private ClientPlayerEntity player;
    private MatrixStack matrices;

    public GameInfoHud(MinecraftClient client) {
        this.client = client;
        textRenderer = client.textRenderer;
    }

    public void draw(MatrixStack matrices) {
        if(client.options.debugEnabled) return;

        client.getProfiler().push("gameInfoHud");

        player = client.player;
        this.matrices = matrices;

        drawInfos();

        client.getProfiler().pop();
    }

    private void drawInfos() {
        // Draw lines of Array of Game info in the screen

        List<String> gameInfo = getGameInfo();
        drawEquipementInfo();

        int lineHeight = textRenderer.fontHeight + 2;
        int top = 0;
        int left = 4;

        for (String line : gameInfo) {
            textRenderer.draw(matrices, line, left, top + 4, Colors.lightGray);
            top += lineHeight;
        }

        if (player.isSprinting()) {
            final String sprintingText = "Sprinting";

            int maxLineHeight = Math.max(10, textRenderer.getWidth(sprintingText));
            maxLineHeight = (int) (Math.ceil(maxLineHeight / 5.0D + 0.5D) * 5);
            int scaleHeight = client.getWindow().getScaledHeight();
            int sprintingTop = scaleHeight - maxLineHeight;

            // Sprinting Info
            textRenderer.draw(matrices, sprintingText, 2, sprintingTop + 20, Colors.lightGray);
        }
    }

    private static String capitalize(String str) {
        // Capitalize first letter of a String
        if (str == null) return null;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private static String getOffset(Direction facing) {
        String offset = "";

        if (facing.getOffsetX() > 0) {
            offset += "+X";
        } else if (facing.getOffsetX() < 0) {
            offset += "-X";
        }

        if (facing.getOffsetZ() > 0) {
            offset += " +Z";
        } else if (facing.getOffsetZ() < 0) {
            offset += " -Z";
        }

        return offset.trim();
    }

    private String zeroPadding(int number) {
        return (number >= 10) ? Integer.toString(number) : String.format("0%s", number);
    }

    private String secondsToString(int pTime) {
        final int min = pTime / 60;
        final int sec = pTime - (min * 60);

        final String strMin = zeroPadding(min);
        final String strSec = zeroPadding(sec);
        return String.format("%s:%s", strMin, strSec);
    }

    private void drawStatusEffectInfo() {
        if (client.player != null) {
            Map<StatusEffect, StatusEffectInstance> effects = client.player.getActiveStatusEffects();

            for (Map.Entry<StatusEffect, StatusEffectInstance> effect : effects.entrySet()) {
                String effectName = I18n.translate(effect.getKey().getTranslationKey());

                String duration = secondsToString(effect.getValue().getDuration() / 20);

                int color = effect.getKey().getColor();

                textRenderer.draw(matrices, effectName + " " + duration, 40, 200, color);
            }
        }
    }

    private void drawEquipementInfo() {
        List<ItemStack> equippedItems = new ArrayList<>();
        PlayerInventory inventory = player.inventory;
        int maxLineHeight = Math.max(10, textRenderer.getWidth(""));

        ItemStack mainHandItem = inventory.getMainHandStack();
        maxLineHeight = Math.max(maxLineHeight, textRenderer.getWidth(I18n.translate(mainHandItem.getTranslationKey())));
        equippedItems.add(mainHandItem);

        for (ItemStack secondHandItem : inventory.offHand) {
            maxLineHeight = Math.max(maxLineHeight, textRenderer.getWidth(I18n.translate(secondHandItem.getTranslationKey())));
            equippedItems.add(secondHandItem);
        }

        for (ItemStack armourItem : player.inventory.armor) {
            maxLineHeight = Math.max(maxLineHeight, textRenderer.getWidth(I18n.translate(armourItem.getTranslationKey())));
            equippedItems.add(armourItem);
        }

        maxLineHeight = (int) (Math.ceil(maxLineHeight / 5.0D + 0.5D) * 5);
        int itemTop = client.getWindow().getScaledHeight() - maxLineHeight;

        int lineHeight = textRenderer.fontHeight + 6;

        // Draw in order Helmet -> Chestplate -> Leggings -> Boots
        for (ItemStack equippedItem : Lists.reverse(equippedItems)) {
            if (equippedItem.getItem().equals(Blocks.AIR.asItem())) {
                // Skip empty slots
                continue;
            }

            client.getItemRenderer().renderGuiItemIcon(equippedItem, 2, itemTop - 68);

            if (equippedItem.getMaxDamage() != 0) {
                int currentDurability = equippedItem.getMaxDamage() - equippedItem.getDamage();

                String itemDurability = currentDurability + "/" + equippedItem.getMaxDamage();

                // Default Durability Color
                int color = Colors.lightGray;

                if (currentDurability < equippedItem.getMaxDamage()) {
                    // Start as Green if item has lost at least 1 durability
                    color = Colors.lightGreen;
                }
                if (currentDurability <= (equippedItem.getMaxDamage() / 1.5)) {
                    color = Colors.lightYellow;
                }
                if (currentDurability <= (equippedItem.getMaxDamage() / 2.5)) {
                    color = Colors.lightOrange;
                }
                if (currentDurability <= (equippedItem.getMaxDamage()) / 4) {
                    color = Colors.lightRed;
                }

                textRenderer.draw(matrices, itemDurability, 22, itemTop - 64, color);
            } else {
                int count = equippedItem.getCount();

                if (count > 1) {
                    String itemCount = String.valueOf(count);
                    textRenderer.draw(matrices, itemCount, 22, itemTop - 64, Colors.lightGray);
                }
            }

            itemTop += lineHeight;
        }
    }

    private static String parseTime(long time) {
        long hours = (time / 1000 + 6) % 24;
        long minutes = (time % 1000) * 60 / 1000;
        String ampm = "AM";

        if (hours >= 12) {
            hours -= 12;
            ampm = "PM";
        }

        if (hours >= 12) {
            hours -= 12;
            ampm = "AM";
        }

        if (hours == 0) hours = 12;

        String mm = "0" + minutes;
        mm = mm.substring(mm.length() - 2);

        return hours + ":" + mm + " " + ampm;
    }

    private List<String> getGameInfo() {
        List<String> gameInfo = new ArrayList<>();

        Direction facing = player.getHorizontalFacing();

        String coordsFormat = "%.0f, %.0f, %.0f %s";

        String direction = "(" + capitalize(facing.asString()) + " " + getOffset(facing) + ")";

        // Coordinates and Direction info
        gameInfo.add(String.format(coordsFormat, player.getX(), player.getY(), player.getZ(), direction));

        // Get everything from fps debug string until the 's' from 'fps'
        // gameInfo.add(client.fpsDebugString.substring(0, client.fpsDebugString.indexOf("s") + 1));
        gameInfo.add(String.format("%d fps", ((GameClientMixin) client.getInstance()).getCurrentFps()));

        // Get biome info
        if (client.world != null) {
            gameInfo.add(capitalize(client.world.getBiome(player.getBlockPos()).getCategory().getName()) + " Biome");

            // Add current parsed time
            gameInfo.add(parseTime(client.world.getTimeOfDay()));
        }

        return gameInfo;
    }
}