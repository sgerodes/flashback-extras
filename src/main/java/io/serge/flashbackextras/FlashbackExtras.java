package io.serge.flashbackextras;

import com.mojang.blaze3d.platform.InputConstants;
import io.serge.flashbackextras.config.FlashbackExtrasConfig;
import io.serge.flashbackextras.export.ExportCapabilityWarmup;
import io.serge.flashbackextras.screen.FlashbackExtrasConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class FlashbackExtras implements ClientModInitializer {
    public static final String MOD_ID = "flashbackextras";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final String KEYBIND_CATEGORY = "flashbackextras.keybind";

    private static final KeyMapping OPEN_MENU_KEYBIND = KeyBindingHelper.registerKeyBinding(createOpenMenuKeybind());

    @Override
    public void onInitializeClient() {
        FlashbackExtrasConfig.load();
        ExportCapabilityWarmup.initialize();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_MENU_KEYBIND.consumeClick()) {
                long window = resolveWindowHandle(client.getWindow());
                boolean rightAltDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS;

                if (rightAltDown && client.screen == null) {
                    client.setScreen(new FlashbackExtrasConfigScreen(null));
                }
            }
        });
    }

    private static KeyMapping createOpenMenuKeybind() {
        try {
            for (Constructor<?> constructor : KeyMapping.class.getConstructors()) {
                Class<?>[] params = constructor.getParameterTypes();
                if (params.length != 4 || params[0] != String.class || params[1] != InputConstants.Type.class || params[2] != int.class) {
                    continue;
                }

                if (params[3] == String.class) {
                    return (KeyMapping) constructor.newInstance(
                        "flashbackextras.keybind.open_menu",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_P,
                        KEYBIND_CATEGORY
                    );
                }

                Object category = createCategoryObject(params[3], ResourceLocation.fromNamespaceAndPath(MOD_ID, "keybind"));
                return (KeyMapping) constructor.newInstance(
                    "flashbackextras.keybind.open_menu",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_P,
                    category
                );
            }

            throw new NoSuchMethodException("No compatible KeyMapping constructor found");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to create Flashback Extras keybind", e);
        }
    }

    private static Object createCategoryObject(Class<?> categoryClass, ResourceLocation categoryId) throws ReflectiveOperationException {
        for (Method method : categoryClass.getDeclaredMethods()) {
            if (!Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 1 || method.getReturnType() != categoryClass) {
                continue;
            }

            Class<?> param = method.getParameterTypes()[0];
            if (param != ResourceLocation.class && !param.isAssignableFrom(ResourceLocation.class)) {
                continue;
            }

            method.setAccessible(true);
            return method.invoke(null, categoryId);
        }

        for (Field field : categoryClass.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || field.getType() != categoryClass) {
                continue;
            }
            field.setAccessible(true);
            Object existing = field.get(null);
            if (existing != null) {
                return existing;
            }
        }

        throw new NoSuchMethodException("No compatible category registration path found");
    }

    private static long resolveWindowHandle(Object window) {
        try {
            Method handle = window.getClass().getMethod("handle");
            return ((Number) handle.invoke(window)).longValue();
        } catch (ReflectiveOperationException ignored) {
            try {
                Method getWindow = window.getClass().getMethod("getWindow");
                return ((Number) getWindow.invoke(window)).longValue();
            } catch (ReflectiveOperationException e) {
                for (Method method : window.getClass().getMethods()) {
                    if (method.getParameterCount() == 0 && (method.getReturnType() == long.class || method.getReturnType() == Long.class)) {
                        try {
                            Object value = method.invoke(window);
                            if (value instanceof Number number) {
                                long handleValue = number.longValue();
                                if (handleValue != 0L) {
                                    return handleValue;
                                }
                            }
                        } catch (ReflectiveOperationException ignoredAgain) {
                        }
                    }
                }
                throw new RuntimeException("Failed to resolve Minecraft window handle", e);
            }
        }
    }
}
