-- ============================================================
-- SQLite SYNC QUEUE TRIGGERS
-- Auto-enqueue INSERT/UPDATE/DELETE operations to sync_queue
-- so the SyncService can push them to the cloud server.
-- ============================================================

-- ── Module 3: Product ──

-- ProductCategory
CREATE TRIGGER IF NOT EXISTS trg_syncq_category_insert
    AFTER INSERT ON ProductCategory
    FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'ProductCategory', NEW.category_id, 'INSERT', 'PENDING');
END;

CREATE TRIGGER IF NOT EXISTS trg_syncq_category_update
    AFTER UPDATE ON ProductCategory
    FOR EACH ROW
    WHEN OLD.sync_status != 'SYNCING'
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'ProductCategory', NEW.category_id, 'UPDATE', 'PENDING');
END;

CREATE TRIGGER IF NOT EXISTS trg_syncq_category_delete
    AFTER DELETE ON ProductCategory
    FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'ProductCategory', OLD.category_id, 'DELETE', 'PENDING');
END;

-- Product
CREATE TRIGGER IF NOT EXISTS trg_syncq_product_insert
    AFTER INSERT ON Product
    FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'Product', NEW.ProductID, 'INSERT', 'PENDING');
END;

CREATE TRIGGER IF NOT EXISTS trg_syncq_product_update
    AFTER UPDATE ON Product
    FOR EACH ROW
    WHEN OLD.sync_status != 'SYNCING'
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'Product', NEW.ProductID, 'UPDATE', 'PENDING');
END;

CREATE TRIGGER IF NOT EXISTS trg_syncq_product_delete
    AFTER DELETE ON Product
    FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'Product', OLD.ProductID, 'DELETE', 'PENDING');
END;

-- ProductVariant
CREATE TRIGGER IF NOT EXISTS trg_syncq_variant_insert
    AFTER INSERT ON ProductVariant
    FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'ProductVariant', NEW.variant_id, 'INSERT', 'PENDING');
END;

CREATE TRIGGER IF NOT EXISTS trg_syncq_variant_update
    AFTER UPDATE ON ProductVariant
    FOR EACH ROW
    WHEN OLD.sync_status != 'SYNCING'
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'ProductVariant', NEW.variant_id, 'UPDATE', 'PENDING');
END;

CREATE TRIGGER IF NOT EXISTS trg_syncq_variant_delete
    AFTER DELETE ON ProductVariant
    FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'ProductVariant', OLD.variant_id, 'DELETE', 'PENDING');
END;

-- PriceList
CREATE TRIGGER IF NOT EXISTS trg_syncq_pricelist_insert
    AFTER INSERT ON PriceList
    FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'PriceList', NEW.price_list_id, 'INSERT', 'PENDING');
END;

CREATE TRIGGER IF NOT EXISTS trg_syncq_pricelist_update
    AFTER UPDATE ON PriceList
    FOR EACH ROW
    WHEN OLD.sync_status != 'SYNCING'
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'PriceList', NEW.price_list_id, 'UPDATE', 'PENDING');
END;

CREATE TRIGGER IF NOT EXISTS trg_syncq_pricelist_delete
    AFTER DELETE ON PriceList
    FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'PriceList', OLD.price_list_id, 'DELETE', 'PENDING');
END;

-- ProductPrice
CREATE TRIGGER IF NOT EXISTS trg_syncq_productprice_insert
    AFTER INSERT ON ProductPrice
    FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'ProductPrice', NEW.price_id, 'INSERT', 'PENDING');
END;

CREATE TRIGGER IF NOT EXISTS trg_syncq_productprice_update
    AFTER UPDATE ON ProductPrice
    FOR EACH ROW
    WHEN OLD.sync_status != 'SYNCING'
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'ProductPrice', NEW.price_id, 'UPDATE', 'PENDING');
END;

CREATE TRIGGER IF NOT EXISTS trg_syncq_productprice_delete
    AFTER DELETE ON ProductPrice
    FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'ProductPrice', OLD.price_id, 'DELETE', 'PENDING');
END;

-- ── Module 5: HR & Shift ──

-- Store
CREATE TRIGGER IF NOT EXISTS trg_syncq_store_insert
    AFTER INSERT ON Store FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'Store', NEW.storeId, 'INSERT', 'PENDING');
END;
CREATE TRIGGER IF NOT EXISTS trg_syncq_store_update
    AFTER UPDATE ON Store FOR EACH ROW
    WHEN OLD.sync_status != 'SYNCING'
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'Store', NEW.storeId, 'UPDATE', 'PENDING');
END;
CREATE TRIGGER IF NOT EXISTS trg_syncq_store_delete
    AFTER DELETE ON Store FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'Store', OLD.storeId, 'DELETE', 'PENDING');
