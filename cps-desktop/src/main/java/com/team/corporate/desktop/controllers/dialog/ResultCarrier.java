package com.team.corporate.desktop.controllers.dialog;

@FunctionalInterface
public interface ResultCarrier<R> {
    R getResult();
}
