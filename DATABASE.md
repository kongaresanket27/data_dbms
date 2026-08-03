# Asset Management System - Core Java (JDBC)

This is a standalone Desktop Application built using **Java Swing** and **JDBC** for managing institutional assets with a direct connection to an Oracle XE database.

## 1. Database Schema

Execute the following commands in your Oracle SQL environment (e.g., SQL Developer or SQL*Plus) after connecting to your database as `hr/hr`.

```sql
-- 1. DEPARTMENT Table
CREATE TABLE DEPARTMENT (
    Dept_ID NUMBER PRIMARY KEY,
    Dept_Name VARCHAR2(100) NOT NULL,
    Manager_ID NUMBER,
    CONSTRAINT fk_dept_manager FOREIGN KEY (Manager_ID) REFERENCES EMPLOYEE(Emp_ID) ON DELETE SET NULL
);

-- 2. LOCATION Table
CREATE TABLE LOCATION (
    Location_ID NUMBER PRIMARY KEY,
    Building_Name VARCHAR2(100) NOT NULL,
    Floor NUMBER,
    Room_Number VARCHAR2(20)
);

-- 3. EMPLOYEE Table
CREATE TABLE EMPLOYEE (
    Emp_ID NUMBER PRIMARY KEY,
    First_Name VARCHAR2(50) NOT NULL,
    Last_Name VARCHAR2(50) NOT NULL,
    Email VARCHAR2(100) UNIQUE,
    Phone VARCHAR2(20),
    Dept_ID NUMBER,
    Role VARCHAR2(20) DEFAULT 'Employee' CHECK (Role IN ('Admin', 'Employee', 'Technician')),
    CONSTRAINT fk_emp_dept FOREIGN KEY (Dept_ID) REFERENCES DEPARTMENT(Dept_ID) ON DELETE SET NULL
);

-- 4. ASSET Table
CREATE TABLE ASSET (
    Asset_ID NUMBER PRIMARY KEY,
    Asset_Name VARCHAR2(100) NOT NULL,
    Asset_Type VARCHAR2(50),
    Purchase_Date DATE,
    Status VARCHAR2(20) DEFAULT 'Available' CHECK (Status IN ('Available', 'Assigned', 'Under_Maintenance', 'Retired')),
    Dept_ID NUMBER,
    Location_ID NUMBER,
    CONSTRAINT fk_asset_dept FOREIGN KEY (Dept_ID) REFERENCES DEPARTMENT(Dept_ID) ON DELETE SET NULL,
    CONSTRAINT fk_asset_loc FOREIGN KEY (Location_ID) REFERENCES LOCATION(Location_ID) ON DELETE SET NULL
);

-- 5. TECHNICIAN Table
CREATE TABLE TECHNICIAN (
    Tech_ID NUMBER PRIMARY KEY,
    Tech_Name VARCHAR2(100) NOT NULL,
    Specialization VARCHAR2(100),
    Phone VARCHAR2(20),
    Is_Active CHAR(1) DEFAULT 'Y' CHECK (Is_Active IN ('Y', 'N'))
);

-- 6. ASSET_ASSIGNMENT Table
CREATE TABLE ASSET_ASSIGNMENT (
    Assignment_ID NUMBER PRIMARY KEY,
    Asset_ID NUMBER NOT NULL,
    Emp_ID NUMBER NOT NULL,
    Assign_Date DATE DEFAULT SYSDATE,
    Return_Date DATE,
    CONSTRAINT fk_assign_asset FOREIGN KEY (Asset_ID) REFERENCES ASSET(Asset_ID) ON DELETE CASCADE,
    CONSTRAINT fk_assign_emp FOREIGN KEY (Emp_ID) REFERENCES EMPLOYEE(Emp_ID) ON DELETE CASCADE,
    CONSTRAINT chk_return_date CHECK (Return_Date IS NULL OR Return_Date >= Assign_Date),
    CONSTRAINT uk_active_assignment UNIQUE (Asset_ID, Emp_ID) -- Ensures one active assignment per asset-employee pair
);

-- 7. MAINTENANCE_REQUEST Table
CREATE TABLE MAINTENANCE_REQUEST (
    Request_ID NUMBER PRIMARY KEY,
    Asset_ID NUMBER NOT NULL,
    Emp_ID NUMBER NOT NULL,
    Tech_ID NUMBER,
    Request_Date DATE DEFAULT SYSDATE,
    Issue_Description CLOB,
    Priority VARCHAR2(10) DEFAULT 'Medium' CHECK (Priority IN ('Low', 'Medium', 'High')),
    Status VARCHAR2(20) DEFAULT 'Pending' CHECK (Status IN ('Pending', 'In_Progress', 'Completed', 'Rejected')),
    Completed_Date DATE,
    Remarks CLOB,
    CONSTRAINT fk_maint_asset FOREIGN KEY (Asset_ID) REFERENCES ASSET(Asset_ID) ON DELETE CASCADE,
    CONSTRAINT fk_maint_emp FOREIGN KEY (Emp_ID) REFERENCES EMPLOYEE(Emp_ID) ON DELETE CASCADE,
    CONSTRAINT fk_maint_tech FOREIGN KEY (Tech_ID) REFERENCES TECHNICIAN(Tech_ID) ON DELETE SET NULL,
    CONSTRAINT chk_completed_date CHECK (Completed_Date IS NULL OR Status = 'Completed'),
    CONSTRAINT uk_active_request UNIQUE (Asset_ID) -- Only one active maintenance request per asset
);

-- Indexes for performance
CREATE INDEX idx_asset_status ON ASSET(Status);
CREATE INDEX idx_asset_dept ON ASSET(Dept_ID);
CREATE INDEX idx_assignment_asset ON ASSET_ASSIGNMENT(Asset_ID);
CREATE INDEX idx_assignment_emp ON ASSET_ASSIGNMENT(Emp_ID);
CREATE INDEX idx_maintenance_asset ON MAINTENANCE_REQUEST(Asset_ID);
CREATE INDEX idx_maintenance_status ON MAINTENANCE_REQUEST(Status);
```

