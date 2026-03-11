-- ============================================================
-- AUTO-UPDATE TRIGGERS for last_modified + version
-- Ensures ANY update on web automatically bumps last_modified
-- so Desktop PULL can detect changes.
-- ============================================================

-- Generic trigger function: auto-update last_modified + version on every UPDATE
CREATE OR REPLACE FUNCTION fn_auto_sync_update()
RETURNS TRIGGER AS $$
BEGIN
    NEW.last_modified := NOW();
    NEW.version := COALESCE(OLD.version, 0) + 1;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ── Module 1: POS ──
DROP TRIGGER IF EXISTS trg_sync_cashier ON Cashier;
CREATE TRIGGER trg_sync_cashier BEFORE UPDATE ON Cashier
    FOR EACH ROW EXECUTE FUNCTION fn_auto_sync_update();

DROP TRIGGER IF EXISTS trg_sync_order ON "Order";
CREATE TRIGGER trg_sync_order BEFORE UPDATE ON "Order"
    FOR EACH ROW EXECUTE FUNCTION fn_auto_sync_update();

DROP TRIGGER IF EXISTS trg_sync_payment ON Payment;
CREATE TRIGGER trg_sync_payment BEFORE UPDATE ON Payment
    FOR EACH ROW EXECUTE FUNCTION fn_auto_sync_update();

DROP TRIGGER IF EXISTS trg_sync_receipt ON Receipt;
CREATE TRIGGER trg_sync_receipt BEFORE UPDATE ON Receipt
    FOR EACH ROW EXECUTE FUNCTION fn_auto_sync_update();

DROP TRIGGER IF EXISTS trg_sync_returnorder ON ReturnOrder;
CREATE TRIGGER trg_sync_returnorder BEFORE UPDATE ON ReturnOrder
    FOR EACH ROW EXECUTE FUNCTION fn_auto_sync_update();

DROP TRIGGER IF EXISTS trg_sync_salesoutbound ON SalesOutbound;
CREATE TRIGGER trg_sync_salesoutbound BEFORE UPDATE ON SalesOutbound
    FOR EACH ROW EXECUTE FUNCTION fn_auto_sync_update();

-- ── Module 2: Inventory ──
DROP TRIGGER IF EXISTS trg_sync_warehouse ON warehouse;
CREATE TRIGGER trg_sync_warehouse BEFORE UPDATE ON warehouse
    FOR EACH ROW EXECUTE FUNCTION fn_auto_sync_update();

DROP TRIGGER IF EXISTS trg_sync_warehouse_balances ON warehouse_balances;
CREATE TRIGGER trg_sync_warehouse_balances BEFORE UPDATE ON warehouse_balances
    FOR EACH ROW EXECUTE FUNCTION fn_auto_sync_update();

-- ── Module 3: Product ──
DROP TRIGGER IF EXISTS trg_sync_productcategory ON ProductCategory;
CREATE TRIGGER trg_sync_productcategory BEFORE UPDATE ON ProductCategory
    FOR EACH ROW EXECUTE FUNCTION fn_auto_sync_update();

DROP TRIGGER IF EXISTS trg_sync_product ON Product;
CREATE TRIGGER trg_sync_product BEFORE UPDATE ON Product
    FOR EACH ROW EXECUTE FUNCTION fn_auto_sync_update();

DROP TRIGGER IF EXISTS trg_sync_productvariant ON ProductVariant;
CREATE TRIGGER trg_sync_productvariant BEFORE UPDATE ON ProductVariant
    FOR EACH ROW EXECUTE FUNCTION fn_auto_sync_update();

DROP TRIGGER IF EXISTS trg_sync_pricelist ON PriceList;
CREATE TRIGGER trg_sync_pricelist BEFORE UPDATE ON PriceList
    FOR EACH ROW EXECUTE FUNCTION fn_auto_sync_update();

DROP TRIGGER IF EXISTS trg_sync_productprice ON ProductPrice;
CREATE TRIGGER trg_sync_productprice BEFORE UPDATE ON ProductPrice
    FOR EACH ROW EXECUTE FUNCTION fn_auto_sync_update();

-- ── Module 4: CRM & Promotion ──
DROP TRIGGER IF EXISTS trg_sync_customer ON Customer;
CREATE TRIGGER trg_sync_customer BEFORE UPDATE ON Customer
    FOR EACH ROW EXECUTE FUNCTION fn_auto_sync_update();

DROP TRIGGER IF EXISTS trg_sync_membershiprank ON MembershipRank;
CREATE TRIGGER trg_sync_membershiprank BEFORE UPDATE ON MembershipRank
    FOR EACH ROW EXECUTE FUNCTION fn_auto_sync_update();

