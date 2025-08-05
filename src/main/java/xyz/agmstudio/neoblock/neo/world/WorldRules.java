package xyz.agmstudio.neoblock.neo.world;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import xyz.agmstudio.neoblock.NeoBlockMod;

import java.lang.reflect.Field;

public class WorldRules {
    public static void applyGameRules(ServerLevel level, UnmodifiableConfig config) {
        for (String key : config.valueMap().keySet()) {
            GameRules.Key<?> rule = getGameRuleByName(key);
            if (rule == null) return;

            String value = config.get(key).toString();
            setGameRule(level, rule, value);
        }
    }

    private static GameRules.Key<?> getGameRuleByName(String name) {
        for (Field field : GameRules.class.getDeclaredFields()) {
            if ((field.getModifiers() & 0x00000008) != 0 && GameRules.Key.class.isAssignableFrom(field.getType())) try {
                GameRules.Key<?> key = (GameRules.Key<?>) field.get(null);
                if (key.getId().equals(name)) return key;
            } catch (IllegalAccessException e) {
                NeoBlockMod.LOGGER.warn("Unable to find game rule \"{}\" due to {}", name, e);
            }
        }

        return null;
    }

    private static <T extends GameRules.Value<T>> void setGameRule(ServerLevel level, GameRules.Key<T> key, String value) {
        GameRules gameRules = level.getGameRules();
        T rule = gameRules.getRule(key);

        NeoBlockMod.LOGGER.info("Setting game rule \"{}\" to \"{}\"", key.getId(), value);
        if (rule instanceof GameRules.BooleanValue bool)
            bool.set(Boolean.parseBoolean(value), level.getServer());
        else if (rule instanceof GameRules.IntegerValue integer) {
            try {
                integer.set(Integer.parseInt(value), level.getServer());
            } catch (NumberFormatException e) {
                NeoBlockMod.LOGGER.warn("Invalid number for GameRule: {}", key.getId());
            }
        }
    }
}
