package script.utils;

import org.osbot.rs07.script.Script;

public class BankingUtils {

    private static final int SLEEP_DURATION_MS = 5000; // Adjust this as needed

    public static boolean openBankWithRetry(Script script) throws InterruptedException {
        int MAX_ATTEMPTS = 3;
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            if (openBank(script)) {
                return true;
            }
            script.log("Attempt to open bank failed, retrying...");
            Sleep.sleepUntil(() -> script.getBank().isOpen(), SLEEP_DURATION_MS);
        }
        script.log("Failed to open bank after multiple attempts");
        return false;
    }

    private static boolean openBank(Script script) throws InterruptedException {
        if (script.getBank().isOpen()) {
            return true;
        }
        return attemptToOpenBank(script);
    }

    private static boolean attemptToOpenBank(Script script) throws InterruptedException {
        if (script.getBank().open()) {
            Sleep.sleepUntil(() -> script.getBank().isOpen(), 10000);
            return true;
        }
        script.log("Failed to open the bank");
        return false;
    }
}