END;

-- Employee
CREATE TRIGGER IF NOT EXISTS trg_syncq_employee_insert
    AFTER INSERT ON Employee FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'Employee', NEW.employeeId, 'INSERT', 'PENDING');
END;
CREATE TRIGGER IF NOT EXISTS trg_syncq_employee_update
    AFTER UPDATE ON Employee FOR EACH ROW
    WHEN OLD.sync_status != 'SYNCING'
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'Employee', NEW.employeeId, 'UPDATE', 'PENDING');
END;
CREATE TRIGGER IF NOT EXISTS trg_syncq_employee_delete
    AFTER DELETE ON Employee FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'Employee', OLD.employeeId, 'DELETE', 'PENDING');
END;

-- EmployeeAssignment
CREATE TRIGGER IF NOT EXISTS trg_syncq_empassign_insert
    AFTER INSERT ON EmployeeAssignment FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'EmployeeAssignment', NEW.assignmentId, 'INSERT', 'PENDING');
END;
CREATE TRIGGER IF NOT EXISTS trg_syncq_empassign_update
    AFTER UPDATE ON EmployeeAssignment FOR EACH ROW
    WHEN OLD.sync_status != 'SYNCING'
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'EmployeeAssignment', NEW.assignmentId, 'UPDATE', 'PENDING');
END;
CREATE TRIGGER IF NOT EXISTS trg_syncq_empassign_delete
    AFTER DELETE ON EmployeeAssignment FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'EmployeeAssignment', OLD.assignmentId, 'DELETE', 'PENDING');
END;

-- HR
CREATE TRIGGER IF NOT EXISTS trg_syncq_hr_insert
    AFTER INSERT ON HR FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'HR', NEW.hrId, 'INSERT', 'PENDING');
END;
CREATE TRIGGER IF NOT EXISTS trg_syncq_hr_update
    AFTER UPDATE ON HR FOR EACH ROW
    WHEN OLD.sync_status != 'SYNCING'
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'HR', NEW.hrId, 'UPDATE', 'PENDING');
END;
CREATE TRIGGER IF NOT EXISTS trg_syncq_hr_delete
    AFTER DELETE ON HR FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'HR', OLD.hrId, 'DELETE', 'PENDING');
END;

-- ShiftTemplate
CREATE TRIGGER IF NOT EXISTS trg_syncq_shifttemplate_insert
    AFTER INSERT ON ShiftTemplate FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'ShiftTemplate', NEW.shiftTemplateId, 'INSERT', 'PENDING');
END;
CREATE TRIGGER IF NOT EXISTS trg_syncq_shifttemplate_update
    AFTER UPDATE ON ShiftTemplate FOR EACH ROW
    WHEN OLD.sync_status != 'SYNCING'
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'ShiftTemplate', NEW.shiftTemplateId, 'UPDATE', 'PENDING');
END;
CREATE TRIGGER IF NOT EXISTS trg_syncq_shifttemplate_delete
    AFTER DELETE ON ShiftTemplate FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'ShiftTemplate', OLD.shiftTemplateId, 'DELETE', 'PENDING');
END;

-- ShiftAssignment
CREATE TRIGGER IF NOT EXISTS trg_syncq_shiftassignment_insert
    AFTER INSERT ON ShiftAssignment FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'ShiftAssignment', NEW.shiftAssignmentId, 'INSERT', 'PENDING');
END;
CREATE TRIGGER IF NOT EXISTS trg_syncq_shiftassignment_update
    AFTER UPDATE ON ShiftAssignment FOR EACH ROW
    WHEN OLD.sync_status != 'SYNCING'
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'ShiftAssignment', NEW.shiftAssignmentId, 'UPDATE', 'PENDING');
END;
CREATE TRIGGER IF NOT EXISTS trg_syncq_shiftassignment_delete
    AFTER DELETE ON ShiftAssignment FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'ShiftAssignment', OLD.shiftAssignmentId, 'DELETE', 'PENDING');
END;

-- AttendanceRecord
CREATE TRIGGER IF NOT EXISTS trg_syncq_attendance_insert
    AFTER INSERT ON AttendanceRecord FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'AttendanceRecord', NEW.attendanceId, 'INSERT', 'PENDING');
