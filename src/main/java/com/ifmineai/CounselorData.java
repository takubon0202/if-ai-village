package com.ifmineai;

import org.bukkit.configuration.ConfigurationSection;

import java.util.UUID;

public class CounselorData {

    private UUID entityUUID;
    private String worldName;
    private double originX;
    private double originY;
    private double originZ;
    private String direction;
    private int range;
    private String personalityType;
    private boolean aiEnabled;

    public CounselorData(UUID entityUUID, String worldName,
                         double originX, double originY, double originZ,
                         String direction, int range) {
        this(entityUUID, worldName, originX, originY, originZ, direction, range, "counselor", false);
    }

    public CounselorData(UUID entityUUID, String worldName,
                         double originX, double originY, double originZ,
                         String direction, int range,
                         String personalityType, boolean aiEnabled) {
        this.entityUUID = entityUUID;
        this.worldName = worldName;
        this.originX = originX;
        this.originY = originY;
        this.originZ = originZ;
        this.direction = direction;
        this.range = range;
        this.personalityType = personalityType;
        this.aiEnabled = aiEnabled;
    }

    public UUID getEntityUUID() {
        return entityUUID;
    }

    public String getWorldName() {
        return worldName;
    }

    public double getOriginX() {
        return originX;
    }

    public double getOriginY() {
        return originY;
    }

    public double getOriginZ() {
        return originZ;
    }

    public String getDirection() {
        return direction;
    }

    public int getRange() {
        return range;
    }

    public String getPersonalityType() {
        return personalityType;
    }

    public void setPersonalityType(String personalityType) {
        this.personalityType = personalityType;
    }

    public boolean isAiEnabled() {
        return aiEnabled;
    }

    public void setAiEnabled(boolean aiEnabled) {
        this.aiEnabled = aiEnabled;
    }

    public void toConfigSection(ConfigurationSection section) {
        section.set("uuid", entityUUID.toString());
        section.set("world", worldName);
        section.set("originX", originX);
        section.set("originY", originY);
        section.set("originZ", originZ);
        section.set("direction", direction);
        section.set("range", range);
        section.set("personalityType", personalityType);
        section.set("aiEnabled", aiEnabled);
    }

    public static CounselorData fromConfigSection(ConfigurationSection section) {
        String uuidStr = section.getString("uuid");
        if (uuidStr == null) {
            return null;
        }
        UUID uuid = UUID.fromString(uuidStr);
        String world = section.getString("world", "world");
        double ox = section.getDouble("originX");
        double oy = section.getDouble("originY");
        double oz = section.getDouble("originZ");
        String dir = section.getString("direction", "north");
        int rng = section.getInt("range", 5);
        String personality = section.getString("personalityType", "counselor");
        boolean ai = section.getBoolean("aiEnabled", false);
        return new CounselorData(uuid, world, ox, oy, oz, dir, rng, personality, ai);
    }
}
