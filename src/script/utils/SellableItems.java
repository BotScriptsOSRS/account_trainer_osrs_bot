package script.utils;

public enum SellableItems {
    LOGS(1511, "Logs"),
    OAK_LOGS(1521, "Oak logs"),
    YEW_LOGS(1515, "Yew logs"),
    RAW_SHRIMPS(317, "Raw shrimps"),
    RAW_ANCHOVIES(321, "Raw anchovies"),
    RAW_TROUT(335, "Raw trout"),
    RAW_SALMON(331, "Raw salmon"),
    RAW_LOBSTER(377, "Raw lobster");

    private final int id;
    private final String name;

    SellableItems(int id, String name) {
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
        for (SellableItems item : values()) {
            if (item.getId() == itemId) {
                return item.getName();
            }
        }
        return null; // Or consider throwing an exception
    }

    public static int getIdByName(String itemName) {
        for (SellableItems item : values()) {
            if (item.getName().equalsIgnoreCase(itemName)) {
                return item.getId();
            }
        }
        return -1; // Or consider throwing an exception
    }
}

