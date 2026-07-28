package com.humanclicker;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/** Tus atamalari. Oyun icinde Ayarlar -> Kontroller'den degistirilebilir. */
public final class HumanClickerKeys {

    private HumanClickerKeys() {}

    public static final String CATEGORY = "key.categories.humanclicker";

    public static KeyBinding toggle;

    public static void register() {
        toggle = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.humanclicker.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                CATEGORY));
    }
}
