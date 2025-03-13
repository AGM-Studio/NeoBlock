package xyz.agmstudio.neoblock.util;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import xyz.agmstudio.neoblock.NeoBlockMod;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class ScoreboardUtil {
    private final Objective OBJECTIVE;
    private final Scoreboard SCOREBOARD;
    private final String STORAGE_PLAYER;

    public static <T extends ScoreboardUtil> T of(LevelAccessor accessor, Class<T> clazz) {
        if (accessor instanceof Level level) {
            try {
                Constructor<T> constructor = clazz.getConstructor(Scoreboard.class);
                constructor.setAccessible(true);
                return constructor.newInstance(level.getScoreboard());
            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                NeoBlockMod.LOGGER.error("Could not find scoreboard for {}: {}", clazz.getSimpleName(), e.getMessage());
            }
        }
        return null;
    }
    public static ScoreboardUtil of(LevelAccessor accessor, String name) {
        if (accessor instanceof Level level) return new ScoreboardUtil(level.getScoreboard(), name);
        return null;
    }
    public static int get(LevelAccessor level, String name, String key) {
        ScoreboardUtil board = ScoreboardUtil.of(level, name);
        return board != null ? board.getScore(board.getStoragePlayer(key)) : 0;
    }
    public static int get(LevelAccessor level, String name, ScoreHolder holder) {
        ScoreboardUtil board = ScoreboardUtil.of(level, name);
        return board != null ? board.getScore(holder) : 0;
    }
    public static void set(LevelAccessor level, String name, String key, int value) {
        ScoreboardUtil board = ScoreboardUtil.of(level, name);
        if (board != null) board.setScore(board.getStoragePlayer(key), value);
    }
    public static void set(LevelAccessor level, String name, ScoreHolder holder, int value) {
        ScoreboardUtil board = ScoreboardUtil.of(level, name);
        if (board != null) board.setScore(holder, value);
    }

    private static Objective getOrCreateObjective(Scoreboard scoreboard, String name) {
        Objective objective = scoreboard.getObjective(name);
        if (objective == null) return scoreboard.addObjective(name, ObjectiveCriteria.DUMMY, Component.literal(name), ObjectiveCriteria.RenderType.INTEGER, false, null);
        return objective;
    }

    protected ScoreHolder getStoragePlayer(String key) {
        return ScoreHolder.forNameOnly(String.format(STORAGE_PLAYER, key));
    }

    public ScoreboardUtil(Scoreboard scoreboard, String name) {
        SCOREBOARD = scoreboard;
        OBJECTIVE = getOrCreateObjective(scoreboard, name);
        STORAGE_PLAYER = "{}#NeoData";
    }

    public void setScore(ScoreHolder holder, int value) {
        SCOREBOARD.getOrCreatePlayerScore(holder, OBJECTIVE).set(value);
    }
    public void setScore(String key, int value) {
        setScore(getStoragePlayer(key), value);
    }

    public int getScore(ScoreHolder holder) {
        return SCOREBOARD.getOrCreatePlayerScore(holder, OBJECTIVE).get();
    }
    public int getScore(String key) {
        return getScore(getStoragePlayer(key));
    }
}