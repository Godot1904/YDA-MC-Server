package online.yudream.minecraft.mod.common;

import java.util.Objects;
import java.util.UUID;

public final class PlayerIdentity {

    private final UUID uuid;
    private final String name;

    public PlayerIdentity(UUID uuid, String name) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.name = name == null || name.trim().isEmpty() ? uuid.toString() : name.trim();
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getId() {
        return uuid.toString();
    }

    public String getName() {
        return name;
    }

    public PlayerEventPayload payload(long eventAt) {
        return new PlayerEventPayload(getId(), name, eventAt);
    }
}
