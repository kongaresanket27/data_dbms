package com.instiasset.models;

import java.sql.Date;

/**
 * Asset Model - Represents an asset in the system
 */
public class Asset {
    private int assetId;
    private String assetName;
    private String assetType;
    private Date purchaseDate;
    private String status; // Available, Assigned, Under_Maintenance, Retired
    private Integer deptId; // Nullable
    private Integer locationId; // Nullable

    // Status constants
    public static final String STATUS_AVAILABLE = "Available";
    public static final String STATUS_ASSIGNED = "Assigned";
    public static final String STATUS_UNDER_MAINTENANCE = "Under_Maintenance";
    public static final String STATUS_RETIRED = "Retired";

    // Constructors
    public Asset() {}

    public Asset(int assetId, String assetName, String assetType, Date purchaseDate,
                String status, Integer deptId, Integer locationId) {
        this.assetId = assetId;
        this.assetName = assetName;
        this.assetType = assetType;
        this.purchaseDate = purchaseDate;
        this.status = status;
        this.deptId = deptId;
        this.locationId = locationId;
    }

    // Getters and Setters
    public int getAssetId() { return assetId; }
    public void setAssetId(int assetId) { this.assetId = assetId; }

    public String getAssetName() { return assetName; }
    public void setAssetName(String assetName) { this.assetName = assetName; }

    public String getAssetType() { return assetType; }
    public void setAssetType(String assetType) { this.assetType = assetType; }

    public Date getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(Date purchaseDate) { this.purchaseDate = purchaseDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getDeptId() { return deptId; }
    public void setDeptId(Integer deptId) { this.deptId = deptId; }

    public Integer getLocationId() { return locationId; }
    public void setLocationId(Integer locationId) { this.locationId = locationId; }

    // Business logic methods
    public boolean isAvailable() {
        return STATUS_AVAILABLE.equals(status);
    }

    public boolean isAssigned() {
        return STATUS_ASSIGNED.equals(status);
    }

    public boolean isUnderMaintenance() {
        return STATUS_UNDER_MAINTENANCE.equals(status);
    }

    public boolean isRetired() {
        return STATUS_RETIRED.equals(status);
    }

    public boolean canBeAssigned() {
        return isAvailable() && !isRetired();
    }

    public boolean canBeMaintained() {
        return !isRetired();
    }

    @Override
    public String toString() {
        return String.format("Asset{id=%d, name='%s', type='%s', status='%s'}",
                           assetId, assetName, assetType, status);
    }
}