package online.yudream.minecraft.mod.common;

public final class PlayerEventPayload {

    private final String playerId;
    private final String playerName;
    private final long eventAt;

    public PlayerEventPayload(String playerId, String playerName, long eventAt) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.eventAt = eventAt;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public long getEventAt() {
        return eventAt;
    }
}
