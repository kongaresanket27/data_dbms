package com.instiasset.models;

/**
 * Technician Model - Represents a technician in the system
 */
public class Technician {
    private int techId;
    private String techName;
    private String specialization;
    private String phone;
    private boolean isActive;

    // Constructors
    public Technician() {}

    public Technician(int techId, String techName, String specialization, String phone, boolean isActive) {
        this.techId = techId;
        this.techName = techName;
        this.specialization = specialization;
        this.phone = phone;
        this.isActive = isActive;
    }

    // Getters and Setters
    public int getTechId() { return techId; }
    public void setTechId(int techId) { this.techId = techId; }

    public String getTechName() { return techName; }
    public void setTechName(String techName) { this.techName = techName; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    @Override
    public String toString() {
        return String.format("Technician{id=%d, name='%s', specialization='%s', active=%s}",
                           techId, techName, specialization, isActive);
    }
}