package com.team.corporate.controllers.dialog;

@FunctionalInterface
public interface DataReceiver<T> {
    void receiveData(T data);
}
