package net.tapacywka.teyvatcartographia.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/** Configurable key bindings (Options → Controls → Key Binds → "Teyvat Cartographia"). */
public final class Keybinds {
    private Keybinds() {}

    public static final String CATEGORY = "key.categories.teyvatcartographia";

    /**
     * Defaults: M (map), N (minimap), = / - (minimap zoom), B (pin your position), Z (stop tracking) and V (pulse):
     * none of them is used by vanilla 1.21.1.
     */
    public static final KeyMapping OPEN_MAP = new KeyMapping("key.teyvatcartographia.open_map", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, CATEGORY);
    public static final KeyMapping TOGGLE_MINIMAP = new KeyMapping("key.teyvatcartographia.toggle_minimap", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, CATEGORY);
    public static final KeyMapping MINIMAP_ZOOM_IN = new KeyMapping("key.teyvatcartographia.minimap_zoom_in", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_EQUAL, CATEGORY);
    public static final KeyMapping MINIMAP_ZOOM_OUT = new KeyMapping("key.teyvatcartographia.minimap_zoom_out", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_MINUS, CATEGORY);
    public static final KeyMapping QUICK_PIN = new KeyMapping("key.teyvatcartographia.quick_pin", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY);
    public static final KeyMapping CLEAR_TARGET = new KeyMapping("key.teyvatcartographia.clear_target", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, CATEGORY);
    /** Pulse of the tracked marker in the world; V is not used by vanilla 1.21.1. */
    public static final KeyMapping PULSE_TARGET = new KeyMapping("key.teyvatcartographia.pulse_target", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY);

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_MAP);
        event.register(TOGGLE_MINIMAP);
        event.register(MINIMAP_ZOOM_IN);
        event.register(MINIMAP_ZOOM_OUT);
        event.register(QUICK_PIN);
        event.register(CLEAR_TARGET);
        event.register(PULSE_TARGET);
    }
}
