package com.instiasset.models;

/**
 * Employee Model - Represents an employee in the system
 */
public class Employee {
    private int empId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private Integer deptId; // Nullable
    private String role; // Admin, Employee, Technician

    // Constructors
    public Employee() {}

    public Employee(int empId, String firstName, String lastName, String email, String phone, Integer deptId, String role) {
        this.empId = empId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.deptId = deptId;
        this.role = role;
    }

    // Getters and Setters
    public int getEmpId() { return empId; }
    public void setEmpId(int empId) { this.empId = empId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Integer getDeptId() { return deptId; }
    public void setDeptId(Integer deptId) { this.deptId = deptId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    @Override
    public String toString() {
        return String.format("Employee{id=%d, name='%s %s', email='%s', role='%s'}",
                           empId, firstName, lastName, email, role);
    }
}