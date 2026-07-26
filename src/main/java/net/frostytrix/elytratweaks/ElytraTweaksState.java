package net.frostytrix.elytratweaks;

public class ElytraTweaksState {
    // Set to true only while the mod's own deploy key is starting fall flight,
    // so PlayerEntityMixin can tell an intentional deploy from every other
    // (vanilla spacebar / other mod) attempt and block the latter on the client.
    public static boolean isCustomPress = false;
}
