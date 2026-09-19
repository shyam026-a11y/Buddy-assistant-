package com.shyam026.buddyassistant;

public final class BuddySecurity {
    public enum Risk {
        SAFE,
        PERSONAL_DATA,
        SENSITIVE
    }

    private BuddySecurity() {}

    public static Risk riskFor(String action) {
        if ("send_sms".equals(action) || "call".equals(action) || "delete".equals(action)) {
            return Risk.SENSITIVE;
        }
        if ("read_notifications".equals(action) || "contacts".equals(action)
                || "calendar".equals(action)) {
            return Risk.PERSONAL_DATA;
        }
        return Risk.SAFE;
    }

    public static boolean requiresExplicitUserConfirmation(String action) {
        return riskFor(action) == Risk.SENSITIVE;
    }
}
