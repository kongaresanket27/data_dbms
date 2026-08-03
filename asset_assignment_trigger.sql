-- Trigger to prevent same asset from being assigned to different employees
-- This trigger fires BEFORE INSERT on ASSET_ASSIGNMENT table

CREATE OR REPLACE TRIGGER check_duplicate_asset_assignment
BEFORE INSERT ON ASSET_ASSIGNMENT
FOR EACH ROW
DECLARE
    v_existing_emp_id NUMBER;
BEGIN
    -- Check if this asset is already assigned to a different employee with no return date
    SELECT Emp_ID INTO v_existing_emp_id
    FROM ASSET_ASSIGNMENT
    WHERE Asset_ID = :NEW.Asset_ID
      AND Return_Date IS NULL  -- Active assignment (not returned)
      AND Emp_ID != :NEW.Emp_ID
      AND ROWNUM = 1;
    
    -- If a record is found, raise an error
    RAISE_APPLICATION_ERROR(-20001, 'ERROR: Asset ID ' || :NEW.Asset_ID || 
        ' is already assigned to a different employee. Cannot assign the same asset to multiple employees.');
    
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        -- No active assignment found for a different employee, proceed with insert
        NULL;
    WHEN OTHERS THEN
        -- Handle other exceptions
        RAISE;
END check_duplicate_asset_assignment;
/

-- Test the trigger (optional - uncomment to test)
/*
-- This should succeed (first assignment)
INSERT INTO ASSET_ASSIGNMENT (Assignment_ID, Asset_ID, Emp_ID, Assign_Date)
VALUES (1001, 5001, 1, SYSDATE);
COMMIT;

-- This should fail (same asset to different employee)
INSERT INTO ASSET_ASSIGNMENT (Assignment_ID, Asset_ID, Emp_ID, Assign_Date)
VALUES (1002, 5001, 2, SYSDATE);

-- This should succeed (same employee reassigned same asset - allowed)
INSERT INTO ASSET_ASSIGNMENT (Assignment_ID, Asset_ID, Emp_ID, Assign_Date)
VALUES (1003, 5001, 1, SYSDATE);

-- This should succeed (previous assignment returned, then new employee assigned)
UPDATE ASSET_ASSIGNMENT SET Return_Date = SYSDATE WHERE Assignment_ID = 1001;
INSERT INTO ASSET_ASSIGNMENT (Assignment_ID, Asset_ID, Emp_ID, Assign_Date)
VALUES (1004, 5001, 2, SYSDATE);
COMMIT;
*/
