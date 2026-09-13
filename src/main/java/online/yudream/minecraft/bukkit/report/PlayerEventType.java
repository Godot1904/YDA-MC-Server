package online.yudream.minecraft.bukkit.report;

public enum PlayerEventType {
    JOIN("join"),
    QUIT("quit"),
    AFK_START("afk/start"),
    AFK_END("afk/end");

    private final String remotePath;

    PlayerEventType(String remotePath) {
        this.remotePath = remotePath;
    }

    public String getRemotePath() {
        return remotePath;
    }
}
