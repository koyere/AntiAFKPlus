package me.koyere.antiafkplus.api.events;

import java.util.UUID;

/**
 * Registration handle for API event listeners.
 */
public class EventRegistration {
    
    private final UUID registrationId;
    private final String eventType;
    private final Object listener;
    private final long registrationTime;
    private boolean unregistered = false;
    
    public EventRegistration(String eventType, Object listener) {
        this.registrationId = UUID.randomUUID();
        this.eventType = eventType;
        this.listener = listener;
        this.registrationTime = System.currentTimeMillis();
    }
    
    /**
     * Get the unique registration ID.
     * @return The registration ID
     */
    public UUID getRegistrationId() {
        return registrationId;
    }
    
    /**
     * Get the event type this registration is for.
     * @return The event type
     */
    public String getEventType() {
        return eventType;
    }
    
    /**
     * Get the listener object.
     * @return The listener
     */
    public Object getListener() {
        return listener;
    }
    
    /**
     * Get the registration timestamp.
     * @return When this was registered (milliseconds since epoch)
     */
    public long getRegistrationTime() {
        return registrationTime;
    }
    
    /**
     * Check if this registration has been unregistered.
     * @return true if unregistered
     */
    public boolean isUnregistered() {
        return unregistered;
    }
    
    /**
     * Mark this registration as unregistered.
     * This is called internally when the listener is removed.
     */
    public void markUnregistered() {
        this.unregistered = true;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventRegistration that = (EventRegistration) o;
        return registrationId.equals(that.registrationId);
    }
    
    @Override
    public int hashCode() {
        return registrationId.hashCode();
    }
    
    @Override
    public String toString() {
        return String.format("EventRegistration{id=%s, type=%s, unregistered=%s}", 
                registrationId, eventType, unregistered);
    }
}