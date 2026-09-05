package com.team.corporate;

import com.team.corporate.interactions.ConsoleInteraction;
import com.team.corporate.interactions.InteractionAction;
import com.team.corporate.interactions.commands.*;

public class Main {
    public static void main(String[] args) {
        var interaction = new ConsoleInteraction();

        interaction.registerCommand(InteractionAction.CREATE_NEW_ORDER, new CreateOrderCommand());
        interaction.registerCommand(InteractionAction.ORDERS_HISTORY, new ShowOrderHistoryCommand());
        interaction.registerCommand(InteractionAction.CANCEL_ORDER, new CancelOrderCommand());
        interaction.registerCommand(InteractionAction.EDIT_ORDER, new EditOrderCommand());
        interaction.registerCommand(InteractionAction.WAREHOUSE_EDIT, new EditWarehouseCommand());

        interaction.run();
    }
}