DROP TRIGGER IF EXISTS trg_sync_loyaltyaccount ON LoyaltyAccount;
CREATE TRIGGER trg_sync_loyaltyaccount BEFORE UPDATE ON LoyaltyAccount
    FOR EACH ROW EXECUTE FUNCTION fn_auto_sync_update();

DROP TRIGGER IF EXISTS trg_sync_loyaltypointrule ON LoyaltyPointRule;
CREATE TRIGGER trg_sync_loyaltypointrule BEFORE UPDATE ON LoyaltyPointRule
    FOR EACH ROW EXECUTE FUNCTION fn_auto_sync_update();

DROP TRIGGER IF EXISTS trg_sync_pointtransaction ON PointTransaction;
CREATE TRIGGER trg_sync_pointtransaction BEFORE UPDATE ON PointTransaction
    FOR EACH ROW EXECUTE FUNCTION fn_auto_sync_update();

DROP TRIGGER IF EXISTS trg_sync_campaign ON Campaign;
CREATE TRIGGER trg_sync_campaign BEFORE UPDATE ON Campaign
    FOR EACH ROW EXECUTE FUNCTION fn_auto_sync_update();

DROP TRIGGER IF EXISTS trg_sync_promotion ON Promotion;
CREATE TRIGGER trg_sync_promotion BEFORE UPDATE ON Promotion
    FOR EACH ROW EXECUTE FUNCTION fn_auto_sync_update();

DROP TRIGGER IF EXISTS trg_sync_promotionlog ON PromotionApplicationLog;
CREATE TRIGGER trg_sync_promotionlog BEFORE UPDATE ON PromotionApplicationLog
    FOR EACH ROW EXECUTE FUNCTION fn_auto_sync_update();

-- ── Module 5: HR & Shift ──
DROP TRIGGER IF EXISTS trg_sync_store ON Store;
CREATE TRIGGER trg_sync_store BEFORE UPDATE ON Store
    FOR EACH ROW EXECUTE FUNCTION fn_auto_sync_update();

DROP TRIGGER IF EXISTS trg_sync_employee ON Employee;
CREATE TRIGGER trg_sync_employee BEFORE UPDATE ON Employee
    FOR EACH ROW EXECUTE FUNCTION fn_auto_sync_update();

DROP TRIGGER IF EXISTS trg_sync_employeeassignment ON EmployeeAssignment;
CREATE TRIGGER trg_sync_employeeassignment BEFORE UPDATE ON EmployeeAssignment
    FOR EACH ROW EXECUTE FUNCTION fn_auto_sync_update();

DROP TRIGGER IF EXISTS trg_sync_storemanager ON StoreManager;
CREATE TRIGGER trg_sync_storemanager BEFORE UPDATE ON StoreManager
    FOR EACH ROW EXECUTE FUNCTION fn_auto_sync_update();

DROP TRIGGER IF EXISTS trg_sync_hr ON HR;
CREATE TRIGGER trg_sync_hr BEFORE UPDATE ON HR
    FOR EACH ROW EXECUTE FUNCTION fn_auto_sync_update();

DROP TRIGGER IF EXISTS trg_sync_shifttemplate ON ShiftTemplate;
CREATE TRIGGER trg_sync_shifttemplate BEFORE UPDATE ON ShiftTemplate
    FOR EACH ROW EXECUTE FUNCTION fn_auto_sync_update();

DROP TRIGGER IF EXISTS trg_sync_shiftassignment ON ShiftAssignment;
CREATE TRIGGER trg_sync_shiftassignment BEFORE UPDATE ON ShiftAssignment
    FOR EACH ROW EXECUTE FUNCTION fn_auto_sync_update();

DROP TRIGGER IF EXISTS trg_sync_attendancerecord ON AttendanceRecord;
CREATE TRIGGER trg_sync_attendancerecord BEFORE UPDATE ON AttendanceRecord
    FOR EACH ROW EXECUTE FUNCTION fn_auto_sync_update();

DROP TRIGGER IF EXISTS trg_sync_payrollperiod ON PayrollPeriod;
CREATE TRIGGER trg_sync_payrollperiod BEFORE UPDATE ON PayrollPeriod
    FOR EACH ROW EXECUTE FUNCTION fn_auto_sync_update();

DROP TRIGGER IF EXISTS trg_sync_payrollsnapshot ON PayrollSnapshot;
CREATE TRIGGER trg_sync_payrollsnapshot BEFORE UPDATE ON PayrollSnapshot
    FOR EACH ROW EXECUTE FUNCTION fn_auto_sync_update();

