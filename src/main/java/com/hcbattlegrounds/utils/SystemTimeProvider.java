package com.hcbattlegrounds.utils;

public class SystemTimeProvider implements TimeProvider {
    @Override
    public long now() {
        return System.currentTimeMillis();
    }
}
