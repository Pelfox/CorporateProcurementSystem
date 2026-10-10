package com.team.corporate.services.ui;

@FunctionalInterface
public interface ResultCarrier<R> {
    R getResult();
}
