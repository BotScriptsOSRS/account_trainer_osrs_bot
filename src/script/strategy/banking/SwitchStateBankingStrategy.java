package script.strategy.banking;

import org.osbot.rs07.api.Bank;
import org.osbot.rs07.api.map.Area;
import org.osbot.rs07.api.map.constants.Banks;
import org.osbot.rs07.api.model.Item;
import org.osbot.rs07.api.ui.Skill;
import org.osbot.rs07.script.MethodProvider;
import org.osbot.rs07.script.Script;
import script.MainScript;
import script.state.BotState;
import script.state.CraftingState;
import script.state.GrandExchangeState;
import script.state.MulingState;
import script.strategy.TaskStrategy;
import script.strategy.grand_exchange.BuyGrandExchangeStrategy;
import script.strategy.grand_exchange.SellGrandExchangeStrategy;
import script.strategy.muling.MulingStrategy;
import script.utils.*;

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.osbot.rs07.script.MethodProvider.random;

public class SwitchStateBankingStrategy implements TaskStrategy {

    private static final int ITEM_INTERACT_WAIT_MS = 5000;
    private static final int SLEEP_MIN_MS = 1000;
    private static final int SLEEP_MAX_MS = 1500;
    private final BotState returnState;
    private final Map<String, Integer> itemsToBuyInAdvance;
    private final Map<String, Integer> requiredBankItems;
    private final List<String> depositExceptions;

    private static final Area[] BANKS = {
            Banks.LUMBRIDGE_UPPER,
            Banks.VARROCK_WEST,
            Banks.VARROCK_EAST,
            Banks.FALADOR_EAST,
            Banks.FALADOR_WEST,
            Banks.EDGEVILLE,
            Banks.GRAND_EXCHANGE,
            Banks.AL_KHARID,
            Banks.DRAYNOR
    };

    public SwitchStateBankingStrategy(Map<Integer, Integer> requiredBankItems, Map<Integer, Integer> itemsToBuyInAdvance, BotState returnState, List<String> depositExceptions) {
        this.requiredBankItems = convertIdMapToNameMap(requiredBankItems);
        this.itemsToBuyInAdvance = convertIdMapToNameMap(itemsToBuyInAdvance);
        this.returnState = returnState;
        this.depositExceptions = depositExceptions;
    }

    public SwitchStateBankingStrategy(Map<Integer, Integer> requiredBankItems, Map<Integer, Integer> itemsToBuyInAdvance, BotState returnState) {
        this(requiredBankItems, itemsToBuyInAdvance, returnState, Collections.singletonList("null"));
    }

    private Map<String, Integer> convertIdMapToNameMap(Map<Integer, Integer> idMap) {
        return idMap.entrySet().stream()
                .filter(entry -> GameItem.getNameById(entry.getKey()) != null)
                .collect(Collectors.toMap(
                        entry -> GameItem.getNameById(entry.getKey()),
                        Map.Entry::getValue
                ));
    }

    @Override
    public void execute(Script script) throws InterruptedException {
        MainScript mainScript = (MainScript) script;
        if (!prepareForBanking(mainScript)) {
            mainScript.log("Failed to prepare for banking.");
            return;
        }

        if (checkAndHandleCoinConditions(mainScript)) {
            return;
        }
        if (!areAllItemsAvailable(mainScript)) {
            mainScript.log("Not all required items are available in the bank.");
            handleMissingItems(mainScript);
            return;
        }
        performBankingActions(mainScript);
    }

    private boolean checkAndHandleCoinConditions(MainScript script) {
        LocalTime currentTime = LocalTime.now();
        LocalTime startTime = LocalTime.of(21, 0);
        LocalTime endTime = LocalTime.of(21, 30);
        if (tooManyCoins(script) && currentTime.isAfter(startTime) && currentTime.isBefore(endTime)) {
            if (bankContainsSellableItems(script)) {
                script.log("Sellable items found in the bank, selling items on Grand Exchange before muling.");
                sellItemsOnGrandExchange(script);
                return true;
            }
            script.log("Too many coins, transition to muling state");
            muleCoins(script);
            return true;
        }
        if (tooLittleCoins(script)) {
            script.log("Too little coins, transition to Grand Exchange state to sell items");
            sellItemsOnGrandExchange(script);
            return true;
        }
        return false;
    }

    private boolean bankContainsSellableItems(MainScript script) {
        int[] sellableItemIds = new int[SellableItems.values().length];
        for (int i = 0; i < sellableItemIds.length; i++) {
            sellableItemIds[i] = SellableItems.values()[i].getId();
        }
        return script.getBank().contains(sellableItemIds);
    }

