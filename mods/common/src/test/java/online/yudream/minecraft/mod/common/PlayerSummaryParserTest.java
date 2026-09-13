package online.yudream.minecraft.mod.common;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PlayerSummaryParserTest {

    @Test
    public void parsesWrappedPlayerSummary() {
        String json = "{\"code\":0,\"data\":["
                + "{\"playerId\":\"a\",\"online\":true,\"afk\":false},"
                + "{\"playerId\":\"b\",\"online\":true,\"afk\":true},"
                + "{\"playerId\":\"c\",\"online\":false,\"afk\":false}"
                + "]}";

        PlayerSummaryParser.Summary summary = PlayerSummaryParser.parse(json);

        assertEquals(3, summary.getTotal());
        assertEquals(2, summary.getOnline());
        assertEquals(1, summary.getAfk());
    }
}
