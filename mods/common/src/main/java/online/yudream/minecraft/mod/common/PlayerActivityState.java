package online.yudream.minecraft.mod.common;

final class PlayerActivityState {

    private final long lastActiveAt;
    private final boolean afk;

    PlayerActivityState(long lastActiveAt, boolean afk) {
        this.lastActiveAt = lastActiveAt;
        this.afk = afk;
    }

    long getLastActiveAt() {
        return lastActiveAt;
    }

    boolean isAfk() {
        return afk;
    }
}
