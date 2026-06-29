package me.koyere.antiafkplus.platform;

import java.lang.reflect.Method;

import org.bukkit.entity.Player;

/**
 * Reflective bridge to the Bukkit player input API ({@code Player#getCurrentInput()}
 * returning {@code org.bukkit.Input}), introduced in Minecraft 1.21.3.
 *
 * <p>The plugin compiles against an older API (Paper 1.20.1) and supports servers
 * from 1.16 upward, so the input API cannot be referenced directly. All access is
 * resolved reflectively at class-load time; on servers older than 1.21.3 the methods
 * simply do not resolve and the feature stays disabled ({@link #isSupported()} is
 * {@code false}).</p>
 *
 * <p>Purpose: tell genuine vehicle steering (a player actively pressing W/A/S/D, jump
 * or sneak while riding) apart from passive vehicle movement (e.g. a boat drifting on
 * a water current). Previously only head rotation distinguished the two, so a player
 * driving straight forward — holding W without moving the mouse — produced no head
 * rotation and was wrongly marked AFK.</p>
 */
public final class PlayerInputUtil {

    private static final boolean SUPPORTED;
    private static final Method GET_CURRENT_INPUT;
    private static final Method IS_FORWARD;
    private static final Method IS_BACKWARD;
    private static final Method IS_LEFT;
    private static final Method IS_RIGHT;
    private static final Method IS_JUMP;
    private static final Method IS_SNEAK;

    static {
        Method getCurrentInput = null;
        Method isForward = null;
        Method isBackward = null;
        Method isLeft = null;
        Method isRight = null;
        Method isJump = null;
        Method isSneak = null;
        boolean supported = false;
        try {
            getCurrentInput = Player.class.getMethod("getCurrentInput");
            Class<?> inputClass = Class.forName("org.bukkit.Input");
            isForward = inputClass.getMethod("isForward");
            isBackward = inputClass.getMethod("isBackward");
            isLeft = inputClass.getMethod("isLeft");
            isRight = inputClass.getMethod("isRight");
            isJump = inputClass.getMethod("isJump");
            isSneak = inputClass.getMethod("isSneak");
            supported = true;
        } catch (Throwable ignored) {
            // Server older than 1.21.3 (no input API). Feature remains disabled.
        }
        GET_CURRENT_INPUT = getCurrentInput;
        IS_FORWARD = isForward;
        IS_BACKWARD = isBackward;
        IS_LEFT = isLeft;
        IS_RIGHT = isRight;
        IS_JUMP = isJump;
        IS_SNEAK = isSneak;
        SUPPORTED = supported;
    }

    private PlayerInputUtil() {
    }

    /**
     * @return {@code true} when the running server exposes the 1.21.3+ input API.
     */
    public static boolean isSupported() {
        return SUPPORTED;
    }

    /**
     * Reports whether the player is actively holding a movement key this tick:
     * forward, backward, left, right, jump or sneak.
     *
     * @param player the player to inspect
     * @return {@code true} if a movement key is pressed; {@code false} when the input
     *         API is unavailable, the player is null, or no movement key is held
     */
    public static boolean hasActiveMovementInput(Player player) {
        if (!SUPPORTED || player == null) {
            return false;
        }
        try {
            Object input = GET_CURRENT_INPUT.invoke(player);
            if (input == null) {
                return false;
            }
            return isPressed(IS_FORWARD, input)
                    || isPressed(IS_BACKWARD, input)
                    || isPressed(IS_LEFT, input)
                    || isPressed(IS_RIGHT, input)
                    || isPressed(IS_JUMP, input)
                    || isPressed(IS_SNEAK, input);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isPressed(Method method, Object input) {
        if (method == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(method.invoke(input));
        } catch (Throwable ignored) {
            return false;
        }
    }
}
