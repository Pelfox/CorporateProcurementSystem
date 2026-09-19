package com.team.corporate.exceptions;

public class OrderItemNotFoundException extends RuntimeException {

    public OrderItemNotFoundException(){
        super("Order Item not found.");
    }

    public OrderItemNotFoundException(String message){
        super(message);
    }

    public OrderItemNotFoundException(String message, Throwable cause){
        super(message, cause);
    }
}