## 2. Sample Data

```sql
-- Insert data in order (respecting foreign key constraints)
INSERT INTO DEPARTMENT (Dept_ID, Dept_Name) VALUES (1, 'Computer Engineering');
INSERT INTO LOCATION VALUES (101, 'Building A', 2, 'Lab 204');

-- Insert employees first (some will be managers)
INSERT INTO EMPLOYEE (Emp_ID, First_Name, Last_Name, Email, Phone, Dept_ID, Role) 
VALUES (1, 'Dr.', 'Smith', 'smith@university.edu', '1234567890', 1, 'Admin');

INSERT INTO EMPLOYEE (Emp_ID, First_Name, Last_Name, Email, Phone, Dept_ID, Role) 
VALUES (2, 'John', 'Doe', 'john.doe@university.edu', '9876543210', 1, 'Employee');

-- Update department manager
UPDATE DEPARTMENT SET Manager_ID = 1 WHERE Dept_ID = 1;

INSERT INTO TECHNICIAN VALUES (1, 'Jane Technician', 'Hardware Specialist', '9876543210', 'Y');
INSERT INTO ASSET VALUES (5001, 'Dell Latitude Laptop', 'Computer', TO_DATE('2023-01-15', 'YYYY-MM-DD'), 'Available', 1, 101);

COMMIT;
```

## 3. Project Structure

The project follows the standard Maven/Java folder structure:

- `src/main/java/com/instiasset/AssetManagementApp.java`: **Main UI Entry Point**.
- `src/main/java/com/instiasset/DBConnection.java`: JDBC Connection Manager.

## 4. Local Setup (ojdbc17)

### Step 1: Requirements
- **Java JDK 17** or higher.
- **Oracle XE Database** running locally.
- **ojdbc17.jar** (Oracle JDBC Driver).

### Step 2: Library Configuration
1. Open the project in your IDE (IntelliJ IDEA, Eclipse, or NetBeans).
2. Right-click the project -> **Module Settings** (IntelliJ) or **Properties** (Eclipse).
3. Under **Libraries** or **Classpath**, add the `ojdbc17.jar` file.

### Step 3: Database Credentials
Ensure your Oracle database has a user `hr` with password `hr`. If you use different credentials, update them in:
`src/main/java/com/instiasset/DBConnection.java`

### Step 4: Run
Execute the `main` method in `AssetManagementApp.java`.

## 5. UI Features
- **Sidebar Navigation**: Switch between Dashboard, Assets, Employees, and Technicians.
- **Live DataSync**: Tables are populated directly from Oracle using `SELECT` queries.
- **Maintenance Queue**: Submit new requests which are saved to the `MAINTENANCE_REQUEST` table via `INSERT` and updated in the UI list.
