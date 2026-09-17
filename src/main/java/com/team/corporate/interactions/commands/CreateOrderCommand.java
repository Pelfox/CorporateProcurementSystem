package com.team.corporate.interactions.commands;

public class CreateOrderCommand implements CommandInterface {
    @Override
    public void execute() {
        System.out.println("CREATE_NEW_ORDER");
    }
}
