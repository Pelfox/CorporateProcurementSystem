package com.team.corporate.services.ui;

@FunctionalInterface
public interface DataReceiver<T> {
    void receiveData(T data);
}
