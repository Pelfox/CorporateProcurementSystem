package com.team.corporate.exceptions;

public class WarehouseNotFoundException extends RuntimeException {

    public WarehouseNotFoundException(){
        super("Warehouse not found.");
    }

    public WarehouseNotFoundException(String message) {
        super(message);
    }

    public WarehouseNotFoundException(String message, Throwable cause){
        super(message, cause);
    }
}
