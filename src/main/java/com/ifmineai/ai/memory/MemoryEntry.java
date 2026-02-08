package com.ifmineai.ai.memory;

public record MemoryEntry(
        MemoryType type,
        String content,
        int importance,
        long timestamp
) {
    public MemoryEntry(MemoryType type, String content, int importance) {
        this(type, content, importance, System.currentTimeMillis());
    }
}
