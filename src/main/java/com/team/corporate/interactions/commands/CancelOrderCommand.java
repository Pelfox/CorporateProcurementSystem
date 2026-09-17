package com.team.corporate.interactions.commands;

public class CancelOrderCommand implements CommandInterface {
    @Override
    public void execute() {
        System.out.println("CANCEL_ORDER");
    }
}
