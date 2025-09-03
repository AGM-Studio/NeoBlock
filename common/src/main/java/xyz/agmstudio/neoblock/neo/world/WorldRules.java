package xyz.agmstudio.neoblock.neo.world;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import xyz.agmstudio.neoblock.NeoBlock;
import xyz.agmstudio.neoblock.platform.IConfig;

import java.lang.reflect.Field;
import java.util.Map;

public class WorldRules {
    public static void applyGameRules(ServerLevel level, IConfig config) {
        for (Map.Entry<String, Object> entry: config.valueMap().entrySet())
            applyGameRule(level, entry.getKey(), entry.getValue());
    }

    public static boolean applyGameRule(ServerLevel level, String key, Object value) {
        GameRules.Key<?> rule = getGameRuleByName(key);
        if (rule == null) return false;

        return setGameRule(level, rule, value.toString());
    }

    private static GameRules.Key<?> getGameRuleByName(String name) {
        for (Field field: GameRules.class.getDeclaredFields()) {
            if ((field.getModifiers() & 0x00000008) != 0 && GameRules.Key.class.isAssignableFrom(field.getType())) try {
                GameRules.Key<?> key = (GameRules.Key<?>) field.get(null);
                if (key.getId().equals(name)) return key;
            } catch (IllegalAccessException e) {
                NeoBlock.LOGGER.warn("Unable to find game rule \"{}\" due to {}", name, e);
            }
        }

        return null;
    }

    private static <T extends GameRules.Value<T>> boolean setGameRule(ServerLevel level, GameRules.Key<T> key, String value) {
        GameRules gameRules = level.getGameRules();
        T rule = gameRules.getRule(key);

        NeoBlock.LOGGER.info("Setting game rule \"{}\" to \"{}\"", key.getId(), value);
        if (rule instanceof GameRules.BooleanValue bool) {
            bool.set(Boolean.parseBoolean(value), level.getServer());
            return true;
        }
        if (rule instanceof GameRules.IntegerValue integer) {
            try {
                integer.set(Integer.parseInt(value), level.getServer());
                return true;
            } catch (NumberFormatException e) {
                NeoBlock.LOGGER.warn("Invalid number for GameRule: {}", key.getId());
                return false;
            }
        }
        NeoBlock.LOGGER.warn("Unknown game rule {}({}) type...", key, rule.getClass().getName());
        return false;
    }
}
