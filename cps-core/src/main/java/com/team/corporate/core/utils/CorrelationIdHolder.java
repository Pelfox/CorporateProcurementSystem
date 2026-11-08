package com.team.corporate.core.utils;

public final class CorrelationIdHolder {
    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();
    private CorrelationIdHolder() {}
    public static void set(String id) { CURRENT.set(id); }
    public static String get() { return CURRENT.get(); }
    public static void clear() { CURRENT.remove(); }
}