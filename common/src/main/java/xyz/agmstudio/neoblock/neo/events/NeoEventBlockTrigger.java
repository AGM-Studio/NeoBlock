package xyz.agmstudio.neoblock.neo.events;

import java.util.regex.Pattern;

public interface NeoEventBlockTrigger {
    Pattern ON_BLOCK_PATTERN = Pattern.compile("^on-(?<count>\\d+)(st|nd|rd|th)?-block$");
    Pattern EVERY_BLOCK_PATTERN = Pattern.compile("^on-every-(?<count>\\d+)-block(s)?$");
    Pattern EVERY_BLOCK_OFFSET_PATTERN = Pattern.compile("^on-every-(?<count>\\d+)-block(s)?-offset-(?<offset>\\d+)$");

    boolean matches(int count);

    record Every(int n) implements NeoEventBlockTrigger {
        @Override public boolean matches(int count) {
            return count % n == 0;
        }
    }

    record EveryOffset(int n, int offset) implements NeoEventBlockTrigger {
        @Override public boolean matches(int count) {
            if (count < n) return false;
            return (count - offset) % n == 0;
        }
    }
}