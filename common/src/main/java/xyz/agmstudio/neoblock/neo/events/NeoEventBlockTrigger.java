package xyz.agmstudio.neoblock.neo.events;

public interface NeoEventBlockTrigger {
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
