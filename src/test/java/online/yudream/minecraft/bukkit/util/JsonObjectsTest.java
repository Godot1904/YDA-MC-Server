package online.yudream.minecraft.bukkit.util;

import online.yudream.minecraft.bukkit.report.PlayerEventPayload;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

public class JsonObjectsTest {

    @Test
    public void playerEventEscapesJsonFields() {
        PlayerEventPayload payload = new PlayerEventPayload("id-1", "A\"B\\C", 123L);

        assertEquals("{\"playerId\":\"id-1\",\"playerName\":\"A\\\"B\\\\C\",\"eventAt\":123}",
                JsonObjects.playerEvent(payload));
    }

    @Test
    public void serializesPlayerSnapshot() {
        assertEquals("{\"observedAt\":456,\"players\":[{\"playerId\":\"id-1\",\"playerName\":\"A\"},{\"playerId\":\"id-2\",\"playerName\":\"B\"}]}",
                JsonObjects.playerSnapshot(Arrays.asList(
                        new PlayerEventPayload("id-1", "A", 1L),
                        new PlayerEventPayload("id-2", "B", 2L)), 456L));
    }
}
