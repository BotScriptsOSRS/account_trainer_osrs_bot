//package script.state;
//
//import script.MainScript;
//import script.strategy.TaskStrategy;
//
//public class GrandExchangeState implements BotState {
//    private TaskStrategy strategy;
//
//    public GrandExchangeState(TaskStrategy strategy) {
//        this.strategy = strategy;
//    }
//
//    @Override
//    public void execute(MainScript script) throws InterruptedException {
//        strategy.execute(script);
//    }
//
//    @Override
//    public BotState nextState(MainScript script) {
//        // Logic to determine the next state after completing Grand Exchange operations
//    }
//}
