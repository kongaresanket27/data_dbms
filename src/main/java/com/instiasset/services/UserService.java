package com.instiasset.services;

import com.instiasset.models.*;
import com.instiasset.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * UserService - Handles user authentication and role-based operations
 */
public class UserService {

    private Employee currentUser;

    /**
     * Authenticates a user by full name and numeric ID password.
     * Employee login uses EMPLOYEE full name and Emp_ID.
     * Technician login uses TECHNICIAN name and Tech_ID.
     */
    public Employee authenticateUser(String username, String password) throws SQLException {
        if (username == null || password == null) {
            return null;
        }

        String normalizedName = username.trim().toUpperCase();
        int id;
        try {
            id = Integer.parseInt(password.trim());
        } catch (NumberFormatException e) {
            return null;
        }

        try (Connection conn = DBConnection.getConnection()) {
            // Attempt employee login first
            String sql = "SELECT * FROM EMPLOYEE WHERE UPPER(TRIM(First_Name || ' ' || Last_Name)) = ? AND Emp_ID = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, normalizedName);
                ps.setInt(2, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        currentUser = new Employee(
                            rs.getInt("Emp_ID"),
                            rs.getString("First_Name"),
                            rs.getString("Last_Name"),
                            rs.getString("Email"),
                            rs.getString("Phone"),
                            rs.getInt("Dept_ID"),
                            getStringOrDefault(rs, "Role", "Employee")
                        );
                        return currentUser;
                    }
                }
            }

