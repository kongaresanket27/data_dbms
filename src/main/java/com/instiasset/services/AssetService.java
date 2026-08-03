package com.instiasset.services;

import com.instiasset.models.*;
import com.instiasset.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * AssetService - Handles all asset-related business logic
 */
public class AssetService {

    /**
     * Assigns an asset to an employee
     * Business Rules:
     * - Asset must be Available
     * - No active assignment for the asset
     * - Updates asset status to Assigned
     */
    public boolean assignAsset(int assetId, int empId) throws SQLException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // Check if asset is available
            Asset asset = getAssetById(assetId);
            if (asset == null || !asset.canBeAssigned()) {
                throw new IllegalStateException("Asset is not available for assignment");
            }

            // Check for active assignments (asset can only be assigned to 1 employee at a time)
            if (hasActiveAssignment(assetId)) {
                int currentOwnerEmpId = getActiveAssignmentOwnerEmpId(assetId);
                throw new IllegalStateException(
                        "Trigger: Asset " + assetId + " is already actively assigned to Emp_ID " + currentOwnerEmpId + "."
                );
            }




            // Create assignment
            String insertSql = "INSERT INTO ASSET_ASSIGNMENT (Assignment_ID, Asset_ID, Emp_ID, Assign_Date) " +
                             "VALUES (?, ?, ?, SYSDATE)";
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                int assignmentId = generateAssignmentId();
                ps.setInt(1, assignmentId);
                ps.setInt(2, assetId);
                ps.setInt(3, empId);
                ps.executeUpdate();
            }

            // Update asset status
            updateAssetStatus(assetId, Asset.STATUS_ASSIGNED);

            conn.commit();
            return true;

        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    /**
     * Returns an asset from an employee
     * Business Rules:
     * - Must have active assignment
     * - Updates asset status to Available
     */
    public boolean returnAsset(int assetId, int empId) throws SQLException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // Find active assignment
            AssetAssignment assignment = getActiveAssignment(assetId, empId);
            if (assignment == null) {
                throw new IllegalStateException("No active assignment found for this asset and employee");
            }

            // Update assignment with return date
            String updateSql = "UPDATE ASSET_ASSIGNMENT SET Return_Date = SYSDATE WHERE Assignment_ID = ?";
            try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setInt(1, assignment.getAssignmentId());
                ps.executeUpdate();
            }

            // Update asset status
            updateAssetStatus(assetId, Asset.STATUS_AVAILABLE);

            conn.commit();
            return true;

        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    /**
     * Creates a maintenance request
     * Business Rules:
     * - Asset must not be Retired
     * - No active maintenance request for the asset
     * - Automatically sets asset status to Under_Maintenance
     */
    public int createMaintenanceRequest(int assetId, int empId, String issueDescription, String priority) throws SQLException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // Validate asset
            Asset asset = getAssetById(assetId);
            if (asset == null || !asset.canBeMaintained()) {
                throw new IllegalStateException("Asset cannot be maintained");
            }

            // Check for active maintenance requests
            if (hasActiveMaintenanceRequest(assetId)) {
                throw new IllegalStateException("Asset already has an active maintenance request");
            }

            // Create maintenance request
            String insertSql = "INSERT INTO MAINTENANCE_REQUEST " +
                             "(Request_ID, Asset_ID, Emp_ID, Tech_ID, Issue_Description, Priority, Status, Request_Date) " +
                             "VALUES (?, ?, ?, NULL, ?, ?, 'Pending', SYSDATE)";
            int requestId = generateRequestId();
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setInt(1, requestId);
                ps.setInt(2, assetId);
                ps.setInt(3, empId);
                ps.setString(4, issueDescription);
                ps.setString(5, priority);
                ps.executeUpdate();
            }

            // Update asset status
            updateAssetStatus(assetId, Asset.STATUS_UNDER_MAINTENANCE);

            conn.commit();
            return requestId;

        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    /**
     * Assigns a technician to a maintenance request
     */
    public boolean assignTechnicianToRequest(int requestId, int techId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "UPDATE MAINTENANCE_REQUEST SET Tech_ID = ?, Status = 'In_Progress' " +
                        "WHERE Request_ID = ? AND Status = 'Pending' AND Tech_ID IS NULL";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, techId);
                ps.setInt(2, requestId);
                return ps.executeUpdate() > 0;
            }
        }
    }

    /**
     * Completes a maintenance request
     * Business Rules:
     * - Sets asset status back to Available
     * - Sets completed date
     */
    public boolean completeMaintenanceRequest(int requestId, String remarks) throws SQLException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // Get asset ID from request
            int assetId = getAssetIdFromRequest(requestId);
            if (assetId == -1) {
                throw new IllegalStateException("Maintenance request not found");
            }

            // Update request
            String updateSql = "UPDATE MAINTENANCE_REQUEST SET Status = 'Completed', Completed_Date = SYSDATE, Remarks = ? " +
                               "WHERE Request_ID = ? AND Status = 'In_Progress'";
            try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setString(1, remarks);
                ps.setInt(2, requestId);
                if (ps.executeUpdate() == 0) {
                    throw new IllegalStateException("Cannot complete request - invalid status or request not found");
                }
            }

            // Update asset status
            updateAssetStatus(assetId, Asset.STATUS_AVAILABLE);

            conn.commit();
            return true;

        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    // Helper methods

    private Asset getAssetById(int assetId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT * FROM ASSET WHERE Asset_ID = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, assetId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new Asset(
                            rs.getInt("Asset_ID"),
                            rs.getString("Asset_Name"),
                            rs.getString("Asset_Type"),
                            rs.getDate("Purchase_Date"),
                            rs.getString("Status"),
                            rs.getInt("Dept_ID"),
                            rs.getInt("Location_ID")
                        );
                    }
                }
            }
        }
        return null;
    }

    private boolean hasActiveAssignment(int assetId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT COUNT(*) FROM ASSET_ASSIGNMENT WHERE Asset_ID = ? AND Return_Date IS NULL";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, assetId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }
            }
        }
    }

    private boolean hasActiveMaintenanceRequest(int assetId) throws SQLException {

        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT COUNT(*) FROM MAINTENANCE_REQUEST WHERE Asset_ID = ? AND Status IN ('Pending', 'In_Progress')";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, assetId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }
            }
        }
    }

    private int getActiveAssignmentOwnerEmpId(int assetId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT Emp_ID FROM ASSET_ASSIGNMENT WHERE Asset_ID = ? AND Return_Date IS NULL";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, assetId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("Emp_ID");
                    }
                }
            }
        }
        return -1;
    }

    private AssetAssignment getActiveAssignment(int assetId, int empId) throws SQLException {

        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT * FROM ASSET_ASSIGNMENT WHERE Asset_ID = ? AND Emp_ID = ? AND Return_Date IS NULL";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, assetId);
                ps.setInt(2, empId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new AssetAssignment(
                            rs.getInt("Assignment_ID"),
                            rs.getInt("Asset_ID"),
                            rs.getInt("Emp_ID"),
                            rs.getDate("Assign_Date"),
                            rs.getDate("Return_Date")
                        );
                    }
                }
            }
        }
        return null;
    }

    private int getAssetIdFromRequest(int requestId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT Asset_ID FROM MAINTENANCE_REQUEST WHERE Request_ID = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, requestId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : -1;
                }
            }
        }
    }

    private void updateAssetStatus(int assetId, String status) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "UPDATE ASSET SET Status = ? WHERE Asset_ID = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, status);
                ps.setInt(2, assetId);
                ps.executeUpdate();
            }
        }
    }

    private int generateAssignmentId() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT NVL(MAX(Assignment_ID), 0) + 1 FROM ASSET_ASSIGNMENT";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                return rs.next() ? rs.getInt(1) : 1;
            }
        }
    }

    private int generateRequestId() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT NVL(MAX(Request_ID), 0) + 1 FROM MAINTENANCE_REQUEST";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                return rs.next() ? rs.getInt(1) : 1;
            }
        }
    }

    /**
     * Starts a maintenance request (technician only)
     */
    public void startMaintenanceRequest(int requestId, int techId) throws SQLException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // Check if request exists and is assigned to this technician
            String checkSql = "SELECT Status, Asset_ID FROM MAINTENANCE_REQUEST WHERE Request_ID = ? AND Tech_ID = ?";
            try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                checkPs.setInt(1, requestId);
                checkPs.setInt(2, techId);
                try (ResultSet rs = checkPs.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("Request not found or not assigned to you");
                    }
                    String status = rs.getString("Status");
                    if (!"Pending".equals(status)) {
                        throw new IllegalStateException("Request is not in pending status");
                    }
                }
            }

            // Update status to In_Progress
            String updateSql = "UPDATE MAINTENANCE_REQUEST SET Status = 'In_Progress' WHERE Request_ID = ?";
            try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
                updatePs.setInt(1, requestId);
                updatePs.executeUpdate();
            }

            conn.commit();
        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException closeEx) {
                    closeEx.printStackTrace();
                }
            }
        }
    }

    /**
     * Completes a maintenance request (technician only)
     */
    public void completeMaintenanceRequest(int requestId, int techId, String remarks) throws SQLException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // Check if request exists and is assigned to this technician
            String checkSql = "SELECT Status, Asset_ID FROM MAINTENANCE_REQUEST WHERE Request_ID = ? AND Tech_ID = ?";
            int assetId;
            try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                checkPs.setInt(1, requestId);
                checkPs.setInt(2, techId);
                try (ResultSet rs = checkPs.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("Request not found or not assigned to you");
                    }
                    String status = rs.getString("Status");
                    if (!"In_Progress".equals(status)) {
                        throw new IllegalStateException("Request is not in progress");
                    }
                    assetId = rs.getInt("Asset_ID");
                }
            }

            // Update request status, completed date, and remarks
            String updateSql = "UPDATE MAINTENANCE_REQUEST SET Status = 'Completed', Completed_Date = SYSDATE, Remarks = ? WHERE Request_ID = ?";
            try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
                updatePs.setString(1, remarks);
                updatePs.setInt(2, requestId);
                updatePs.executeUpdate();
            }

            // Check if asset was under maintenance and update status
            String assetCheckSql = "SELECT Status FROM ASSET WHERE Asset_ID = ?";
            try (PreparedStatement assetCheckPs = conn.prepareStatement(assetCheckSql)) {
                assetCheckPs.setInt(1, assetId);
                try (ResultSet rs = assetCheckPs.executeQuery()) {
                    if (rs.next() && "Under_Maintenance".equals(rs.getString("Status"))) {
                        // Update asset status back to Available
                        String assetUpdateSql = "UPDATE ASSET SET Status = 'Available' WHERE Asset_ID = ?";
                        try (PreparedStatement assetUpdatePs = conn.prepareStatement(assetUpdateSql)) {
                            assetUpdatePs.setInt(1, assetId);
                            assetUpdatePs.executeUpdate();
                        }
                    }
                }
            }

            conn.commit();
        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException closeEx) {
                    closeEx.printStackTrace();
                }
            }
        }
    }

    /**
     * Gets all maintenance requests
     */
    public List<MaintenanceRequest> getAllMaintenanceRequests() throws SQLException {
        List<MaintenanceRequest> requests = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT Request_ID, Asset_ID, Emp_ID, Tech_ID, Request_Date, Issue_Description, Priority, Status, Completed_Date, Remarks " +
                         "FROM MAINTENANCE_REQUEST ORDER BY Request_Date DESC";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
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
        return requests;
    }

    public int getAssetCount() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT COUNT(*) FROM ASSET";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public int getMaintenanceRequestCount() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT COUNT(*) FROM MAINTENANCE_REQUEST";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public int getMaintenanceRequestCountByStatus(String status) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT COUNT(*) FROM MAINTENANCE_REQUEST WHERE Status = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, status);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            }
        }
    }

    /**
     * Gets all assets
     */
    public List<Asset> getAllAssets() throws SQLException {
        List<Asset> assets = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT * FROM ASSET ORDER BY Asset_ID";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
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
        return assets;
    }
}