    private void muleCoins(MainScript script) {
        if (!Banks.GRAND_EXCHANGE.contains(script.myPlayer())) {
            depositInventoryAndEquipment(script);
        }
        TaskStrategy mulingStrategy = new MulingStrategy();
        MulingState mulingState = new MulingState(script, mulingStrategy, returnState);
        script.setCurrentState(mulingState);
    }

    private void sellItemsOnGrandExchange(MainScript script) {
        depositInventoryAndEquipment(script);
        TaskStrategy sellGrandExchangeStrategy = new SellGrandExchangeStrategy();
        GrandExchangeState grandExchangeState = new GrandExchangeState(script, sellGrandExchangeStrategy, returnState);
        script.setCurrentState(grandExchangeState);
    }

    private void performBankingActions(Script script) throws InterruptedException {
        depositInventoryAndEquipment(script);
        int craftingLevel = script.getSkills().getStatic(Skill.CRAFTING);
        if (returnState instanceof CraftingState && !Banks.EDGEVILLE.contains(script.myPlayer()) && craftingLevel >= 5){
            script.log("Walking to Edgeville bank to get crafting supplies"); // needed to not run from GE with gold bars etc
            script.getWalking().webWalk(Banks.EDGEVILLE);
        }
        withdrawRequiredItems(script);
        equipItemsIfNeeded(script);
        closeBank(script);
    }

    private boolean prepareForBanking(Script script) throws InterruptedException {
        if (!isAtBank(script)) {
            walkToNearestBank(script);
        }
        return BankingUtils.openBankWithRetry(script);
    }
    private boolean isAtBank(Script script) {
        return Arrays.stream(BANKS).anyMatch(bank -> bank.contains(script.myPosition()));
    }

