package com.instiasset.models;

/**
 * Department Model - Represents a department in the system
 */
public class Department {
    private int deptId;
    private String deptName;
    private Integer managerId; // Nullable - FK to Employee

    // Constructors
    public Department() {}

    public Department(int deptId, String deptName, Integer managerId) {
        this.deptId = deptId;
        this.deptName = deptName;
        this.managerId = managerId;
    }

    // Getters and Setters
    public int getDeptId() { return deptId; }
    public void setDeptId(int deptId) { this.deptId = deptId; }

    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }

    public Integer getManagerId() { return managerId; }
    public void setManagerId(Integer managerId) { this.managerId = managerId; }

    @Override
    public String toString() {
        return String.format("Department{id=%d, name='%s', managerId=%s}",
                           deptId, deptName, managerId);
    }
}