package net.prospect.otherworlds.bending.progression;

/** The title shown for an element level. Gives no bonuses. */
public enum ElementTitle {
    NOVICE(1, "Novice"),
    APPRENTICE(5, "Apprentice"),
    INTERMEDIATE(10, "Intermediate"),
    COMPETENT(15, "Competent"),
    ADEPT(20, "Adept"),
    JOURNEYMAN(25, "Journeyman"),
    MASTER(45, "Master");

    private final int minLevel;
    private final String displayName;

    ElementTitle(int minLevel, String displayName) {
        this.minLevel = minLevel;
        this.displayName = displayName;
    }

    public int getMinLevel() {
        return minLevel;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ElementTitle forLevel(int level) {
        ElementTitle result = NOVICE;
        for (ElementTitle title : values()) {
            if (level >= title.minLevel) result = title;
        }
        return result;
    }
}