    private boolean areAllItemsAvailable(Script script) throws InterruptedException {
        MethodProvider.sleep(random(SLEEP_MIN_MS, SLEEP_MAX_MS));
        for (Map.Entry<String, Integer> entry : requiredBankItems.entrySet()) {
            if (!isItemAvailable(script, entry.getKey(), entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    private boolean tooManyCoins(Script script){
        return getTotalItemAmount(script, GameItem.COINS.getName()) > 10000000;
    }

    private boolean tooLittleCoins(Script script){
        return getTotalItemAmount(script, GameItem.COINS.getName()) < 250000;
    }

    private boolean isItemAvailable(Script script, String itemName, int requiredQuantity) {
        int totalAmount = getTotalItemAmount(script, itemName);
        return totalAmount >= requiredQuantity || isItemAvailableInBank(script, itemName, requiredQuantity - totalAmount);
    }

    private int getTotalItemAmount(Script script, String itemName) {
        int amountBank = (int) script.getBank().getAmount(itemName);
        int amountInInventory = (int) script.getInventory().getAmount(itemName);
        int amountEquipped = (int) script.getEquipment().getAmount(itemName);
        return amountBank + amountInInventory + amountEquipped;
    }

    private boolean isItemAvailableInBank(Script script, String itemName, int requiredAmount) {
        Item bankItem = script.getBank().getItem(itemName);
        return bankItem != null && bankItem.getAmount() >= requiredAmount;
    }

    private void walkToNearestBank(Script script) {
        script.log("Walking to the nearest F2P bank");
        script.getWalking().webWalk(BANKS);
    }

    private void depositInventoryAndEquipment(Script script) {
        if (!script.getInventory().isEmpty() && script.getBank().depositAllExcept(depositExceptions.toArray(new String[0]))) {
            script.log("Depositing inventory");
            Sleep.sleepUntil(()-> script.getInventory().isEmptyExcept(depositExceptions.toArray(new String[0])), ITEM_INTERACT_WAIT_MS);
        }
        if (!script.getEquipment().isEmpty() && script.getBank().depositWornItems()) {
            script.log("Depositing equipment");
            Sleep.sleepUntil(()-> script.getEquipment().isEmpty(), ITEM_INTERACT_WAIT_MS);
        }
    }

    private void withdrawRequiredItems(Script script) throws InterruptedException {
        if (!script.getBank().isOpen() && !BankingUtils.openBankWithRetry(script)) {
            return;
        }
        if (script.getBank().isBankModeEnabled(Bank.BankMode.WITHDRAW_NOTE) && script.getBank().enableMode(Bank.BankMode.WITHDRAW_ITEM)){
            Sleep.sleepUntil(()-> script.getBank().isBankModeEnabled(Bank.BankMode.WITHDRAW_ITEM), ITEM_INTERACT_WAIT_MS);
        }
        for (Map.Entry<String, Integer> entry : requiredBankItems.entrySet()) {
            withdrawSingleItem(script, entry.getKey(), entry.getValue());
        }
    }

    private void withdrawSingleItem(Script script, String itemName, int quantity) {
        int amountToWithdraw = quantity - (int) script.getInventory().getAmount(itemName);
        if (amountToWithdraw > 0 && script.getBank().contains(itemName)) {
            script.log("Withdrawing " + amountToWithdraw + " of " + itemName);
            if (script.getBank().withdraw(itemName, amountToWithdraw)) {
                waitForItemInInventory(script, itemName);
            }
        }
    }

    private void waitForItemInInventory(Script script, String itemName) {
        Sleep.sleepUntil(()-> script.getInventory().contains(itemName), ITEM_INTERACT_WAIT_MS);
    }

    private void closeBank(Script script) {
        script.log("Closing the bank");
        script.getBank().close();
        waitForBankToClose(script);
    }

    private void waitForBankToClose(Script script) {
        Sleep.sleepUntil(()-> !script.getBank().isOpen(), ITEM_INTERACT_WAIT_MS);
    }

    private void handleMissingItems(Script script) {
        Map<String, Integer> itemsToPurchase = calculateItemsToPurchase(script);

        if (!itemsToPurchase.isEmpty()) {
            proceedToPurchaseItems(script, itemsToPurchase);
        }
    }

    private Map<String, Integer> calculateItemsToPurchase(Script script) {
        Map<String, Integer> itemsToPurchase = new HashMap<>();

        for (Map.Entry<String, Integer> entry : itemsToBuyInAdvance.entrySet()) {
            String itemName = entry.getKey();
            int requiredQuantity = entry.getValue();
            int totalAmount = getTotalAmountAvailable(script, itemName);

            if (totalAmount < requiredQuantity) {
                itemsToPurchase.put(itemName, requiredQuantity - totalAmount);
            }
        }

        return itemsToPurchase;
    }

    private int getTotalAmountAvailable(Script script, String itemName) {
        int amountInInventory = (int) script.getInventory().getAmount(itemName);
        int amountEquipped = (int) script.getEquipment().getAmount(itemName);
        Item bankItem = script.getBank().getItem(itemName);
        int amountInBank = bankItem != null ? bankItem.getAmount() : 0;

        return amountInInventory + amountEquipped + amountInBank;
    }

    private void proceedToPurchaseItems(Script script, Map<String, Integer> itemsToPurchase) {
        if (!Banks.GRAND_EXCHANGE.contains(script.myPlayer())) {
            depositInventoryAndEquipment(script);
        } else if (!script.getInventory().isEmptyExcept(GameItem.COINS.getId()) && script.getBank().depositAllExcept(GameItem.COINS.getId())){
                script.log("Depositing inventory");
                Sleep.sleepUntil(()-> script.getInventory().isEmptyExcept(GameItem.COINS.getId()), ITEM_INTERACT_WAIT_MS);
        }
        script.log("Transitioning to Grand Exchange State to buy missing items.");
        TaskStrategy grandExchangeStrategy = new BuyGrandExchangeStrategy(itemsToPurchase);
        ((MainScript) script).setCurrentState(new GrandExchangeState((MainScript) script, grandExchangeStrategy, returnState));
    }

    private void equipItemsIfNeeded(Script script) {
        for (String itemName : requiredBankItems.keySet()) {
            equipItem(script, itemName);
        }
    }

    private void equipItem(Script script, String itemName) {
        int itemId = GameItem.getIdByName(itemName);
        if (itemId != -1 && script.getInventory().contains(itemId)) {
            Item item = script.getInventory().getItem(itemId);
            if (item.hasAction("Wield") && isRequiredLevelMet(script, itemName)) {
                script.log("Equipping " + itemName);
                item.interact("Wield");
                waitForItemEquipped(script, itemId);
            }
        }
    }

    private boolean isRequiredLevelMet(Script script, String itemName) {
        int requiredLevel = getRequiredLevelForItem(itemName);
        return script.getSkills().getDynamic(Skill.ATTACK) >= requiredLevel;
    }

    private int getRequiredLevelForItem(String itemName) {
        itemName = itemName.toLowerCase();
        if (itemName.contains("rune")) return 40;
        if (itemName.contains("adamant")) return 30;
        if (itemName.contains("mithril")) return 20;
        if (itemName.contains("black")) return 10;
        if (itemName.contains("steel")) return 5;
        return 0; // Default level if no specific type is found
    }

    private void waitForItemEquipped(Script script, int itemId) {
        Sleep.sleepUntil(()-> script.getEquipment().contains(itemId), ITEM_INTERACT_WAIT_MS);
    }
}
