package me.koyere.antiafkplus.api.data;

/**
 * Types of player activities that can be tracked by the AFK system.
 */
public enum ActivityType {
    /**
     * Player moved their character (walking, running, swimming, etc.).
     */
    MOVEMENT("Movement", 1.0),
    
    /**
     * Player rotated their head (looking around).
     */
    HEAD_ROTATION("Head Rotation", 1.5),
    
    /**
     * Player jumped.
     */
    JUMP("Jump", 0.8),
    
    /**
     * Player executed a command.
     */
    COMMAND("Command", 2.0),
    
    /**
     * Player sent a chat message.
     */
    CHAT("Chat", 2.5),
    
    /**
     * Player interacted with a block or entity.
     */
    INTERACTION("Interaction", 1.8),
    
    /**
     * Player broke a block.
     */
    BLOCK_BREAK("Block Break", 1.5),
    
    /**
     * Player placed a block.
     */
    BLOCK_PLACE("Block Place", 1.5),
    
    /**
     * Player used an item.
     */
    ITEM_USE("Item Use", 1.2),
    
    /**
     * Player attacked or damaged something.
     */
    COMBAT("Combat", 2.0),
    
    /**
     * Player opened an inventory or container.
     */
    INVENTORY("Inventory", 1.3),
    
    /**
     * Player dropped an item.
     */
    ITEM_DROP("Item Drop", 1.0),
    
    /**
     * Player picked up an item.
     */
    ITEM_PICKUP("Item Pickup", 1.1),
    
    /**
     * Player changed their held item.
     */
    ITEM_SWITCH("Item Switch", 0.5),
    
    /**
     * Player teleported or was teleported.
     */
    TELEPORT("Teleport", 0.3),
    
    /**
     * Player changed worlds.
     */
    WORLD_CHANGE("World Change", 0.2),
    
    /**
     * Player performed fishing activity.
     */
    FISHING("Fishing", 1.4),
    
    /**
     * Custom activity recorded by another plugin.
     */
    CUSTOM("Custom", 1.0),
    
    /**
     * Unknown or unclassified activity.
     */
    UNKNOWN("Unknown", 0.5);
    
    private final String displayName;
    private final double activityWeight;
    
    ActivityType(String displayName, double activityWeight) {
        this.displayName = displayName;
        this.activityWeight = activityWeight;
    }
    
    /**
     * Get the human-readable display name for this activity type.
     * @return The display name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Get the activity weight for calculating activity scores.
     * Higher weights indicate more intentional activities.
     * @return The activity weight (typically 0.0 to 3.0)
     */
    public double getActivityWeight() {
        return activityWeight;
    }
    
    /**
     * Check if this activity type indicates high player engagement.
     * @return true if this is a high-engagement activity
     */
    public boolean isHighEngagement() {
        return activityWeight >= 2.0;
    }
    
    /**
     * Check if this activity type is easily automated.
     * @return true if this activity can be easily automated
     */
    public boolean isEasilyAutomated() {
        return this == JUMP || this == ITEM_SWITCH || this == TELEPORT || this == WORLD_CHANGE;
    }
    
    /**
     * Check if this activity type requires direct player input.
     * @return true if this activity requires intentional player action
     */
    public boolean requiresPlayerInput() {
        return this == CHAT || this == COMMAND || this == COMBAT || this == INTERACTION;
    }
}