END;
CREATE TRIGGER IF NOT EXISTS trg_syncq_attendance_update
    AFTER UPDATE ON AttendanceRecord FOR EACH ROW
    WHEN OLD.sync_status != 'SYNCING'
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'AttendanceRecord', NEW.attendanceId, 'UPDATE', 'PENDING');
END;
CREATE TRIGGER IF NOT EXISTS trg_syncq_attendance_delete
    AFTER DELETE ON AttendanceRecord FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'AttendanceRecord', OLD.attendanceId, 'DELETE', 'PENDING');
END;

-- PayrollPeriod
CREATE TRIGGER IF NOT EXISTS trg_syncq_payrollperiod_insert
    AFTER INSERT ON PayrollPeriod FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'PayrollPeriod', NEW.payrollPeriodId, 'INSERT', 'PENDING');
END;
CREATE TRIGGER IF NOT EXISTS trg_syncq_payrollperiod_update
    AFTER UPDATE ON PayrollPeriod FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'PayrollPeriod', NEW.payrollPeriodId, 'UPDATE', 'PENDING');
END;

-- PayrollSnapshot
CREATE TRIGGER IF NOT EXISTS trg_syncq_payrollsnapshot_insert
    AFTER INSERT ON PayrollSnapshot FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'PayrollSnapshot', NEW.snapshotId, 'INSERT', 'PENDING');
END;
CREATE TRIGGER IF NOT EXISTS trg_syncq_payrollsnapshot_update
    AFTER UPDATE ON PayrollSnapshot FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'PayrollSnapshot', NEW.snapshotId, 'UPDATE', 'PENDING');
END;

-- ── Module 4: CRM & Promotion ──

-- Customer
CREATE TRIGGER IF NOT EXISTS trg_syncq_customer_insert
    AFTER INSERT ON Customer FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'Customer', NEW.CustomerID, 'INSERT', 'PENDING');
END;
CREATE TRIGGER IF NOT EXISTS trg_syncq_customer_update
    AFTER UPDATE ON Customer FOR EACH ROW
    WHEN OLD.sync_status != 'SYNCING'
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'Customer', NEW.CustomerID, 'UPDATE', 'PENDING');
END;
CREATE TRIGGER IF NOT EXISTS trg_syncq_customer_delete
    AFTER DELETE ON Customer FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'Customer', OLD.CustomerID, 'DELETE', 'PENDING');
END;

-- MembershipRank
CREATE TRIGGER IF NOT EXISTS trg_syncq_membershiprank_insert
    AFTER INSERT ON MembershipRank FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'MembershipRank', NEW.TierID, 'INSERT', 'PENDING');
END;
CREATE TRIGGER IF NOT EXISTS trg_syncq_membershiprank_update
    AFTER UPDATE ON MembershipRank FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'MembershipRank', NEW.TierID, 'UPDATE', 'PENDING');
END;

-- LoyaltyAccount
CREATE TRIGGER IF NOT EXISTS trg_syncq_loyaltyaccount_insert
    AFTER INSERT ON LoyaltyAccount FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'LoyaltyAccount', NEW.AccountID, 'INSERT', 'PENDING');
END;
CREATE TRIGGER IF NOT EXISTS trg_syncq_loyaltyaccount_update
    AFTER UPDATE ON LoyaltyAccount FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'LoyaltyAccount', NEW.AccountID, 'UPDATE', 'PENDING');
END;

-- Campaign
CREATE TRIGGER IF NOT EXISTS trg_syncq_campaign_insert
    AFTER INSERT ON Campaign FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'Campaign', NEW.CampaignID, 'INSERT', 'PENDING');
END;
CREATE TRIGGER IF NOT EXISTS trg_syncq_campaign_update
    AFTER UPDATE ON Campaign FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'Campaign', NEW.CampaignID, 'UPDATE', 'PENDING');
END;

-- Promotion
CREATE TRIGGER IF NOT EXISTS trg_syncq_promotion_insert
    AFTER INSERT ON Promotion FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'Promotion', NEW.PromotionID, 'INSERT', 'PENDING');
END;
CREATE TRIGGER IF NOT EXISTS trg_syncq_promotion_update
    AFTER UPDATE ON Promotion FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'Promotion', NEW.PromotionID, 'UPDATE', 'PENDING');
END;

-- ── Module 1: POS ──

-- Cashier
CREATE TRIGGER IF NOT EXISTS trg_syncq_cashier_insert
    AFTER INSERT ON Cashier FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'Cashier', NEW.cashierId, 'INSERT', 'PENDING');
END;
CREATE TRIGGER IF NOT EXISTS trg_syncq_cashier_update
    AFTER UPDATE ON Cashier FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'Cashier', NEW.cashierId, 'UPDATE', 'PENDING');
END;

-- StoreManager
CREATE TRIGGER IF NOT EXISTS trg_syncq_storemanager_insert
    AFTER INSERT ON StoreManager FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'StoreManager', NEW.managerId, 'INSERT', 'PENDING');
