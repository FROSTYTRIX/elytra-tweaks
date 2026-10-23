package net.frostytrix.elytratweaks;

import java.util.Locale;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.TranslatableEnum;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.EnumValue<DeployKeyMode> DEPLOY_KEY_MODE = BUILDER
            .comment("How the deploy key behaves.",
                    "TOGGLE: press to open the elytra, press again to close it.",
                    "HOLD: the elytra stays open only while the key is held.")
            .defineEnum("deployKeyMode", DeployKeyMode.TOGGLE);

    public static final ModConfigSpec.EnumValue<JumpDeploy> JUMP_DEPLOY = BUILDER
            .comment("Whether the vanilla jump key can still open the elytra while falling.",
                    "BLOCKED: never, only the deploy key opens it.",
                    "WITH_FIREWORK: only while holding a firework rocket in either hand.",
                    "VANILLA: always, like vanilla.")
            .defineEnum("jumpDeploy", JumpDeploy.BLOCKED);

    public static final ModConfigSpec.BooleanValue SAFETY_NET = BUILDER
            .comment("Fold the wings when the elytra opens in a way the jump setting doesn't allow,",
                    "for example through another mod. Some modpacks show a one-frame flicker when this",
                    "kicks in; turning it off removes the flicker, but those deploys are then no longer stopped.")
            .define("safetyNet", true);

    public static final ModConfigSpec.IntValue WIND_VOLUME = BUILDER
            .comment("Volume of the wind sound while gliding, in percent (0 = muted, 100 = vanilla).")
            .defineInRange("windVolume", 100, 0, 100);

    public static final ModConfigSpec SPEC = BUILDER.build();

    public enum DeployKeyMode implements TranslatableEnum {
        TOGGLE,
        HOLD;

        @Override
        public Component getTranslatedName() {
            return Component.translatable(ElytraTweaksMod.MOD_ID + ".configuration.deployKeyMode." + name().toLowerCase(Locale.ROOT));
        }
    }

    public enum JumpDeploy implements TranslatableEnum {
        BLOCKED,
        WITH_FIREWORK,
        VANILLA;

        @Override
        public Component getTranslatedName() {
            return Component.translatable(ElytraTweaksMod.MOD_ID + ".configuration.jumpDeploy." + name().toLowerCase(Locale.ROOT));
        }
    }
}
