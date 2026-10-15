package com.team.corporate.controllers.dialog;

@FunctionalInterface
public interface ResultCarrier<R> {
    R getResult();
}
