package com.instiasset.models;

import java.sql.Date;

/**
 * MaintenanceRequest Model - Represents a maintenance request in the system
 */
public class MaintenanceRequest {
    private int requestId;
    private int assetId;
    private int empId;
    private Integer techId; // Nullable
    private Date requestDate;
    private String issueDescription;
    private String priority; // Low, Medium, High
    private String status; // Pending, In_Progress, Completed, Rejected
    private Date completedDate;
    private String remarks;

    // Status constants
    public static final String STATUS_PENDING = "Pending";
    public static final String STATUS_IN_PROGRESS = "In_Progress";
    public static final String STATUS_COMPLETED = "Completed";
    public static final String STATUS_REJECTED = "Rejected";

    // Priority constants
    public static final String PRIORITY_LOW = "Low";
    public static final String PRIORITY_MEDIUM = "Medium";
    public static final String PRIORITY_HIGH = "High";

    // Constructors
    public MaintenanceRequest() {}

    public MaintenanceRequest(int requestId, int assetId, int empId, Integer techId,
                            Date requestDate, String issueDescription, String priority,
                            String status, Date completedDate, String remarks) {
        this.requestId = requestId;
        this.assetId = assetId;
        this.empId = empId;
        this.techId = techId;
        this.requestDate = requestDate;
        this.issueDescription = issueDescription;
        this.priority = priority;
        this.status = status;
        this.completedDate = completedDate;
        this.remarks = remarks;
    }

    // Getters and Setters
    public int getRequestId() { return requestId; }
    public void setRequestId(int requestId) { this.requestId = requestId; }

    public int getAssetId() { return assetId; }
    public void setAssetId(int assetId) { this.assetId = assetId; }

    public int getEmpId() { return empId; }
    public void setEmpId(int empId) { this.empId = empId; }

    public Integer getTechId() { return techId; }
    public void setTechId(Integer techId) { this.techId = techId; }

    public Date getRequestDate() { return requestDate; }
    public void setRequestDate(Date requestDate) { this.requestDate = requestDate; }

    public String getIssueDescription() { return issueDescription; }
    public void setIssueDescription(String issueDescription) { this.issueDescription = issueDescription; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getCompletedDate() { return completedDate; }
    public void setCompletedDate(Date completedDate) { this.completedDate = completedDate; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    // Business logic methods
    public boolean isPending() {
        return STATUS_PENDING.equals(status);
    }

    public boolean isInProgress() {
        return STATUS_IN_PROGRESS.equals(status);
    }

    public boolean isCompleted() {
        return STATUS_COMPLETED.equals(status);
    }

    public boolean isRejected() {
        return STATUS_REJECTED.equals(status);
    }

    public boolean canAssignTechnician() {
        return isPending() && techId == null;
    }

    public boolean canStartWork() {
        return isPending() && techId != null;
    }

    public boolean canComplete() {
        return isInProgress() && techId != null;
    }

    public boolean canReject() {
        return isPending() || isInProgress();
    }

    @Override
    public String toString() {
        return String.format("MaintenanceRequest{id=%d, assetId=%d, status='%s', priority='%s'}",
                           requestId, assetId, status, priority);
    }
}