package xyz.agmstudio.neoblock.tiers;

public enum WorldState {
    INACTIVE(0),    // Default, before activation
    ACTIVE(1),      // NeoBlock is running
    DISABLED(2),     // NeoBlock is disabled
    UPDATED(3);     // NeoBlock configs has been updated

    private final int id;

    WorldState(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static WorldState fromId(int id) {
        for (WorldState state : values()) {
            if (state.id == id) return state;
        }
        return INACTIVE;
    }
}