            // Attempt technician login when employee login fails
            sql = "SELECT * FROM TECHNICIAN WHERE UPPER(TRIM(Tech_Name)) = ? AND Tech_ID = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, normalizedName);
                ps.setInt(2, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        currentUser = new Employee(
                            rs.getInt("Tech_ID"),
                            rs.getString("Tech_Name"),
                            "",                      // no last name split available
                            "",                      // no email for technician login
                            rs.getString("Phone"),
                            0,                        // no department needed for technician
                            "Technician"
                        );
                        return currentUser;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Gets current logged-in user
     */
    public Employee getCurrentUser() {
        return currentUser;
    }

    /**
     * Checks if current user has admin role
     */
    public boolean isAdmin() {
        return currentUser != null && "Admin".equalsIgnoreCase(trimToEmpty(currentUser.getRole()));
    }

    /**
     * Checks if current user has employee role
     */
    public boolean isEmployee() {
        return currentUser != null && "Employee".equalsIgnoreCase(trimToEmpty(currentUser.getRole()));
    }

    /**
     * Checks if current user has technician role
     */
    public boolean isTechnician() {
        return currentUser != null && "Technician".equalsIgnoreCase(trimToEmpty(currentUser.getRole()));
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String getStringOrDefault(ResultSet rs, String columnName, String defaultValue) {
        try {
            String value = rs.getString(columnName);
            return value != null ? value : defaultValue;
        } catch (SQLException ex) {
            return defaultValue;
        }
    }

    /**
     * Gets all employees
     */
    public List<Employee> getAllEmployees() throws SQLException {
        List<Employee> employees = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT * FROM EMPLOYEE ORDER BY Emp_ID";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    employees.add(new Employee(
                        rs.getInt("Emp_ID"),
                        rs.getString("First_Name"),
                        rs.getString("Last_Name"),
                        rs.getString("Email"),
                        rs.getString("Phone"),
                        rs.getInt("Dept_ID"),
                        getStringOrDefault(rs, "Role", "Employee")
                    ));
                }
            }
        }
        return employees;
    }

    /**
     * Returns all employees available for login.
     */
    public List<Employee> getLoginEmployees() throws SQLException {
        List<Employee> employees = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT * FROM EMPLOYEE ORDER BY Emp_ID";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    employees.add(new Employee(
                        rs.getInt("Emp_ID"),
                        rs.getString("First_Name"),
                        rs.getString("Last_Name"),
                        rs.getString("Email"),
                        rs.getString("Phone"),
                        rs.getInt("Dept_ID"),
                        getStringOrDefault(rs, "Role", "Employee")
                    ));
                }
            }
        }
        return employees;
    }

    public Employee getEmployeeById(int empId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT * FROM EMPLOYEE WHERE Emp_ID = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, empId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new Employee(
                            rs.getInt("Emp_ID"),
                            rs.getString("First_Name"),
                            rs.getString("Last_Name"),
                            rs.getString("Email"),
                            rs.getString("Phone"),
                            rs.getInt("Dept_ID"),
                            getStringOrDefault(rs, "Role", "Employee")
                        );
                    }
                }
            }
        }
        return null;
    }

    public List<Technician> getLoginTechnicians() throws SQLException {
        List<Technician> technicians = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT * FROM TECHNICIAN ORDER BY Tech_ID";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    technicians.add(new Technician(
                        rs.getInt("Tech_ID"),
                        rs.getString("Tech_Name"),
                        rs.getString("Specialization"),
                        rs.getString("Phone"),
                        true
                    ));
                }
            }
        }
        return technicians;
    }

    public Employee getTechnicianAsUser(int techId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT * FROM TECHNICIAN WHERE Tech_ID = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, techId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new Employee(
                            rs.getInt("Tech_ID"),
                            rs.getString("Tech_Name"),
                            "",
                            "",
                            rs.getString("Phone"),
                            0,
                            "Technician"
                        );
                    }
                }
            }
        }
        return null;
    }

    public void setCurrentUser(Employee user) {
        this.currentUser = user;
    }

    /**
     * Gets assets assigned to current user
     */
    public List<Asset> getMyAssignedAssets() throws SQLException {
        if (currentUser == null) {
            throw new SecurityException("User not authenticated");
        }

        List<Asset> assets = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT A.* FROM ASSET A " +
                        "JOIN ASSET_ASSIGNMENT AA ON A.Asset_ID = AA.Asset_ID " +
                        "WHERE AA.Emp_ID = ? AND AA.Return_Date IS NULL";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, currentUser.getEmpId());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        assets.add(new Asset(
                            rs.getInt("Asset_ID"),
                            rs.getString("Asset_Name"),
                            rs.getString("Asset_Type"),
                            rs.getDate("Purchase_Date"),
                            rs.getString("Status"),
                            rs.getInt("Dept_ID"),
                            rs.getInt("Location_ID")
                        ));
                    }
                }
            }
        }
        return assets;
    }

    /**
     * Gets maintenance requests created by current user
     */
    public List<MaintenanceRequest> getMyMaintenanceRequests() throws SQLException {
        if (currentUser == null) {
            throw new SecurityException("User not authenticated");
        }

        List<MaintenanceRequest> requests = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT Request_ID, Asset_ID, Emp_ID, Tech_ID, Request_Date, Issue_Description, Priority, Status, Completed_Date, Remarks " +
                         "FROM MAINTENANCE_REQUEST WHERE Emp_ID = ? ORDER BY Request_Date DESC";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, currentUser.getEmpId());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        requests.add(new MaintenanceRequest(
                            rs.getInt("Request_ID"),
                            rs.getInt("Asset_ID"),
                            rs.getInt("Emp_ID"),
                            rs.getInt("Tech_ID"),
                            rs.getDate("Request_Date"),
                            rs.getString("Issue_Description"),
                            rs.getString("Priority"),
                            rs.getString("Status"),
                            rs.getDate("Completed_Date"),
                            rs.getString("Remarks")
                        ));
                    }
                }
            }
        }
        return requests;
    }

    /**
     * Gets maintenance requests assigned to current technician, or all requests for non-technicians
     */
    public List<MaintenanceRequest> getAssignedMaintenanceRequests() throws SQLException {
        List<MaintenanceRequest> requests = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection()) {
            String sql;
            if (isTechnician()) {
                sql = "SELECT Request_ID, Asset_ID, Emp_ID, Tech_ID, Request_Date, Issue_Description, Priority, Status, Completed_Date, Remarks " +
                      "FROM MAINTENANCE_REQUEST WHERE Tech_ID = ? AND Status IN ('In_Progress', 'Pending') ORDER BY Request_Date";
            } else {
                sql = "SELECT Request_ID, Asset_ID, Emp_ID, Tech_ID, Request_Date, Issue_Description, Priority, Status, Completed_Date, Remarks " +
                      "FROM MAINTENANCE_REQUEST ORDER BY Request_Date DESC";
            }
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                if (isTechnician()) {
                    ps.setInt(1, currentUser.getEmpId());
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        requests.add(new MaintenanceRequest(
                            rs.getInt("Request_ID"),
                            rs.getInt("Asset_ID"),
                            rs.getInt("Emp_ID"),
                            rs.getInt("Tech_ID"),
                            rs.getDate("Request_Date"),
                            rs.getString("Issue_Description"),
                            rs.getString("Priority"),
                            rs.getString("Status"),
                            rs.getDate("Completed_Date"),
                            rs.getString("Remarks")
                        ));
                    }
                }
            }
        }
        return requests;
    }

    /**
     * Gets all technicians
     */
    public List<Technician> getAllTechnicians() throws SQLException {
        List<Technician> technicians = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT * FROM TECHNICIAN ORDER BY Tech_ID";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    technicians.add(new Technician(
                        rs.getInt("Tech_ID"),
                        rs.getString("Tech_Name"),
                        rs.getString("Specialization"),
                        rs.getString("Phone"),
                        true
                    ));
                }
            }
        }
        return technicians;
    }
}