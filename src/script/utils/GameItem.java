package script.utils;

public enum GameItem {
    SMALL_FISHING_NET(303, "Small fishing net"),
    FLY_FISHING_ROD(309, "Fly fishing rod"),
    FEATHER(314, "Feather"),
    LOBSTER_POT(301, "Lobster pot"),
    COINS(995, "Coins"),
    BRONZE_AXE(1351, "Bronze axe"),
    STEEL_AXE(1353, "Steel axe"),
    BLACK_AXE(1361, "Black axe"),
    MITHRIL_AXE(1355, "Mithril axe"),
    ADAMANT_AXE(1357, "Adamant axe"),
    RUNE_AXE(1359, "Rune axe");

    private final int id;
    private final String name;

    GameItem(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public static String getNameById(int itemId) {
        for (GameItem item : values()) {
            if (item.getId() == itemId) {
                return item.getName();
            }
        }
        return null; // Or consider throwing an exception
    }

    public static int getIdByName(String itemName) {
        for (GameItem item : values()) {
            if (item.getName().equalsIgnoreCase(itemName)) {
                return item.getId();
            }
        }
        return -1; // Or consider throwing an exception
    }
}

