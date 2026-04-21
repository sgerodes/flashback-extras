package io.serge.flashbackextras;

import com.mojang.blaze3d.platform.InputConstants;
import io.serge.flashbackextras.config.FlashbackExtrasConfig;
import io.serge.flashbackextras.export.ExportCapabilityWarmup;
import io.serge.flashbackextras.screen.FlashbackExtrasConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FlashbackExtras implements ClientModInitializer {
    public static final String MOD_ID = "flashbackextras";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final KeyMapping.Category KEYBIND_CATEGORY = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath(MOD_ID, "keybind")
    );

    private static final KeyMapping OPEN_MENU_KEYBIND = KeyMappingHelper.registerKeyMapping(new KeyMapping(
        "flashbackextras.keybind.open_menu",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_P,
        KEYBIND_CATEGORY
    ));

    @Override
    public void onInitializeClient() {
        FlashbackExtrasConfig.load();
        ExportCapabilityWarmup.initialize();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_MENU_KEYBIND.consumeClick()) {
                long window = client.getWindow().handle();
                boolean rightAltDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS;

                if (rightAltDown && client.screen == null) {
                    client.setScreen(new FlashbackExtrasConfigScreen(null));
                }
            }
        });
    }
}
