package online.yudream.minecraft.mod.common;

public final class PlayerSummaryParser {

    private PlayerSummaryParser() {
    }

    public static Summary parse(String json) {
        if (json == null || json.isEmpty()) {
            return new Summary(0, 0, 0);
        }
        return new Summary(
                count(json, "\"playerId\""),
                count(json, "\"online\":true"),
                count(json, "\"afk\":true")
        );
    }

    private static int count(String text, String needle) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }

    public static final class Summary {
        private final int total;
        private final int online;
        private final int afk;

        private Summary(int total, int online, int afk) {
            this.total = total;
            this.online = online;
            this.afk = afk;
        }

        public int getTotal() {
            return total;
        }

        public int getOnline() {
            return online;
        }

        public int getAfk() {
            return afk;
        }
    }
}