END;
CREATE TRIGGER IF NOT EXISTS trg_syncq_storemanager_update
    AFTER UPDATE ON StoreManager FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'StoreManager', NEW.managerId, 'UPDATE', 'PENDING');
END;

-- Order (table name is reserved in SQL so uses backticks in SQLite)
CREATE TRIGGER IF NOT EXISTS trg_syncq_order_insert
    AFTER INSERT ON "Order" FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'Order', NEW.orderId, 'INSERT', 'PENDING');
END;
CREATE TRIGGER IF NOT EXISTS trg_syncq_order_update
    AFTER UPDATE ON "Order" FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'Order', NEW.orderId, 'UPDATE', 'PENDING');
END;

-- OrderItem
CREATE TRIGGER IF NOT EXISTS trg_syncq_orderitem_insert
    AFTER INSERT ON OrderItem FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'OrderItem', NEW.orderItemId, 'INSERT', 'PENDING');
END;
CREATE TRIGGER IF NOT EXISTS trg_syncq_orderitem_update
    AFTER UPDATE ON OrderItem FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'OrderItem', NEW.orderItemId, 'UPDATE', 'PENDING');
END;

-- Payment
CREATE TRIGGER IF NOT EXISTS trg_syncq_payment_insert
    AFTER INSERT ON Payment FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'Payment', NEW.paymentId, 'INSERT', 'PENDING');
END;
CREATE TRIGGER IF NOT EXISTS trg_syncq_payment_update
    AFTER UPDATE ON Payment FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'Payment', NEW.paymentId, 'UPDATE', 'PENDING');
END;

-- CashPayment
CREATE TRIGGER IF NOT EXISTS trg_syncq_cashpayment_insert
    AFTER INSERT ON CashPayment FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'CashPayment', NEW.paymentId, 'INSERT', 'PENDING');
END;

-- QRPayment
CREATE TRIGGER IF NOT EXISTS trg_syncq_qrpayment_insert
    AFTER INSERT ON QRPayment FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'QRPayment', NEW.paymentId, 'INSERT', 'PENDING');
END;

-- Receipt
CREATE TRIGGER IF NOT EXISTS trg_syncq_receipt_insert
    AFTER INSERT ON Receipt FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'Receipt', NEW.receiptId, 'INSERT', 'PENDING');
END;

-- ReturnOrder
CREATE TRIGGER IF NOT EXISTS trg_syncq_returnorder_insert
    AFTER INSERT ON ReturnOrder FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'ReturnOrder', NEW.returnId, 'INSERT', 'PENDING');
END;
CREATE TRIGGER IF NOT EXISTS trg_syncq_returnorder_update
    AFTER UPDATE ON ReturnOrder FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'ReturnOrder', NEW.returnId, 'UPDATE', 'PENDING');
END;

-- ReturnOrderItem
CREATE TRIGGER IF NOT EXISTS trg_syncq_returnorderitem_insert
    AFTER INSERT ON ReturnOrderItem FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'ReturnOrderItem', NEW.returnItemId, 'INSERT', 'PENDING');
END;

-- SalesOutbound
CREATE TRIGGER IF NOT EXISTS trg_syncq_salesoutbound_insert
    AFTER INSERT ON SalesOutbound FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'SalesOutbound', NEW.outboundId, 'INSERT', 'PENDING');
END;
CREATE TRIGGER IF NOT EXISTS trg_syncq_salesoutbound_update
    AFTER UPDATE ON SalesOutbound FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'SalesOutbound', NEW.outboundId, 'UPDATE', 'PENDING');
END;

-- SalesOutboundItem
CREATE TRIGGER IF NOT EXISTS trg_syncq_salesoutbounditem_insert
    AFTER INSERT ON SalesOutboundItem FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'SalesOutboundItem', NEW.outboundItemId, 'INSERT', 'PENDING');
END;

-- ── Module 2: Inventory ──

-- warehouse
CREATE TRIGGER IF NOT EXISTS trg_syncq_warehouse_insert
    AFTER INSERT ON warehouse FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'warehouse', NEW.warehouse_id, 'INSERT', 'PENDING');
END;
CREATE TRIGGER IF NOT EXISTS trg_syncq_warehouse_update
    AFTER UPDATE ON warehouse FOR EACH ROW
BEGIN
    INSERT INTO sync_queue(queue_id, table_name, record_id, operation, status)
    VALUES (lower(hex(randomblob(16))), 'warehouse', NEW.warehouse_id, 'UPDATE', 'PENDING');
END;

