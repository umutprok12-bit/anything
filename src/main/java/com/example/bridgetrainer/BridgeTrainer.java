package com.example.bridgetrainer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

/**
 * Cha-cha bridging practice HUD for Forge 1.8.9.
 * Display only: it reads your state and draws cues. It never sends input or moves the camera.
 */
@Mod(modid = "bridgetrainer", name = "Bridge Trainer", version = "1.0",
        clientSideOnly = true, acceptedMinecraftVersions = "[1.8.9]")
public class BridgeTrainer {

    // ---- Tweak these ----
    private static final float PITCH_MIN = 75.0f;     // good pitch range for side-face aim
    private static final float PITCH_MAX = 82.0f;
    private static final long  BEAT_MS   = 180;       // time per A/D step
    private static final float HOLD_FRAC = 0.70f;     // how much of each beat the key stays lit
    // ---------------------

    private static final int GREEN = 0xFF55FF55;
    private static final int RED   = 0xFFFF5555;
    private static final int CUE   = 0xFF3FA9F5;
    private static final int OFF   = 0x88000000;
    private static final int WHITE = 0xFFFFFFFF;

    private final Minecraft mc = Minecraft.getMinecraft();
    private KeyBinding toggleKey;
    private boolean enabled = false;
    private long startTime = 0;

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        toggleKey = new KeyBinding("Toggle Bridge Trainer", Keyboard.KEY_J, "Bridge Trainer");
        ClientRegistry.registerKeyBinding(toggleKey);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent e) {
        if (toggleKey.isPressed()) {
            enabled = !enabled;
            startTime = System.currentTimeMillis();
        }
    }

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post e) {
        if (!enabled || e.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (mc.thePlayer == null || mc.currentScreen != null) return;

        int w = e.resolution.getScaledWidth();
        int h = e.resolution.getScaledHeight();

        drawCrosshair(w / 2, h / 2);
        drawKeyPanel(w - 100, h - 80);
    }

    private boolean aimGood() {
        float pitch = mc.thePlayer.rotationPitch;
        boolean pitchOk = pitch >= PITCH_MIN && pitch <= PITCH_MAX;

        MovingObjectPosition mop = mc.objectMouseOver;
        boolean sideFace = mop != null
                && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                && mop.sideHit != null
                && mop.sideHit.getAxis().isHorizontal();

        return pitchOk && sideFace;
    }

    private void drawCrosshair(int cx, int cy) {
        int color = aimGood() ? GREEN : RED;
        Gui.drawRect(cx - 6, cy - 1, cx + 7, cy + 1, color);   // horizontal bar
        Gui.drawRect(cx - 1, cy - 6, cx + 1, cy + 7, color);   // vertical bar
    }

    private void drawKeyPanel(int x, int y) {
        long elapsed = System.currentTimeMillis() - startTime;
        long step = elapsed / BEAT_MS;
        long inBeat = elapsed % BEAT_MS;
        boolean lit = inBeat < BEAT_MS * HOLD_FRAC;
        boolean leftTurn = (step % 2 == 0);

        boolean cueA = lit && leftTurn;
        boolean cueD = lit && !leftTurn;

        // Layout: W on top row (unused for cha-cha, shown dim), A S D middle, Shift bottom
        box(x + 24, y,      "W",     false,  mc.gameSettings.keyBindForward.isKeyDown());
        box(x,      y + 24, "A",     cueA,   mc.gameSettings.keyBindLeft.isKeyDown());
        box(x + 24, y + 24, "S",     true,   mc.gameSettings.keyBindBack.isKeyDown());
        box(x + 48, y + 24, "D",     cueD,   mc.gameSettings.keyBindRight.isKeyDown());
        box(x,      y + 48, "SHIFT", true,   mc.gameSettings.keyBindSneak.isKeyDown(), 72);

        FontRenderer fr = mc.fontRendererObj;
        fr.drawStringWithShadow("blue = press/hold   white = you", x, y - 12, WHITE);
    }

    private void box(int x, int y, String label, boolean cue, boolean pressed) {
        box(x, y, label, cue, pressed, 22);
    }

    private void box(int x, int y, String label, boolean cue, boolean pressed, int width) {
        int height = 22;
        Gui.drawRect(x, y, x + width, y + height, cue ? CUE : OFF);
        if (pressed) {
            // white outline = key you are actually holding
            Gui.drawRect(x, y, x + width, y + 1, WHITE);
            Gui.drawRect(x, y + height - 1, x + width, y + height, WHITE);
            Gui.drawRect(x, y, x + 1, y + height, WHITE);
            Gui.drawRect(x + width - 1, y, x + width, y + height, WHITE);
        }
        FontRenderer fr = mc.fontRendererObj;
        int tx = x + (width - fr.getStringWidth(label)) / 2;
        fr.drawStringWithShadow(label, tx, y + 7, WHITE);
    }
}
