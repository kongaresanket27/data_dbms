package com.instiasset.models;

import java.sql.Date;

/**
 * AssetAssignment Model - Represents asset assignment history
 */
public class AssetAssignment {
    private int assignmentId;
    private int assetId;
    private int empId;
    private Date assignDate;
    private Date returnDate;

    // Constructors
    public AssetAssignment() {}

    public AssetAssignment(int assignmentId, int assetId, int empId, Date assignDate, Date returnDate) {
        this.assignmentId = assignmentId;
        this.assetId = assetId;
        this.empId = empId;
        this.assignDate = assignDate;
        this.returnDate = returnDate;
    }

    // Getters and Setters
    public int getAssignmentId() { return assignmentId; }
    public void setAssignmentId(int assignmentId) { this.assignmentId = assignmentId; }

    public int getAssetId() { return assetId; }
    public void setAssetId(int assetId) { this.assetId = assetId; }

    public int getEmpId() { return empId; }
    public void setEmpId(int empId) { this.empId = empId; }

    public Date getAssignDate() { return assignDate; }
    public void setAssignDate(Date assignDate) { this.assignDate = assignDate; }

    public Date getReturnDate() { return returnDate; }
    public void setReturnDate(Date returnDate) { this.returnDate = returnDate; }

    // Business logic methods
    public boolean isActive() {
        return returnDate == null;
    }

    public boolean isReturned() {
        return returnDate != null;
    }

    @Override
    public String toString() {
        return String.format("AssetAssignment{id=%d, assetId=%d, empId=%d, active=%s}",
                           assignmentId, assetId, empId, isActive());
    }
}