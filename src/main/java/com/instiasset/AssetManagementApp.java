package com.instiasset;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Vector;
import java.util.List;

// Import models and services
import com.instiasset.models.*;
import com.instiasset.services.*;

/**
 * Custom button with smooth rounded corners
 */
class RoundedButton extends JButton {
    private int cornerRadius = 15;

    public RoundedButton(String text) {
        super(text);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    public RoundedButton(String text, int radius) {
        super(text);
        this.cornerRadius = radius;
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Draw background
        g2d.setColor(getBackground());
        g2d.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);

        // Draw text
        super.paintComponent(g);
    }

    @Override
    protected void paintBorder(Graphics g) {
        // No border needed since we're drawing rounded background
    }
}

/**
 * Main Application Frame for the Institute Asset Management System.
 * Built with Java Swing and direct JDBC connectivity.
 * Enhanced with proper business logic, role-based access, and workflow management.
 */
public class AssetManagementApp extends JFrame {

    // Services
    private final AssetService assetService = new AssetService();
    private final UserService userService = new UserService();

    // Current user
    private Employee currentUser;

    // Schema helpers
    private Boolean departmentHasManagerNameColumn = null;

    private JPanel contentPanel;
    private CardLayout cardLayout;
    private JPanel dashboardPanel;

    public AssetManagementApp() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);

        try {
            List<Employee> employees = userService.getAllEmployees();
            currentUser = employees.stream()
                    .filter(emp -> emp.getRole() != null && "Admin".equalsIgnoreCase(emp.getRole().trim()))
                    .findFirst()
                    .orElse(employees.isEmpty()
                            ? new Employee(0, "System", "User", "system@instiasset", "", 0, "Admin")
                            : employees.get(0));
            userService.setCurrentUser(currentUser);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error loading user data: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            currentUser = new Employee(0, "System", "User", "system@instiasset", "", 0, "Admin");
            userService.setCurrentUser(currentUser);
        }

        setTitle("InstiAsset - Database Dashboard");

        setLayout(new BorderLayout());

        setupSidebar();
        setupContentArea();

        setVisible(true);
    }

    private void setupSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(200, 700));
        sidebar.setBackground(new Color(33, 37, 41));
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));

        JLabel title = new JLabel("InstiAsset");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(title);
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));

        JLabel subtitle = new JLabel("Asset Management");
        subtitle.setForeground(new Color(189, 195, 199));
        subtitle.setFont(new Font("Arial", Font.PLAIN, 12));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(subtitle);
        sidebar.add(Box.createRigidArea(new Dimension(0, 30)));

        // Navigation menu for each role
        String[] menuItems = new String[]{"Dashboard", "Asset Catalog", "Maintenance Center", "Employees", "Technicians", "Departments", "Department Locations"};

        for (String item : menuItems) {
            JButton btn = new RoundedButton(item, 10);
            btn.setMaximumSize(new Dimension(180, 40));
            btn.setBackground(new Color(33, 37, 41));
            btn.setForeground(Color.LIGHT_GRAY);
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);

            btn.addActionListener(e -> {
                if ("Dashboard".equals(item)) {
                    refreshDashboardPanel();
                }
                cardLayout.show(contentPanel, item);
            });

            sidebar.add(btn);
            sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        // Logout button
        JButton logoutBtn = new RoundedButton("Logout", 10);
        logoutBtn.setMaximumSize(new Dimension(180, 40));
        logoutBtn.setBackground(new Color(192, 57, 43));
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        logoutBtn.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(this, "Are you sure you want to logout?", "Logout", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                dispose();
                System.exit(0);
            }
        });

        sidebar.add(Box.createVerticalGlue());
        sidebar.add(logoutBtn);

        add(sidebar, BorderLayout.WEST);
    }

    private void setupContentArea() {
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(Color.WHITE);

        dashboardPanel = createDashboardPanel();
        contentPanel.add(dashboardPanel, "Dashboard");
        contentPanel.add(createAssetPanel(), "Asset Catalog");
        contentPanel.add(createMaintenancePanel(), "Maintenance Center");
        contentPanel.add(createEmployeePanel(), "Employees");
        contentPanel.add(createTechnicianPanel(), "Technicians");
        contentPanel.add(createDepartmentPanel(), "Departments");
        contentPanel.add(createDepartmentLocationPanel(), "Department Locations");
        contentPanel.add(createEmployeeMaintenancePanel(), "New Maintenance Request");
        contentPanel.add(createTechnicianMaintenancePanel(), "My Tasks");

        add(contentPanel, BorderLayout.CENTER);
    }

    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(new Color(236, 239, 241));

        panel.add(createDashboardHeaderPanel(), BorderLayout.NORTH);
        panel.add(createDashboardCenterPanel(), BorderLayout.CENTER);

        return panel;
    }

    private JPanel createDashboardHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout(10, 10));
        header.setBackground(new Color(236, 239, 241));
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JLabel title = new JLabel("Dashboard");
        title.setFont(new Font("Segoe UI", Font.BOLD, 32));
        title.setForeground(new Color(44, 62, 80));

        JLabel subtitle = new JLabel("Overview of assets and maintenance request health.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        subtitle.setForeground(new Color(115, 115, 115));

        header.add(title, BorderLayout.WEST);
        header.add(subtitle, BorderLayout.SOUTH);
        return header;
    }

    private JPanel createDashboardCenterPanel() {
        JPanel center = new JPanel(new BorderLayout(20, 20));
        center.setOpaque(false);
        center.add(createDashboardStatsPanel(), BorderLayout.NORTH);
        return center;
    }

    private JPanel createDashboardChartPanel() {
        JPanel chartPanel = new JPanel(new BorderLayout());
        chartPanel.setBackground(Color.WHITE);
        chartPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(214, 219, 222)),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        JLabel chartTitle = new JLabel("Maintenance Status Overview");
        chartTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        chartTitle.setForeground(new Color(44, 62, 80));
        chartTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        chartPanel.add(chartTitle, BorderLayout.NORTH);

        try {
            int pendingRequests = assetService.getMaintenanceRequestCountByStatus("Pending");
            int inProgressRequests = assetService.getMaintenanceRequestCountByStatus("In_Progress");
            int completedRequests = assetService.getMaintenanceRequestCountByStatus("Completed");

            chartPanel.add(new StatusBarChart(pendingRequests, inProgressRequests, completedRequests), BorderLayout.CENTER);
        } catch (Exception e) {
            JLabel errorLabel = new JLabel("Unable to load chart data.", SwingConstants.CENTER);
            errorLabel.setForeground(Color.RED);
            chartPanel.add(errorLabel, BorderLayout.CENTER);
        }

        return chartPanel;
    }

    private void refreshDashboardPanel() {
        if (dashboardPanel == null) {
            return;
        }
        dashboardPanel.removeAll();
        dashboardPanel.add(createDashboardHeaderPanel(), BorderLayout.NORTH);
        dashboardPanel.add(createDashboardCenterPanel(), BorderLayout.CENTER);
        dashboardPanel.revalidate();
        dashboardPanel.repaint();
    }

    private JPanel createDashboardStatsPanel() {
        JPanel statsGrid = new JPanel(new GridLayout(1, 5, 20, 20));
        statsGrid.setBackground(Color.WHITE);

        try {
            int totalAssets = assetService.getAssetCount();
            int totalRequests = assetService.getMaintenanceRequestCount();
            int pendingRequests = assetService.getMaintenanceRequestCountByStatus("Pending");
            int inProgressRequests = assetService.getMaintenanceRequestCountByStatus("In_Progress");
            int completedRequests = assetService.getMaintenanceRequestCountByStatus("Completed");

            statsGrid.add(createStatCard("Total Assets", String.valueOf(totalAssets), new Color(41, 128, 185)));
            statsGrid.add(createStatCard("Total Requests", String.valueOf(totalRequests), new Color(192, 57, 43)));
            statsGrid.add(createStatCard("Pending", String.valueOf(pendingRequests), new Color(243, 156, 18)));
            statsGrid.add(createStatCard("In Progress", String.valueOf(inProgressRequests), new Color(231, 76, 60)));
            statsGrid.add(createStatCard("Completed", String.valueOf(completedRequests), new Color(39, 174, 96)));
        } catch (Exception e) {
            // Fallback stats
            statsGrid.add(createStatCard("Total Assets", "N/A", new Color(41, 128, 185)));
            statsGrid.add(createStatCard("Total Requests", "N/A", new Color(192, 57, 43)));
            statsGrid.add(createStatCard("Pending", "N/A", new Color(243, 156, 18)));
            statsGrid.add(createStatCard("In Progress", "N/A", new Color(231, 76, 60)));
            statsGrid.add(createStatCard("Completed", "N/A", new Color(39, 174, 96)));
        }

        return statsGrid;
    }

    private JPanel createStatCard(String title, String value, Color color) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(color);
        card.setPreferredSize(new Dimension(200, 120));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color.darker(), 2),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        
        JLabel lblTitle = new JLabel(title, SwingConstants.LEFT);
        lblTitle.setForeground(Color.WHITE);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        
        JLabel lblValue = new JLabel(value, SwingConstants.LEFT);
        lblValue.setForeground(Color.WHITE);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 40));

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(lblValue, BorderLayout.CENTER);
        return card;
    }

    private JPanel createAssetPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JLabel title = new JLabel("Asset Repository (Oracle DB)");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        panel.add(title, BorderLayout.NORTH);

        String[] columns = {"ID", "Name", "Type", "Status", "Purchase Date", "Assigned To", "Assigned Date"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table read-only
            }
        };
        JTable table = new JTable(model);
        table.setRowHeight(30);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        loadAssetData(model);

        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addBtn = new RoundedButton("Add Asset", 12);
        JButton editBtn = new RoundedButton("Edit Asset", 12);
        JButton assignBtn = new RoundedButton("Assign Asset", 12);
        JButton deleteBtn = new RoundedButton("Delete Asset", 12);
        JButton refreshBtn = new RoundedButton("Refresh", 12);

        addBtn.setBackground(new Color(39, 174, 96));
        addBtn.setForeground(Color.WHITE);
        editBtn.setBackground(new Color(41, 128, 185));
        editBtn.setForeground(Color.WHITE);
        assignBtn.setBackground(new Color(155, 89, 182));
        assignBtn.setForeground(Color.WHITE);
        deleteBtn.setBackground(new Color(192, 57, 43));
        deleteBtn.setForeground(Color.WHITE);
        refreshBtn.setBackground(new Color(149, 165, 166));
        refreshBtn.setForeground(Color.WHITE);

        addBtn.addActionListener(e -> showAddAssetDialog(model));
        editBtn.addActionListener(e -> showEditAssetDialog(table, model));
        assignBtn.addActionListener(e -> showAssignAssetDialog(table, model));
        deleteBtn.addActionListener(e -> deleteAsset(table, model));
        refreshBtn.addActionListener(e -> loadAssetData(model));

        buttonPanel.add(addBtn);
        buttonPanel.add(editBtn);
        buttonPanel.add(assignBtn);
        buttonPanel.add(deleteBtn);
        buttonPanel.add(refreshBtn);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createEmployeePanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JLabel title = new JLabel("Employee & Faculty Directory");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        panel.add(title, BorderLayout.NORTH);

        String[] columns = {"ID", "First Name", "Last Name", "Email", "Phone", "Dept ID"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(model);
        table.setRowHeight(30);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        loadEmployeeData(model);

        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addBtn = new RoundedButton("Add Employee", 12);
        JButton editBtn = new RoundedButton("Edit Employee", 12);
        JButton deleteBtn = new RoundedButton("Delete Employee", 12);
        JButton refreshBtn = new RoundedButton("Refresh", 12);

        addBtn.setBackground(new Color(39, 174, 96));
        addBtn.setForeground(Color.WHITE);
        editBtn.setBackground(new Color(41, 128, 185));
        editBtn.setForeground(Color.WHITE);
        deleteBtn.setBackground(new Color(192, 57, 43));
        deleteBtn.setForeground(Color.WHITE);
        refreshBtn.setBackground(new Color(149, 165, 166));
        refreshBtn.setForeground(Color.WHITE);

        addBtn.addActionListener(e -> showAddEmployeeDialog(model));
        editBtn.addActionListener(e -> showEditEmployeeDialog(table, model));
        deleteBtn.addActionListener(e -> deleteEmployee(table, model));
        refreshBtn.addActionListener(e -> loadEmployeeData(model));

        buttonPanel.add(addBtn);
        buttonPanel.add(editBtn);
        buttonPanel.add(deleteBtn);
        buttonPanel.add(refreshBtn);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createTechnicianPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JLabel title = new JLabel("Support Technicians");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        panel.add(title, BorderLayout.NORTH);

        String[] columns = {"ID", "Name", "Specialization", "Contact No"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(model);
        table.setRowHeight(30);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        loadTechnicianData(model);

        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addBtn = new RoundedButton("Add Technician", 12);
        JButton editBtn = new RoundedButton("Edit Technician", 12);
        JButton deleteBtn = new RoundedButton("Delete Technician", 12);
        JButton refreshBtn = new RoundedButton("Refresh", 12);

        addBtn.setBackground(new Color(39, 174, 96));
        addBtn.setForeground(Color.WHITE);
        editBtn.setBackground(new Color(41, 128, 185));
        editBtn.setForeground(Color.WHITE);
        deleteBtn.setBackground(new Color(192, 57, 43));
        deleteBtn.setForeground(Color.WHITE);
        refreshBtn.setBackground(new Color(149, 165, 166));
        refreshBtn.setForeground(Color.WHITE);

        addBtn.addActionListener(e -> showAddTechnicianDialog(model));
        editBtn.addActionListener(e -> showEditTechnicianDialog(table, model));
        deleteBtn.addActionListener(e -> deleteTechnician(table, model));
        refreshBtn.addActionListener(e -> loadTechnicianData(model));

        buttonPanel.add(addBtn);
        buttonPanel.add(editBtn);
        buttonPanel.add(deleteBtn);
        buttonPanel.add(refreshBtn);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createMaintenancePanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JLabel title = new JLabel("Maintenance Service Queue");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        panel.add(title, BorderLayout.NORTH);

        // Queue Form
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setPreferredSize(new Dimension(350, 500));
        form.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Submit Ticket to Oracle"),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        JTextField assetIdField = new JTextField();
        JTextField empIdField = new JTextField();
        JComboBox<String> priorityCombo = new JComboBox<>(new String[]{"Low", "Medium", "High"});
        JTextArea issueArea = new JTextArea(6, 20);
        issueArea.setLineWrap(true);
        JButton submitBtn = new RoundedButton("Insert into JDBC Queue", 12);
        submitBtn.setBackground(new Color(41, 128, 185));
        submitBtn.setForeground(Color.WHITE);
        submitBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));

        form.add(new JLabel("Asset Reference ID:"));
        form.add(assetIdField);
        form.add(Box.createRigidArea(new Dimension(0, 15)));
        form.add(new JLabel("Reporting Employee (ID):"));
        form.add(empIdField);
        form.add(Box.createRigidArea(new Dimension(0, 15)));
        form.add(new JLabel("Priority:"));
        form.add(priorityCombo);
        form.add(Box.createRigidArea(new Dimension(0, 15)));
        form.add(new JLabel("Problem Details:"));
        form.add(new JScrollPane(issueArea));
        form.add(Box.createRigidArea(new Dimension(0, 25)));
        form.add(submitBtn);

        panel.add(form, BorderLayout.WEST);

        // Maintenance request list
        String[] columns = {"Request ID", "Asset ID", "Employee ID", "Technician ID", "Request Date", "Issue", "Status", "Completed Date", "Remarks", "Priority"};
        DefaultTableModel maintenanceModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable requestTable = new JTable(maintenanceModel);
        requestTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        requestTable.setAutoCreateRowSorter(true);
        loadMaintenanceRequests(maintenanceModel);

        panel.add(new JScrollPane(requestTable), BorderLayout.CENTER);

        JPanel editPanel = new JPanel(new BorderLayout(10, 10));
        editPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Update Maintenance Request"),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JPanel controlRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"Pending", "In Progress", "Complete"});
        JTextField techIdField = new JTextField(6);
        JButton updateBtn = new RoundedButton("Update Request", 12);
        JButton refreshBtn = new RoundedButton("Refresh List", 12);
        JButton exportBtn = new RoundedButton("Export to Excel", 12);

        controlRow.add(new JLabel("Status:"));
        controlRow.add(statusCombo);
        controlRow.add(new JLabel("Tech ID:"));
        controlRow.add(techIdField);
        controlRow.add(updateBtn);
        controlRow.add(refreshBtn);
        controlRow.add(exportBtn);

        JTextArea remarksArea = new JTextArea(4, 1);
        remarksArea.setLineWrap(true);
        remarksArea.setWrapStyleWord(true);
        JPanel remarksPanel = new JPanel(new BorderLayout());
        remarksPanel.setBorder(BorderFactory.createTitledBorder("Remarks"));
        remarksPanel.add(new JScrollPane(remarksArea), BorderLayout.CENTER);

        editPanel.add(controlRow, BorderLayout.NORTH);
        editPanel.add(remarksPanel, BorderLayout.CENTER);

        panel.add(editPanel, BorderLayout.SOUTH);

        requestTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && requestTable.getSelectedRow() != -1) {
                int selectedRow = requestTable.convertRowIndexToModel(requestTable.getSelectedRow());
                Object statusValue = maintenanceModel.getValueAt(selectedRow, 6);
                String status = statusValue != null ? statusValue.toString() : "Pending";
                if ("In_Progress".equals(status) || "In Progress".equals(status)) {
                    statusCombo.setSelectedItem("In Progress");
                } else if ("Completed".equals(status)) {
                    statusCombo.setSelectedItem("Complete");
                } else {
                    statusCombo.setSelectedItem("Pending");
                }

                Object techValue = maintenanceModel.getValueAt(selectedRow, 3);
                techIdField.setText(techValue == null ? "" : techValue.toString());
                Object remarksValue = maintenanceModel.getValueAt(selectedRow, 8);
                remarksArea.setText(remarksValue == null ? "" : remarksValue.toString());
            }
        });

        updateBtn.addActionListener(e -> {
            int selectedRow = requestTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Please select a maintenance request to update.", "No Request Selected", JOptionPane.WARNING_MESSAGE);
                return;
            }

            selectedRow = requestTable.convertRowIndexToModel(selectedRow);
            int requestId = Integer.parseInt(maintenanceModel.getValueAt(selectedRow, 0).toString());
            String selectedStatus = statusCombo.getSelectedItem().toString();
            String dbStatus = normalizeStatusForDatabase(selectedStatus);

            Integer techId = null;
            String techText = techIdField.getText().trim();
            if (!techText.isEmpty()) {
                try {
                    techId = Integer.parseInt(techText);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Technician ID must be a number.", "Invalid Tech ID", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            String remarks = remarksArea.getText().trim();

            try {
                boolean updated = updateMaintenanceRequestDetails(requestId, dbStatus, remarks, techId);
                if (updated) {
                    JOptionPane.showMessageDialog(this, "Request updated successfully.");
                    loadMaintenanceRequests(maintenanceModel);
                } else {
                    JOptionPane.showMessageDialog(this, "No records were updated. The request may have been removed.", "Update Failed", JOptionPane.WARNING_MESSAGE);
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error updating maintenance request: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        refreshBtn.addActionListener(e -> loadMaintenanceRequests(maintenanceModel));

        exportBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Export Maintenance Requests");
            chooser.setSelectedFile(new File("maintenance_requests.csv"));
            int option = chooser.showSaveDialog(this);
            if (option == JFileChooser.APPROVE_OPTION) {
                File selectedFile = chooser.getSelectedFile();
                if (!selectedFile.getName().toLowerCase().endsWith(".csv")) {
                    selectedFile = new File(selectedFile.getParentFile(), selectedFile.getName() + ".csv");
                }
                try {
                    exportMaintenanceRequestsToCsv(selectedFile);
                    JOptionPane.showMessageDialog(this, "Maintenance requests exported successfully to:\n" + selectedFile.getAbsolutePath(), "Export Complete", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        submitBtn.addActionListener(e -> {
            try (Connection conn = DBConnection.getConnection()) {
                String sql = "INSERT INTO MAINTENANCE_REQUEST (Request_ID, Asset_ID, Emp_ID, Tech_ID, Issue_Description, Priority, Status, Request_Date) " +
                             "VALUES (?, ?, ?, NULL, ?, ?, 'Pending', SYSDATE)";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                int reqId = (int) (System.currentTimeMillis() % 1000000);
                pstmt.setInt(1, reqId);
                pstmt.setInt(2, Integer.parseInt(assetIdField.getText()));
                pstmt.setInt(3, Integer.parseInt(empIdField.getText()));
                pstmt.setString(4, issueArea.getText());
                pstmt.setString(5, priorityCombo.getSelectedItem().toString());
                
                pstmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Success: JDBC Entry Created in Oracle");
                loadMaintenanceRequests(maintenanceModel);
                
                assetIdField.setText("");
                empIdField.setText("");
                issueArea.setText("");
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "JDBC System Error: " + ex.getMessage());
            }
        });

        return panel;
    }
    private JPanel createDepartmentPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JLabel title = new JLabel("Department Management");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        panel.add(title, BorderLayout.NORTH);

        String[] columns = {"ID", "Name", "Manager Name", "Manager ID"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(model);
        table.setRowHeight(30);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        loadDepartmentData(model);

        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addBtn = new RoundedButton("Add Department", 12);
        JButton editBtn = new RoundedButton("Edit Department", 12);
        JButton deleteBtn = new RoundedButton("Delete Department", 12);
        JButton refreshBtn = new RoundedButton("Refresh", 12);

        addBtn.setBackground(new Color(39, 174, 96));
        addBtn.setForeground(Color.WHITE);
        editBtn.setBackground(new Color(41, 128, 185));
        editBtn.setForeground(Color.WHITE);
        deleteBtn.setBackground(new Color(192, 57, 43));
        deleteBtn.setForeground(Color.WHITE);

        addBtn.addActionListener(e -> showAddDepartmentDialog(model));
        editBtn.addActionListener(e -> showEditDepartmentDialog(table, model));
        deleteBtn.addActionListener(e -> deleteDepartment(table, model));
        refreshBtn.addActionListener(e -> loadDepartmentData(model));

        buttonPanel.add(addBtn);
        buttonPanel.add(editBtn);
        buttonPanel.add(deleteBtn);
        buttonPanel.add(refreshBtn);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createDepartmentLocationPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JLabel title = new JLabel("Department Asset Locations");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        panel.add(title, BorderLayout.NORTH);

        String[] columns = {"Dept ID", "Dept Name", "Asset ID", "Asset Name", "Location ID", "Building", "Room"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(model);
        table.setRowHeight(30);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        loadDepartmentLocationData(model);

        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshBtn = new RoundedButton("Refresh", 12);
        refreshBtn.setBackground(new Color(41, 128, 185));
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.addActionListener(e -> loadDepartmentLocationData(model));
        buttonPanel.add(refreshBtn);

        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    private void loadDepartmentLocationData(DefaultTableModel model) {
        model.setRowCount(0);
        try (Connection conn = DBConnection.getConnection()) {
            String query = "SELECT d.Dept_ID, d.Dept_Name, a.Asset_ID, a.Asset_Name, l.Location_ID, l.Building_Name, l.Room_Number " +
                           "FROM ASSET a LEFT JOIN DEPARTMENT d ON a.Dept_ID = d.Dept_ID " +
                           "LEFT JOIN LOCATION l ON a.Location_ID = l.Location_ID " +
                           "ORDER BY d.Dept_ID, a.Asset_ID";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                Vector<String> row = new Vector<>();
                row.add(rs.getString("Dept_ID") != null ? rs.getString("Dept_ID") : "");
                row.add(rs.getString("Dept_Name") != null ? rs.getString("Dept_Name") : "");
                row.add(rs.getString("Asset_ID") != null ? rs.getString("Asset_ID") : "");
                row.add(rs.getString("Asset_Name") != null ? rs.getString("Asset_Name") : "");
                row.add(rs.getString("Location_ID") != null ? rs.getString("Location_ID") : "");
                row.add(rs.getString("Building_Name") != null ? rs.getString("Building_Name") : "");
                row.add(rs.getString("Room_Number") != null ? rs.getString("Room_Number") : "");
                model.addRow(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadAssetData(DefaultTableModel model) {
        model.setRowCount(0);
        try (Connection conn = DBConnection.getConnection()) {
            String query = "SELECT A.Asset_ID, A.Asset_Name, A.Asset_Type, A.Status, A.Purchase_Date, " +
                    "E.First_Name || ' ' || E.Last_Name AS Assigned_To, AA.Assign_Date " +
                    "FROM ASSET A " +
                    "LEFT JOIN ASSET_ASSIGNMENT AA ON A.Asset_ID = AA.Asset_ID AND AA.Return_Date IS NULL " +
                    "LEFT JOIN EMPLOYEE E ON AA.Emp_ID = E.Emp_ID " +
                    "ORDER BY A.Asset_ID";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                Vector<String> row = new Vector<>();
                row.add(rs.getString("Asset_ID"));
                row.add(rs.getString("Asset_Name"));
                row.add(rs.getString("Asset_Type"));
                row.add(rs.getString("Status"));
                row.add(String.valueOf(rs.getDate("Purchase_Date")));
                row.add(rs.getString("Assigned_To") != null ? rs.getString("Assigned_To") : "");
                row.add(rs.getString("Assign_Date") != null ? rs.getString("Assign_Date") : "");
                model.addRow(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadEmployeeData(DefaultTableModel model) {
        model.setRowCount(0);
        try (Connection conn = DBConnection.getConnection()) {
            String query = "SELECT Emp_ID, First_Name, Last_Name, Email, Phone, Dept_ID FROM EMPLOYEE";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                Vector<String> row = new Vector<>();
                row.add(rs.getString("Emp_ID"));
                row.add(rs.getString("First_Name"));
                row.add(rs.getString("Last_Name"));
                row.add(rs.getString("Email"));
                row.add(rs.getString("Phone") != null ? rs.getString("Phone") : "");
                row.add(rs.getString("Dept_ID"));
                model.addRow(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadTechnicianData(DefaultTableModel model) {
        model.setRowCount(0);
        try (Connection conn = DBConnection.getConnection()) {
            String query = "SELECT * FROM TECHNICIAN";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                Vector<String> row = new Vector<>();
                row.add(rs.getString("Tech_ID"));
                row.add(rs.getString("Tech_Name"));
                row.add(rs.getString("Specialization"));
                row.add(rs.getString("Phone"));
                model.addRow(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadMaintenanceQueue(DefaultListModel<String> listModel) {
        listModel.clear();
        try (Connection conn = DBConnection.getConnection()) {
            String query = "SELECT M.Request_ID, A.Asset_Name, M.Status " +
                         "FROM MAINTENANCE_REQUEST M JOIN ASSET A ON M.Asset_ID = A.Asset_ID " +
                         "ORDER BY M.Request_Date DESC";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                String entry = String.format("REQ #%s - [%s] - Asset: %s", 
                    rs.getString("Request_ID"), rs.getString("Status"), rs.getString("Asset_Name"));
                listModel.addElement(entry);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private int extractRequestIdFromQueueEntry(String entry) {
        if (entry == null || !entry.contains("#") || !entry.contains(" -")) {
            return -1;
        }
        try {
            int start = entry.indexOf('#') + 1;
            int end = entry.indexOf(" -", start);
            return Integer.parseInt(entry.substring(start, end).trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private boolean updateMaintenanceRequestStatus(int requestId, String status) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "UPDATE MAINTENANCE_REQUEST SET Status = ? WHERE Request_ID = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, status);
                pstmt.setInt(2, requestId);
                return pstmt.executeUpdate() > 0;
            }
        }
    }

    private void loadMaintenanceRequests(DefaultTableModel model) {
        model.setRowCount(0);
        try (Connection conn = DBConnection.getConnection()) {
            String query = "SELECT Request_ID, Asset_ID, Emp_ID, Tech_ID, TO_CHAR(Request_Date, 'YYYY-MM-DD') AS Request_Date, " +
                           "Issue_Description, Status, TO_CHAR(Completed_Date, 'YYYY-MM-DD') AS Completed_Date, Remarks, Priority " +
                           "FROM MAINTENANCE_REQUEST ORDER BY Request_Date DESC";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("Request_ID"));
                row.add(rs.getInt("Asset_ID"));
                row.add(rs.getInt("Emp_ID"));
                Object techValue = rs.getObject("Tech_ID");
                row.add(techValue != null ? techValue : "");
                row.add(rs.getString("Request_Date"));
                row.add(rs.getString("Issue_Description"));
                row.add(convertStatusForDisplay(rs.getString("Status")));
                row.add(rs.getString("Completed_Date"));
                row.add(rs.getString("Remarks"));
                row.add(rs.getString("Priority"));
                model.addRow(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String convertStatusForDisplay(String status) {
        if ("In_Progress".equals(status) || "In Progress".equals(status)) {
            return "In Progress";
        }
        return status != null ? status : "Pending";
    }

    private void exportMaintenanceRequestsToCsv(File file) throws SQLException, IOException {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT Request_ID, Asset_ID, Emp_ID, Tech_ID, TO_CHAR(Request_Date, 'YYYY-MM-DD') AS Request_Date, Issue_Description, Status, TO_CHAR(Completed_Date, 'YYYY-MM-DD') AS Completed_Date, Remarks, Priority FROM MAINTENANCE_REQUEST ORDER BY Request_Date DESC");
             BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("Request ID,Asset ID,Employee ID,Technician ID,Request Date,Issue,Status,Completed Date,Remarks,Priority");
            writer.newLine();
            while (rs.next()) {
                String requestId = String.valueOf(rs.getInt("Request_ID"));
                String assetId = String.valueOf(rs.getInt("Asset_ID"));
                String empId = String.valueOf(rs.getInt("Emp_ID"));
                Object techValue = rs.getObject("Tech_ID");
                String techId = techValue != null ? String.valueOf(techValue) : "";
                String requestDate = rs.getString("Request_Date");
                String issue = rs.getString("Issue_Description");
                String status = convertStatusForDisplay(rs.getString("Status"));
                String completedDate = rs.getString("Completed_Date");
                String remarks = rs.getString("Remarks");
                String priority = rs.getString("Priority");

                writer.write(escapeCsv(requestId) + "," + escapeCsv(assetId) + "," + escapeCsv(empId) + "," + escapeCsv(techId) + "," + escapeCsv(requestDate) + "," + escapeCsv(issue) + "," + escapeCsv(status) + "," + escapeCsv(completedDate) + "," + escapeCsv(remarks) + "," + escapeCsv(priority));
                writer.newLine();
            }
        }
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\r") || escaped.contains("\"")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private String normalizeStatusForDatabase(String selectedStatus) {
        if ("In Progress".equals(selectedStatus) || "In Process".equals(selectedStatus)) {
            return "In_Progress";
        }
        if ("Complete".equals(selectedStatus)) {
            return "Completed";
        }
        return "Pending";
    }

    private boolean updateMaintenanceRequestDetails(int requestId, String status, String remarks, Integer techId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "UPDATE MAINTENANCE_REQUEST SET Status = ?, Remarks = ?, Tech_ID = ?, " +
                         "Completed_Date = CASE WHEN ? = 'Completed' THEN SYSDATE ELSE NULL END " +
                         "WHERE Request_ID = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, status);
                if (remarks == null || remarks.isEmpty()) {
                    pstmt.setNull(2, Types.VARCHAR);
                } else {
                    pstmt.setString(2, remarks);
                }
                if (techId == null) {
                    pstmt.setNull(3, Types.INTEGER);
                } else {
                    pstmt.setInt(3, techId);
                }
                pstmt.setString(4, status);
                pstmt.setInt(5, requestId);
                return pstmt.executeUpdate() > 0;
            }
        }
    }

    private static class StatusBarChart extends JPanel {
        private final int pending;
        private final int inProgress;
        private final int completed;

        public StatusBarChart(int pending, int inProgress, int completed) {
            this.pending = pending;
            this.inProgress = inProgress;
            this.completed = completed;
            setPreferredSize(new Dimension(780, 240));
            setBackground(Color.WHITE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int total = Math.max(1, pending + inProgress + completed);
            int width = getWidth() - 80;
            int height = getHeight() - 100;
            int startX = 40;
            int startY = 40;

            int pendingBar = Math.round(width * (pending / (float) total));
            int inProgressBar = Math.round(width * (inProgress / (float) total));
            int completedBar = width - pendingBar - inProgressBar;

            int barHeight = 40;
            int y = startY;

            g2.setColor(new Color(243, 156, 18));
            g2.fillRoundRect(startX, y, pendingBar, barHeight, 20, 20);
            g2.setColor(new Color(231, 76, 60));
            g2.fillRoundRect(startX + pendingBar, y, inProgressBar, barHeight, 20, 20);
            g2.setColor(new Color(39, 174, 96));
            g2.fillRoundRect(startX + pendingBar + inProgressBar, y, completedBar, barHeight, 20, 20);

            g2.setColor(new Color(44, 62, 80));
            g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
            g2.drawString("Pending: " + pending, startX, y + barHeight + 25);
            g2.drawString("In Progress: " + inProgress, startX + 220, y + barHeight + 25);
            g2.drawString("Completed: " + completed, startX + 440, y + barHeight + 25);

            g2.setColor(new Color(189, 195, 199));
            int lineY = y + barHeight + 40;
            g2.drawLine(startX, lineY, startX + width, lineY);

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            g2.drawString("Data is pulled live from the database.", startX, lineY + 20);
        }
    }

    private void showAddAssetDialog(DefaultTableModel model) {
        JDialog dialog = new JDialog(this, "Add New Asset", true);
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(this);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        JTextField idField = new JTextField(20);
        JTextField nameField = new JTextField(20);
        JTextField typeField = new JTextField(20);
        JTextField statusField = new JTextField("Available", 20);
        JTextField purchaseDateField = new JTextField("YYYY-MM-DD", 20);
        JComboBox<String> deptCombo = new JComboBox<>();
        JComboBox<String> locationCombo = new JComboBox<>();

        deptCombo.addItem("None");
        locationCombo.addItem("None");
        for (String deptOption : loadDepartmentOptions()) {
            deptCombo.addItem(deptOption);
        }
        for (String locOption : loadLocationOptions()) {
            locationCombo.addItem(locOption);
        }

        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Asset ID:"), gbc);
        gbc.gridx = 1;
        formPanel.add(idField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new JLabel("Asset Name:"), gbc);
        gbc.gridx = 1;
        formPanel.add(nameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(new JLabel("Asset Type:"), gbc);
        gbc.gridx = 1;
        formPanel.add(typeField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        formPanel.add(new JLabel("Status:"), gbc);
        gbc.gridx = 1;
        formPanel.add(statusField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        formPanel.add(new JLabel("Purchase Date:"), gbc);
        gbc.gridx = 1;
        formPanel.add(purchaseDateField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        formPanel.add(new JLabel("Department:"), gbc);
        gbc.gridx = 1;
        formPanel.add(deptCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 6;
        formPanel.add(new JLabel("Location:"), gbc);
        gbc.gridx = 1;
        formPanel.add(locationCombo, gbc);

        JButton saveBtn = new RoundedButton("Save", 12);
        JButton cancelBtn = new RoundedButton("Cancel", 12);
        saveBtn.setBackground(new Color(39, 174, 96));
        saveBtn.setForeground(Color.WHITE);
        cancelBtn.setBackground(new Color(192, 57, 43));
        cancelBtn.setForeground(Color.WHITE);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);

        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        saveBtn.addActionListener(e -> {
            try {
                Integer deptId = deptCombo.getSelectedIndex() > 0 ? Integer.parseInt(((String) deptCombo.getSelectedItem()).split(" - ")[0]) : null;
                Integer locationId = locationCombo.getSelectedIndex() > 0 ? Integer.parseInt(((String) locationCombo.getSelectedItem()).split(" - ")[0]) : null;
                insertAsset(Integer.parseInt(idField.getText()), nameField.getText(), typeField.getText(),
                           statusField.getText(), purchaseDateField.getText(), deptId, locationId);
                loadAssetData(model);
                dialog.dispose();
                JOptionPane.showMessageDialog(this, "Asset added successfully!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        cancelBtn.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    private void showEditAssetDialog(JTable table, DefaultTableModel model) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an asset to edit.");
            return;
        }

        JDialog dialog = new JDialog(this, "Edit Asset", true);
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(this);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        JTextField idField = new JTextField(model.getValueAt(selectedRow, 0).toString(), 20);
        idField.setEditable(false);
        JTextField nameField = new JTextField(model.getValueAt(selectedRow, 1).toString(), 20);
        JTextField typeField = new JTextField(model.getValueAt(selectedRow, 2).toString(), 20);
        JTextField statusField = new JTextField(model.getValueAt(selectedRow, 3).toString(), 20);
        JTextField purchaseDateField = new JTextField(model.getValueAt(selectedRow, 4).toString(), 20);
        JComboBox<String> deptCombo = new JComboBox<>();
        JComboBox<String> locationCombo = new JComboBox<>();

        deptCombo.addItem("None");
        locationCombo.addItem("None");
        for (String deptOption : loadDepartmentOptions()) {
            deptCombo.addItem(deptOption);
        }
        for (String locOption : loadLocationOptions()) {
            locationCombo.addItem(locOption);
        }

        Integer currentDeptId = null;
        Integer currentLocationId = null;
        try (Connection conn = DBConnection.getConnection()) {
            String query = "SELECT Dept_ID, Location_ID FROM ASSET WHERE Asset_ID = ?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, Integer.parseInt(idField.getText()));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                currentDeptId = rs.getObject("Dept_ID") != null ? rs.getInt("Dept_ID") : null;
                currentLocationId = rs.getObject("Location_ID") != null ? rs.getInt("Location_ID") : null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (currentDeptId != null) {
            String currentDept = currentDeptId + " - " + getDepartmentName(currentDeptId);
            deptCombo.setSelectedItem(currentDept);
        }
        if (currentLocationId != null) {
            String currentLoc = currentLocationId + " - " + getLocationName(currentLocationId);
            locationCombo.setSelectedItem(currentLoc);
        }

        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Asset ID:"), gbc);
        gbc.gridx = 1;
        formPanel.add(idField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new JLabel("Asset Name:"), gbc);
        gbc.gridx = 1;
        formPanel.add(nameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(new JLabel("Asset Type:"), gbc);
        gbc.gridx = 1;
        formPanel.add(typeField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        formPanel.add(new JLabel("Status:"), gbc);
        gbc.gridx = 1;
        formPanel.add(statusField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        formPanel.add(new JLabel("Purchase Date:"), gbc);
        gbc.gridx = 1;
        formPanel.add(purchaseDateField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        formPanel.add(new JLabel("Department:"), gbc);
        gbc.gridx = 1;
        formPanel.add(deptCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 6;
        formPanel.add(new JLabel("Location:"), gbc);
        gbc.gridx = 1;
        formPanel.add(locationCombo, gbc);

        JButton saveBtn = new JButton("Update");
        JButton cancelBtn = new JButton("Cancel");

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);

        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        saveBtn.addActionListener(e -> {
            try {
                Integer deptId = deptCombo.getSelectedIndex() > 0 ? Integer.parseInt(((String) deptCombo.getSelectedItem()).split(" - ")[0]) : null;
                Integer locationId = locationCombo.getSelectedIndex() > 0 ? Integer.parseInt(((String) locationCombo.getSelectedItem()).split(" - ")[0]) : null;
                updateAsset(Integer.parseInt(idField.getText()), nameField.getText(), typeField.getText(),
                           statusField.getText(), purchaseDateField.getText(), deptId, locationId);
                loadAssetData(model);
                dialog.dispose();
                JOptionPane.showMessageDialog(this, "Asset updated successfully!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        cancelBtn.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    private void deleteAsset(JTable table, DefaultTableModel model) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an asset to delete.");
            return;
        }

        int assetId = Integer.parseInt(model.getValueAt(selectedRow, 0).toString());
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this asset?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                deleteAssetFromDB(assetId);
                loadAssetData(model);
                JOptionPane.showMessageDialog(this, "Asset deleted successfully!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        }
    }

    private void insertAsset(int id, String name, String type, String status, String purchaseDate, Integer deptId, Integer locationId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "INSERT INTO ASSET (Asset_ID, Asset_Name, Asset_Type, Purchase_Date, Status, Dept_ID, Location_ID) VALUES (?, ?, ?, TO_DATE(?, 'YYYY-MM-DD'), ?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ps.setString(2, name);
            ps.setString(3, type);
            ps.setString(4, purchaseDate);
            ps.setString(5, status);
            if (deptId != null) ps.setInt(6, deptId);
            else ps.setNull(6, java.sql.Types.INTEGER);
            if (locationId != null) ps.setInt(7, locationId);
            else ps.setNull(7, java.sql.Types.INTEGER);
            ps.executeUpdate();
        }
    }

    private void updateAsset(int id, String name, String type, String status, String purchaseDate, Integer deptId, Integer locationId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "UPDATE ASSET SET Asset_Name=?, Asset_Type=?, Purchase_Date=TO_DATE(?, 'YYYY-MM-DD'), Status=?, Dept_ID=?, Location_ID=? WHERE Asset_ID=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, name);
            ps.setString(2, type);
            ps.setString(3, purchaseDate);
            ps.setString(4, status);
            if (deptId != null) ps.setInt(5, deptId);
            else ps.setNull(5, java.sql.Types.INTEGER);
            if (locationId != null) ps.setInt(6, locationId);
            else ps.setNull(6, java.sql.Types.INTEGER);
            ps.setInt(7, id);
            ps.executeUpdate();
        }
    }

    private void deleteAssetFromDB(int id) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "DELETE FROM ASSET WHERE Asset_ID=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private List<String> loadDepartmentOptions() {
        List<String> options = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT Dept_ID, Dept_Name FROM DEPARTMENT ORDER BY Dept_ID";
            try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    options.add(rs.getInt("Dept_ID") + " - " + rs.getString("Dept_Name"));
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return options;
    }

    private List<String> loadEmployeeOptions() {
        List<String> options = new ArrayList<>();
        options.add("None");
        try {
            List<Employee> employees = userService.getAllEmployees();
            for (Employee emp : employees) {
                options.add(emp.getEmpId() + " - " + emp.getFullName());
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return options;
    }

    private List<String> loadLocationOptions() {
        List<String> options = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT Location_ID, Building_Name, Room_Number FROM LOCATION ORDER BY Location_ID";
            try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String room = rs.getString("Room_Number") != null ? " (" + rs.getString("Room_Number") + ")" : "";
                    options.add(rs.getInt("Location_ID") + " - " + rs.getString("Building_Name") + room);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return options;
    }

    private String getDepartmentName(int deptId) {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT Dept_Name FROM DEPARTMENT WHERE Dept_ID = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, deptId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("Dept_Name");
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return "";
    }

    private String getLocationName(int locationId) {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT Building_Name, Room_Number FROM LOCATION WHERE Location_ID = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, locationId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String room = rs.getString("Room_Number") != null ? " (" + rs.getString("Room_Number") + ")" : "";
                        return rs.getString("Building_Name") + room;
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return "";
    }

    private void showAssignAssetDialog(JTable table, DefaultTableModel model) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an asset to assign.");
            return;
        }

        int assetId = Integer.parseInt(model.getValueAt(selectedRow, 0).toString());

        JDialog dialog = new JDialog(this, "Assign Asset", true);
        dialog.setSize(400, 200);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel lblAssetId = new JLabel("Asset ID:");
        JTextField assetIdField = new JTextField(String.valueOf(assetId));
        assetIdField.setEditable(false);

        JLabel lblEmployee = new JLabel("Select Employee:");
        JComboBox<String> employeeCombo = new JComboBox<>();

        // Load employees
        int currentUserIndex = 0;
        try {
            List<Employee> employees = userService.getAllEmployees();
            for (int i = 0; i < employees.size(); i++) {
                Employee emp = employees.get(i);
                employeeCombo.addItem(emp.getEmpId() + " - " + emp.getFullName());
                // Set default to current user
                if (emp.getEmpId() == currentUser.getEmpId()) {
                    currentUserIndex = i;
                }
            }
            employeeCombo.setSelectedIndex(currentUserIndex);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error loading employees: " + ex.getMessage());
        }

        panel.add(lblAssetId);
        panel.add(assetIdField);
        panel.add(lblEmployee);
        panel.add(employeeCombo);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton assignBtn = new RoundedButton("Assign", 12);
        JButton cancelBtn = new RoundedButton("Cancel", 12);

        assignBtn.addActionListener(e -> {
            try {
                String selected = (String) employeeCombo.getSelectedItem();
                int empId = Integer.parseInt(selected.split(" - ")[0]);
                assetService.assignAsset(assetId, empId);
                loadAssetData(model);
                dialog.dispose();
                JOptionPane.showMessageDialog(this, "Asset assigned successfully!");
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? "Unknown error" : ex.getMessage();
                JOptionPane.showMessageDialog(this, msg);
            }
        });

        cancelBtn.addActionListener(e -> dialog.dispose());

        buttonPanel.add(assignBtn);
        buttonPanel.add(cancelBtn);

        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void showAddEmployeeDialog(DefaultTableModel model) {
        JDialog dialog = new JDialog(this, "Add New Employee", true);
        dialog.setSize(400, 350);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridLayout(7, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JTextField idField = new JTextField();
        JTextField firstNameField = new JTextField();
        JTextField lastNameField = new JTextField();
        JTextField emailField = new JTextField();
        JTextField phoneField = new JTextField();
        JTextField deptIdField = new JTextField();

        panel.add(new JLabel("Employee ID:"));
        panel.add(idField);
        panel.add(new JLabel("First Name:"));
        panel.add(firstNameField);
        panel.add(new JLabel("Last Name:"));
        panel.add(lastNameField);
        panel.add(new JLabel("Email:"));
        panel.add(emailField);
        panel.add(new JLabel("Phone:"));
        panel.add(phoneField);
        panel.add(new JLabel("Dept ID:"));
        panel.add(deptIdField);

        JButton saveBtn = new JButton("Save");
        JButton cancelBtn = new JButton("Cancel");

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);

        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        saveBtn.addActionListener(e -> {
            try {
                insertEmployee(Integer.parseInt(idField.getText()), firstNameField.getText(), lastNameField.getText(),
                             emailField.getText(), phoneField.getText(), Integer.parseInt(deptIdField.getText()));
                loadEmployeeData(model);
                dialog.dispose();
                JOptionPane.showMessageDialog(this, "Employee added successfully!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        cancelBtn.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    private void showEditEmployeeDialog(JTable table, DefaultTableModel model) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an employee to edit.");
            return;
        }

        JDialog dialog = new JDialog(this, "Edit Employee", true);
        dialog.setSize(400, 350);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridLayout(7, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JTextField idField = new JTextField(model.getValueAt(selectedRow, 0).toString());
        idField.setEditable(false);
        JTextField firstNameField = new JTextField(model.getValueAt(selectedRow, 1).toString());
        JTextField lastNameField = new JTextField(model.getValueAt(selectedRow, 2).toString());
        JTextField emailField = new JTextField(model.getValueAt(selectedRow, 3).toString());
        JTextField phoneField = new JTextField();
        JTextField deptIdField = new JTextField(model.getValueAt(selectedRow, 4).toString());

        // Load current phone
        try (Connection conn = DBConnection.getConnection()) {
            String query = "SELECT Phone FROM EMPLOYEE WHERE Emp_ID = ?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, Integer.parseInt(idField.getText()));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                phoneField.setText(rs.getString("Phone"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        panel.add(new JLabel("Employee ID:"));
        panel.add(idField);
        panel.add(new JLabel("First Name:"));
        panel.add(firstNameField);
        panel.add(new JLabel("Last Name:"));
        panel.add(lastNameField);
        panel.add(new JLabel("Email:"));
        panel.add(emailField);
        panel.add(new JLabel("Phone:"));
        panel.add(phoneField);
        panel.add(new JLabel("Dept ID:"));
        panel.add(deptIdField);

        JButton saveBtn = new JButton("Update");
        JButton cancelBtn = new JButton("Cancel");

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);

        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        saveBtn.addActionListener(e -> {
            try {
                updateEmployee(Integer.parseInt(idField.getText()), firstNameField.getText(), lastNameField.getText(),
                             emailField.getText(), phoneField.getText(), Integer.parseInt(deptIdField.getText()));
                loadEmployeeData(model);
                dialog.dispose();
                JOptionPane.showMessageDialog(this, "Employee updated successfully!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        cancelBtn.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    private void deleteEmployee(JTable table, DefaultTableModel model) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an employee to delete.");
            return;
        }

        int empId = Integer.parseInt(model.getValueAt(selectedRow, 0).toString());
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this employee?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                deleteEmployeeFromDB(empId);
                loadEmployeeData(model);
                JOptionPane.showMessageDialog(this, "Employee deleted successfully!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        }
    }

    private void insertEmployee(int id, String firstName, String lastName, String email, String phone, int deptId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "INSERT INTO EMPLOYEE (Emp_ID, First_Name, Last_Name, Email, Phone, Dept_ID) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ps.setString(2, firstName);
            ps.setString(3, lastName);
            ps.setString(4, email);
            ps.setString(5, phone);
            ps.setInt(6, deptId);
            ps.executeUpdate();
        }
    }

    private void updateEmployee(int id, String firstName, String lastName, String email, String phone, int deptId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "UPDATE EMPLOYEE SET First_Name=?, Last_Name=?, Email=?, Phone=?, Dept_ID=? WHERE Emp_ID=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, firstName);
            ps.setString(2, lastName);
            ps.setString(3, email);
            ps.setString(4, phone);
            ps.setInt(5, deptId);
            ps.setInt(6, id);
            ps.executeUpdate();
        }
    }

    private void deleteEmployeeFromDB(int id) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "DELETE FROM EMPLOYEE WHERE Emp_ID=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private void showAddTechnicianDialog(DefaultTableModel model) {
        JDialog dialog = new JDialog(this, "Add New Technician", true);
        dialog.setSize(400, 250);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JTextField idField = new JTextField();
        JTextField nameField = new JTextField();
        JTextField specializationField = new JTextField();
        JTextField phoneField = new JTextField();

        panel.add(new JLabel("Technician ID:"));
        panel.add(idField);
        panel.add(new JLabel("Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Specialization:"));
        panel.add(specializationField);
        panel.add(new JLabel("Phone:"));
        panel.add(phoneField);

        JButton saveBtn = new JButton("Save");
        JButton cancelBtn = new JButton("Cancel");

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);

        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        saveBtn.addActionListener(e -> {
            try {
                insertTechnician(Integer.parseInt(idField.getText()), nameField.getText(),
                               specializationField.getText(), phoneField.getText());
                loadTechnicianData(model);
                dialog.dispose();
                JOptionPane.showMessageDialog(this, "Technician added successfully!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        cancelBtn.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    private void showEditTechnicianDialog(JTable table, DefaultTableModel model) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a technician to edit.");
            return;
        }

        JDialog dialog = new JDialog(this, "Edit Technician", true);
        dialog.setSize(400, 250);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JTextField idField = new JTextField(model.getValueAt(selectedRow, 0).toString());
        idField.setEditable(false);
        JTextField nameField = new JTextField(model.getValueAt(selectedRow, 1).toString());
        JTextField specializationField = new JTextField(model.getValueAt(selectedRow, 2).toString());
        JTextField phoneField = new JTextField(model.getValueAt(selectedRow, 3).toString());

        panel.add(new JLabel("Technician ID:"));
        panel.add(idField);
        panel.add(new JLabel("Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Specialization:"));
        panel.add(specializationField);
        panel.add(new JLabel("Phone:"));
        panel.add(phoneField);

        JButton saveBtn = new JButton("Update");
        JButton cancelBtn = new JButton("Cancel");

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);

        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        saveBtn.addActionListener(e -> {
            try {
                updateTechnician(Integer.parseInt(idField.getText()), nameField.getText(),
                               specializationField.getText(), phoneField.getText());
                loadTechnicianData(model);
                dialog.dispose();
                JOptionPane.showMessageDialog(this, "Technician updated successfully!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        cancelBtn.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    private void deleteTechnician(JTable table, DefaultTableModel model) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a technician to delete.");
            return;
        }

        int techId = Integer.parseInt(model.getValueAt(selectedRow, 0).toString());
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this technician?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                deleteTechnicianFromDB(techId);
                loadTechnicianData(model);
                JOptionPane.showMessageDialog(this, "Technician deleted successfully!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        }
    }

    private void insertTechnician(int id, String name, String specialization, String phone) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "INSERT INTO TECHNICIAN (Tech_ID, Tech_Name, Specialization, Phone) VALUES (?, ?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ps.setString(2, name);
            ps.setString(3, specialization);
            ps.setString(4, phone);
            ps.executeUpdate();
        }
    }

    private void updateTechnician(int id, String name, String specialization, String phone) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "UPDATE TECHNICIAN SET Tech_Name=?, Specialization=?, Phone=? WHERE Tech_ID=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, name);
            ps.setString(2, specialization);
            ps.setString(3, phone);
            ps.setInt(4, id);
            ps.executeUpdate();
        }
    }

    private void deleteTechnicianFromDB(int id) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "DELETE FROM TECHNICIAN WHERE Tech_ID=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private void loadDepartmentData(DefaultTableModel model) {
        model.setRowCount(0);
        try (Connection conn = DBConnection.getConnection()) {
            boolean hasManagerName = hasDepartmentManagerNameColumn(conn);
            String query;
            if (hasManagerName) {
                query = "SELECT Dept_ID, Dept_Name, Manager_Name, Manager_ID FROM DEPARTMENT ORDER BY Dept_ID";
            } else {
                query = "SELECT D.Dept_ID, D.Dept_Name, E.First_Name || ' ' || E.Last_Name AS Manager_Name, D.Manager_ID " +
                        "FROM DEPARTMENT D LEFT JOIN EMPLOYEE E ON D.Manager_ID = E.Emp_ID ORDER BY D.Dept_ID";
            }
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                Vector<String> row = new Vector<>();
                row.add(rs.getString("Dept_ID"));
                row.add(rs.getString("Dept_Name"));
                row.add(rs.getString("Manager_Name") != null ? rs.getString("Manager_Name") : "");
                row.add(rs.getString("Manager_ID") != null ? rs.getString("Manager_ID") : "");
                model.addRow(row);
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showAddDepartmentDialog(DefaultTableModel model) {
        JDialog dialog = new JDialog(this, "Add New Department", true);
        dialog.setSize(400, 200);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JTextField idField = new JTextField();
        JTextField nameField = new JTextField();
        JComboBox<String> managerCombo = new JComboBox<>();

        for (String option : loadEmployeeOptions()) {
            managerCombo.addItem(option);
        }

        panel.add(new JLabel("Department ID:"));
        panel.add(idField);
        panel.add(new JLabel("Department Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Manager:"));
        panel.add(managerCombo);

        JButton saveBtn = new JButton("Save");
        JButton cancelBtn = new JButton("Cancel");

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);

        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        saveBtn.addActionListener(e -> {
            try {
                String selectedManager = (String) managerCombo.getSelectedItem();
                String managerIdText = "";
                if (selectedManager != null && !selectedManager.equals("None")) {
                    managerIdText = selectedManager.split(" - ")[0].trim();
                }
                insertDepartment(Integer.parseInt(idField.getText()), nameField.getText(), managerIdText);
                loadDepartmentData(model);
                dialog.dispose();
                JOptionPane.showMessageDialog(this, "Department added successfully!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        cancelBtn.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    private void showEditDepartmentDialog(JTable table, DefaultTableModel model) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a department to edit.");
            return;
        }

        JDialog dialog = new JDialog(this, "Edit Department", true);
        dialog.setSize(400, 200);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JTextField idField = new JTextField(model.getValueAt(selectedRow, 0).toString());
        idField.setEditable(false);
        JTextField nameField = new JTextField(model.getValueAt(selectedRow, 1).toString());
        JComboBox<String> managerCombo = new JComboBox<>();
        String currentManagerName = model.getValueAt(selectedRow, 2).toString();
        String currentManagerId = model.getValueAt(selectedRow, 3).toString();

        for (String option : loadEmployeeOptions()) {
            managerCombo.addItem(option);
        }
        if (currentManagerId == null || currentManagerId.trim().isEmpty()) {
            managerCombo.setSelectedItem("None");
        } else {
            managerCombo.setSelectedItem(currentManagerId + " - " + currentManagerName);
        }

        panel.add(new JLabel("Department ID:"));
        panel.add(idField);
        panel.add(new JLabel("Department Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Manager:"));
        panel.add(managerCombo);

        JButton saveBtn = new JButton("Update");
        JButton cancelBtn = new JButton("Cancel");

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);

        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        saveBtn.addActionListener(e -> {
            try {
                String selectedManager = (String) managerCombo.getSelectedItem();
                String managerIdText = "";
                if (selectedManager != null && !selectedManager.equals("None")) {
                    managerIdText = selectedManager.split(" - ")[0].trim();
                }
                updateDepartment(Integer.parseInt(idField.getText()), nameField.getText(), managerIdText);
                loadDepartmentData(model);
                dialog.dispose();
                JOptionPane.showMessageDialog(this, "Department updated successfully!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        cancelBtn.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    private void deleteDepartment(JTable table, DefaultTableModel model) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a department to delete.");
            return;
        }

        int deptId = Integer.parseInt(model.getValueAt(selectedRow, 0).toString());
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this department?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                deleteDepartmentFromDB(deptId);
                loadDepartmentData(model);
                JOptionPane.showMessageDialog(this, "Department deleted successfully!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        }
    }

    private void insertDepartment(int id, String name, String managerIdText) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            boolean hasManagerName = hasDepartmentManagerNameColumn(conn);
            String sql = hasManagerName
                    ? "INSERT INTO DEPARTMENT (Dept_ID, Dept_Name, Manager_ID, Manager_Name) VALUES (?, ?, ?, ?)"
                    : "INSERT INTO DEPARTMENT (Dept_ID, Dept_Name, Manager_ID) VALUES (?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ps.setString(2, name);
            Integer managerId = getValidatedManagerId(managerIdText);
            if (managerId == null) {
                ps.setNull(3, Types.NUMERIC);
            } else {
                ps.setInt(3, managerId);
            }
            if (hasManagerName) {
                String managerName = managerId == null ? null : getEmployeeFullName(managerId);
                if (managerName == null) {
                    ps.setNull(4, Types.VARCHAR);
                } else {
                    ps.setString(4, managerName);
                }
            }
            ps.executeUpdate();
        }
    }

    private void updateDepartment(int id, String name, String managerIdText) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            boolean hasManagerName = hasDepartmentManagerNameColumn(conn);
            String sql = hasManagerName
                    ? "UPDATE DEPARTMENT SET Dept_Name=?, Manager_ID=?, Manager_Name=? WHERE Dept_ID=?"
                    : "UPDATE DEPARTMENT SET Dept_Name=?, Manager_ID=? WHERE Dept_ID=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, name);
            Integer managerId = getValidatedManagerId(managerIdText);
            if (managerId == null) {
                ps.setNull(2, Types.NUMERIC);
            } else {
                ps.setInt(2, managerId);
            }
            if (hasManagerName) {
                String managerName = managerId == null ? null : getEmployeeFullName(managerId);
                if (managerName == null) {
                    ps.setNull(3, Types.VARCHAR);
                } else {
                    ps.setString(3, managerName);
                }
                ps.setInt(4, id);
            } else {
                ps.setInt(3, id);
            }
            ps.executeUpdate();
        }
    }

    private Integer getValidatedManagerId(String managerIdText) throws SQLException {
        if (managerIdText == null || managerIdText.trim().isEmpty()) {
            return null;
        }

        int managerId;
        try {
            managerId = Integer.parseInt(managerIdText.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Manager ID must be a valid numeric employee ID.");
        }

        if (userService.getEmployeeById(managerId) == null) {
            throw new IllegalArgumentException("Manager ID " + managerId + " does not exist.");
        }

        return managerId;
    }

    private boolean hasDepartmentManagerNameColumn(Connection conn) throws SQLException {
        if (departmentHasManagerNameColumn != null) {
            return departmentHasManagerNameColumn;
        }
        try (ResultSet rs = conn.getMetaData().getColumns(null, null, "DEPARTMENT", "MANAGER_NAME")) {
            departmentHasManagerNameColumn = rs.next();
        }
        return departmentHasManagerNameColumn;
    }

    private String getEmployeeFullName(int empId) throws SQLException {
        Employee employee = userService.getEmployeeById(empId);
        return employee == null ? null : employee.getFullName();
    }

    private void deleteDepartmentFromDB(int id) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "DELETE FROM DEPARTMENT WHERE Dept_ID=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // Role-based panels
    private JPanel createEmployeeMaintenancePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Request Maintenance");
        title.setFont(new Font("Arial", Font.BOLD, 18));
        panel.add(title, BorderLayout.NORTH);

        JPanel content = new JPanel(new GridLayout(2, 1, 10, 10));

        // Request form
        JPanel formPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createTitledBorder("New Maintenance Request"));

        JComboBox<String> assetCombo = new JComboBox<>();
        JTextArea issueArea = new JTextArea(3, 20);
        JComboBox<String> priorityCombo = new JComboBox<>(new String[]{"Low", "Medium", "High", "Critical"});

        // Load user's assigned assets
        try {
            List<Asset> myAssets = userService.getMyAssignedAssets();
            for (Asset asset : myAssets) {
                assetCombo.addItem(asset.getAssetId() + " - " + asset.getAssetName());
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading assets: " + e.getMessage());
        }

        formPanel.add(new JLabel("Asset:"));
        formPanel.add(assetCombo);
        formPanel.add(new JLabel("Issue Description:"));
        formPanel.add(new JScrollPane(issueArea));
        formPanel.add(new JLabel("Priority:"));
        formPanel.add(priorityCombo);
        formPanel.add(new JLabel(""));
        JButton submitBtn = new JButton("Submit Request");
        formPanel.add(submitBtn);

        submitBtn.addActionListener(e -> {
            try {
                if (assetCombo.getSelectedItem() == null) {
                    JOptionPane.showMessageDialog(this, "Please select an asset");
                    return;
                }

                String assetItem = assetCombo.getSelectedItem().toString();
                int assetId = Integer.parseInt(assetItem.split(" - ")[0]);
                String issue = issueArea.getText().trim();
                String priority = priorityCombo.getSelectedItem().toString();

                if (issue.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Please describe the issue");
                    return;
                }

                assetService.createMaintenanceRequest(assetId, currentUser.getEmpId(), issue, priority);
                JOptionPane.showMessageDialog(this, "Maintenance request submitted successfully!");
                issueArea.setText("");
                cardLayout.show(contentPanel, "Dashboard"); // Refresh dashboard

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        content.add(formPanel);

        // My requests table
        JPanel requestsPanel = new JPanel(new BorderLayout());
        requestsPanel.setBorder(BorderFactory.createTitledBorder("My Maintenance Requests"));

        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("Request ID");
        model.addColumn("Asset");
        model.addColumn("Issue");
        model.addColumn("Status");
        model.addColumn("Date");

        JTable table = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(table);
        requestsPanel.add(scrollPane, BorderLayout.CENTER);

        // Load user's maintenance requests
        try {
            List<MaintenanceRequest> requests = userService.getMyMaintenanceRequests();
            for (MaintenanceRequest req : requests) {
                // Get asset name
                String assetName = "Unknown";
                try (Connection conn = DBConnection.getConnection()) {
                    String sql = "SELECT Asset_Name FROM ASSET WHERE Asset_ID = ?";
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setInt(1, req.getAssetId());
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        assetName = rs.getString("Asset_Name");
                    }
                }
                model.addRow(new Object[]{
                    req.getRequestId(),
                    assetName,
                    req.getIssueDescription(),
                    req.getStatus(),
                    req.getRequestDate()
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading requests: " + e.getMessage());
        }

        content.add(requestsPanel);
        panel.add(content, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createTechnicianMaintenancePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("My Maintenance Tasks");
        title.setFont(new Font("Arial", Font.BOLD, 18));
        panel.add(title, BorderLayout.NORTH);

        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("Request ID");
        model.addColumn("Asset");
        model.addColumn("Employee");
        model.addColumn("Issue");
        model.addColumn("Status");
        model.addColumn("Date");

        JTable table = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Action buttons
        JPanel buttonPanel = new JPanel();
        JButton startBtn = new JButton("Begin Maintenance");
        JButton completeBtn = new JButton("Mark as Completed");
        JButton refreshBtn = new JButton("Refresh List");

        buttonPanel.add(startBtn);
        buttonPanel.add(completeBtn);
        buttonPanel.add(refreshBtn);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        // Load assigned maintenance requests
        Runnable loadData = () -> {
            model.setRowCount(0);
            try {
                List<MaintenanceRequest> requests = userService.getAssignedMaintenanceRequests();
                for (MaintenanceRequest req : requests) {
                    // Get asset and employee names
                    String assetName = "Unknown";
                    String employeeName = "Unknown";
                    try (Connection conn = DBConnection.getConnection()) {
                        // Get asset name
                        String sql = "SELECT Asset_Name FROM ASSET WHERE Asset_ID = ?";
                        PreparedStatement ps = conn.prepareStatement(sql);
                        ps.setInt(1, req.getAssetId());
                        ResultSet rs = ps.executeQuery();
                        if (rs.next()) {
                            assetName = rs.getString("Asset_Name");
                        }

                        // Get employee name
                        sql = "SELECT First_Name, Last_Name FROM EMPLOYEE WHERE Emp_ID = ?";
                        ps = conn.prepareStatement(sql);
                        ps.setInt(1, req.getEmpId());
                        rs = ps.executeQuery();
                        if (rs.next()) {
                            employeeName = rs.getString("First_Name") + " " + rs.getString("Last_Name");
                        }
                    }
                    model.addRow(new Object[]{
                        req.getRequestId(),
                        assetName,
                        employeeName,
                        req.getIssueDescription(),
                        req.getStatus(),
                        req.getRequestDate()
                    });
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error loading tasks: " + e.getMessage());
            }
        };

        loadData.run();

        startBtn.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Please select a task to start");
                return;
            }

            int requestId = (Integer) model.getValueAt(selectedRow, 0);
            try {
                assetService.startMaintenanceRequest(requestId, currentUser.getEmpId());
                JOptionPane.showMessageDialog(this, "Task started successfully!");
                loadData.run();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        completeBtn.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Please select a task to complete");
                return;
            }

            int requestId = (Integer) model.getValueAt(selectedRow, 0);
            String remarks = JOptionPane.showInputDialog(this, "Enter completion remarks:");
            if (remarks != null) {
                try {
                    assetService.completeMaintenanceRequest(requestId, currentUser.getEmpId(), remarks);
                    JOptionPane.showMessageDialog(this, "Task completed successfully!");
                    loadData.run();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
                }
            }
        });

        refreshBtn.addActionListener(e -> loadData.run());

        if (!userService.isTechnician()) {
            startBtn.setEnabled(false);
            completeBtn.setEnabled(false);
            startBtn.setToolTipText("Available for technicians only");
            completeBtn.setToolTipText("Available for technicians only");
        }

        return panel;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AssetManagementApp::new);
    }
}
