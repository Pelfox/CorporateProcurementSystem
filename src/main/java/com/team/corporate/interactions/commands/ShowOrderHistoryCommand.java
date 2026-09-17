package com.team.corporate.interactions.commands;

public class ShowOrderHistoryCommand implements CommandInterface{
    @Override
    public void execute() {
        System.out.println("ORDERS_HISTORY");
    }
}
