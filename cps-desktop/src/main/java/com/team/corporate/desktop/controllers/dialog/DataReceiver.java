package com.team.corporate.desktop.controllers.dialog;

@FunctionalInterface
public interface DataReceiver<T> {
    void receiveData(T data);
}
