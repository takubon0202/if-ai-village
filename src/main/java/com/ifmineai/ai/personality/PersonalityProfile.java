package com.ifmineai.ai.personality;

public record PersonalityProfile(
        String type,
        String displayName,
        String description,
        int friendliness,
        int curiosity,
        int sociability,
        String speechStyle
) {
    public static PersonalityProfile defaultProfile() {
        return new PersonalityProfile(
                "counselor",
                "相談員",
                "親切で穏やかな相談員。困っている人を見つけると声をかける。",
                8, 5, 7,
                "丁寧語で穏やかに話す"
        );
    }
}
