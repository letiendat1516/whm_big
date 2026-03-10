-- ============================================================
-- SEED DATA for PostgreSQL (Railway)
-- ============================================================

-- STORES
INSERT INTO Store("storeId","storeCode",name,address,status) VALUES
('STORE-001','S001','Cửa hàng VLXD Trung Tâm','123 Nguyễn Trãi, Q.1','ACTIVE'),
('STORE-002','S002','Chi nhánh Quận 7','45 Nguyễn Thị Thập, Q.7','ACTIVE'),
('STORE-003','S003','Chi nhánh Thủ Đức','789 Xa lộ Hà Nội, TP.Thủ Đức','ACTIVE')
ON CONFLICT DO NOTHING;

-- EMPLOYEES
INSERT INTO Employee("employeeId","employeeCode","fullName","hireDate","baseSalary",status) VALUES
('EMP-001','NV001','Nguyễn Văn An','2024-01-15',8000000,'ACTIVE'),
('EMP-002','NV002','Trần Thị Bình','2024-02-01',7500000,'ACTIVE'),
('EMP-003','NV003','Lê Minh Cường','2024-03-01',9000000,'ACTIVE'),
('EMP-004','NV004','Phạm Thị Dung','2024-04-01',7500000,'ACTIVE'),
('EMP-005','NV005','Hoàng Văn Em','2023-06-01',10000000,'ACTIVE')
ON CONFLICT DO NOTHING;

-- EMPLOYEE ASSIGNMENTS
INSERT INTO EmployeeAssignment("assignmentId","employeeId","storeId","startDate",status) VALUES
('ASSIGN-001','EMP-001','STORE-001','2024-01-15','ACTIVE'),
('ASSIGN-002','EMP-002','STORE-001','2024-02-01','ACTIVE'),
('ASSIGN-003','EMP-003','STORE-002','2024-03-01','ACTIVE'),
('ASSIGN-004','EMP-004','STORE-003','2024-04-01','ACTIVE'),
('ASSIGN-005','EMP-005','STORE-001','2023-06-01','ACTIVE')
ON CONFLICT DO NOTHING;

-- STORE MANAGER
INSERT INTO StoreManager("managerId","employeeId","storeId") VALUES
('MGR-001','EMP-005','STORE-001'),
('MGR-002','EMP-003','STORE-002')
ON CONFLICT DO NOTHING;

-- SHIFT TEMPLATES
INSERT INTO ShiftTemplate("shiftTemplateId",name,"startTime","endTime","breakMinutes",status) VALUES
('SHIFT-T1','Ca Sáng','06:00','14:00',30,'ACTIVE'),
('SHIFT-T2','Ca Chiều','14:00','22:00',30,'ACTIVE'),
('SHIFT-T3','Ca Đêm','22:00','06:00',30,'ACTIVE')
ON CONFLICT DO NOTHING;

-- PRODUCT CATEGORIES (Vật liệu xây dựng)
INSERT INTO ProductCategory(category_id,category_name,description,status) VALUES
('CAT-001','Xi măng','Các loại xi măng','1'),
('CAT-002','Sắt thép','Thép xây dựng các loại','1'),
('CAT-003','Gạch','Gạch xây, gạch ốp lát','1'),
('CAT-004','Cát đá','Cát xây, đá dăm','1'),
('CAT-005','Sơn','Sơn tường, sơn chống thấm','1')
ON CONFLICT DO NOTHING;

-- PRODUCTS
INSERT INTO Product("ProductID",product_code,"Name",category_id,brand,unit,"Description","Status",sales_rate) VALUES
('PROD-001','XM-001','Xi măng Hà Tiên PCB40','CAT-001','Hà Tiên','bao','Xi măng PCB40 50kg','ACTIVE',150),
('PROD-002','XM-002','Xi măng INSEE','CAT-001','INSEE','bao','Xi măng INSEE 50kg','ACTIVE',120),
('PROD-003','ST-001','Thép Pomina D10','CAT-002','Pomina','cây','Thép cuộn D10 dài 11.7m','ACTIVE',90),
('PROD-004','ST-002','Thép Việt Nhật D12','CAT-002','Việt Nhật','cây','Thép vằn D12 dài 11.7m','ACTIVE',60),
('PROD-005','GA-001','Gạch Đồng Tâm 40x40','CAT-003','Đồng Tâm','viên','Gạch lát nền 40x40cm','ACTIVE',80)
ON CONFLICT DO NOTHING;

-- PRICE LIST
INSERT INTO PriceList(price_list_id,price_list_name,description,status) VALUES
('PL-001','Giá bán lẻ','Bảng giá bán lẻ tiêu chuẩn','ACTIVE'),
('PL-002','Giá hội viên','Giá ưu đãi cho khách hàng thân thiết','ACTIVE'),
('PL-003','Giá sỉ','Giá bán sỉ cho đại lý','INACTIVE')
ON CONFLICT DO NOTHING;

-- PRODUCT VARIANTS
INSERT INTO ProductVariant(variant_id,product_id,variant_name,barcode,status) VALUES
('VAR-001','PROD-001','50kg/bao','8935001001001',1),
('VAR-002','PROD-001','Pallet 50 bao','8935001001002',1),
('VAR-003','PROD-002','50kg/bao','8935002001001',1),
('VAR-004','PROD-003','Cây 11.7m','8935003001001',1),
('VAR-005','PROD-004','Cây 11.7m','8935004001001',1),
('VAR-006','PROD-005','Thùng 16 viên','8935005001001',1),
('VAR-007','PROD-005','Viên lẻ','8935005001002',1)
ON CONFLICT DO NOTHING;

-- PRODUCT PRICES
INSERT INTO ProductPrice(price_id,variant_id,price_list_id,price,start_time,end_time) VALUES
('PRC-001','VAR-001','PL-001',95000,'2024-01-01',NULL),
('PRC-002','VAR-001','PL-002',90000,'2024-01-01',NULL),
('PRC-003','VAR-002','PL-001',4500000,'2024-01-01',NULL),
('PRC-004','VAR-003','PL-001',98000,'2024-01-01',NULL),
('PRC-005','VAR-004','PL-001',85000,'2024-01-01',NULL),
('PRC-006','VAR-005','PL-001',95000,'2024-01-01',NULL),
('PRC-007','VAR-006','PL-001',120000,'2024-01-01',NULL),
('PRC-008','VAR-007','PL-001',8000,'2024-01-01',NULL)
ON CONFLICT DO NOTHING;

-- CASHIERS
INSERT INTO Cashier("cashierId","employeeId","storeId",is_active) VALUES
('CSH-001','EMP-001','STORE-001',1),
('CSH-002','EMP-002','STORE-001',1),
('CSH-003','EMP-004','STORE-003',1)
ON CONFLICT DO NOTHING;

-- WAREHOUSES
INSERT INTO warehouse(warehouse_id,warehouse_code,warehouse_name,address,is_active,threshold) VALUES
('WH-001','KHO-001','Kho Trung Tâm','100 Điện Biên Phủ, Q.1',1,20),
('WH-002','KHO-002','Kho Quận 7','50 Nguyễn Hữu Thọ, Q.7',1,15),
('WH-003','KHO-003','Kho Thủ Đức','200 Xa Lộ Hà Nội',1,10)
ON CONFLICT DO NOTHING;

-- WAREHOUSE BALANCES
INSERT INTO warehouse_balances(warehouse_id,product_id,on_hand_qty,reserved_qty) VALUES
('WH-001','PROD-001',500,10),
('WH-001','PROD-002',300,5),
('WH-001','PROD-003',200,0),
('WH-001','PROD-004',150,0),
('WH-001','PROD-005',1000,0),
('WH-002','PROD-001',200,0),
('WH-002','PROD-003',100,0),
('WH-003','PROD-002',150,0),
('WH-003','PROD-005',500,0)
ON CONFLICT DO NOTHING;

-- MEMBERSHIP RANKS
INSERT INTO MembershipRank("TierID","TierName","MinPointsRequired","EarningMultipliers",description) VALUES
('TIER-001','Thường',0,1.0,'Hạng mặc định'),
('TIER-002','Bạc',500,1.5,'Tích lũy 500 điểm'),
('TIER-003','Vàng',2000,2.0,'Tích lũy 2000 điểm'),
('TIER-004','Kim Cương',5000,3.0,'Tích lũy 5000 điểm')
ON CONFLICT DO NOTHING;

-- CUSTOMERS
INSERT INTO Customer("CustomerID","PhoneNum","FullName","Email") VALUES
('CUST-001','0901234567','Nguyễn Văn Khách','khach1@email.com'),
('CUST-002','0902345678','Trần Thị Hoa','hoa@email.com'),
('CUST-003','0903456789','Lê Công Trình','congtrinh@email.com')
ON CONFLICT DO NOTHING;

-- LOYALTY ACCOUNTS
INSERT INTO LoyaltyAccount("AccountID","CustomerID","TierID","CurrentPoints","TotalPointsEarned") VALUES
('ACC-001','CUST-001','TIER-002',750,1200),
('ACC-002','CUST-002','TIER-001',100,100),
('ACC-003','CUST-003','TIER-003',2500,3500)
ON CONFLICT DO NOTHING;

-- LOYALTY POINT RULES
INSERT INTO LoyaltyPointRule("RuleID","RuleName","EarnRate","RedeemRate","MaxRedeemPerOrder",is_active) VALUES
('RULE-001','Tích điểm tiêu chuẩn',0.001,100,500,1),
('RULE-002','Tích điểm gấp đôi',0.002,100,1000,0)
ON CONFLICT DO NOTHING;

-- CAMPAIGNS
INSERT INTO Campaign("CampaignID","Name","Description","StartDate","EndDate","IsActive") VALUES
('CAMP-001','Khuyến mãi Tháng 3','Giảm giá mùa xây dựng','2026-03-01','2026-03-31',1),
('CAMP-002','Flash Sale Cuối Tuần','Giảm giá cuối tuần','2026-03-08','2026-03-09',1)
ON CONFLICT DO NOTHING;

-- PROMOTIONS
INSERT INTO Promotion("PromotionID","CampaignID","PromoType","Name","Priority","RuleDefinition","VoucherCode","MaxUsageCount","CurrentUsageCount","ExpiryDate","IsActive") VALUES
('PROMO-001','CAMP-001','PERCENT_DISCOUNT','Giảm 5% Xi Măng',1,'{"percent":5}','XIMANG5',100,5,'2026-03-31',1),
('PROMO-002','CAMP-001','FIXED_DISCOUNT','Giảm 50K đơn từ 2 triệu',2,'{"fixed":50000,"min_order":2000000}','GIAM50K',50,2,'2026-03-31',1),
('PROMO-003','CAMP-002','PERCENT_DISCOUNT','Flash Sale 10%',3,'{"percent":10}',NULL,200,0,'2026-03-09',1)
ON CONFLICT DO NOTHING;

-- SAMPLE ORDERS
INSERT INTO "Order"("orderId","cashierId","storeId","orderDate",status,"totalAmount","discountAmount","finalAmount","taxRate","taxAmount","paymentStatus","paidAmount","debtAmount",note) VALUES
('ORD-001','CSH-001','STORE-001','2026-03-07 09:15:00','COMPLETED',950000,0,1045000,0.1,95000,'PAID',1045000,0,NULL),
('ORD-002','CSH-001','STORE-001','2026-03-07 10:30:00','COMPLETED',4500000,225000,4702500,0.1,427500,'PAID',4702500,0,'Khách VIP'),
('ORD-003','CSH-002','STORE-001','2026-03-07 14:45:00','COMPLETED',255000,0,280500,0.1,25500,'PARTIAL',200000,80500,'Còn nợ 80.500đ')
ON CONFLICT DO NOTHING;

-- ORDER ITEMS
INSERT INTO OrderItem("orderItemId","orderId","productId","variantId",quantity,"unitPrice",subtotal) VALUES
('OI-001','ORD-001','PROD-001','VAR-001',10,95000,950000),
('OI-002','ORD-002','PROD-001','VAR-002',1,4500000,4500000),
('OI-003','ORD-003','PROD-005','VAR-007',30,8000,240000),
('OI-004','ORD-003','PROD-003','VAR-004',1,15000,15000)
ON CONFLICT DO NOTHING;

-- PAYMENTS
INSERT INTO Payment("paymentId","orderId",method,"amountPaid",status) VALUES
('PAY-001','ORD-001','CASH',1045000,'COMPLETED'),
('PAY-002','ORD-002','QR',4702500,'COMPLETED'),
('PAY-003','ORD-003','CASH',200000,'COMPLETED')
ON CONFLICT DO NOTHING;

INSERT INTO CashPayment("paymentId","cashReceived","changeAmount") VALUES
('PAY-001',1100000,55000),
('PAY-003',200000,0)
ON CONFLICT DO NOTHING;

INSERT INTO QRPayment("paymentId","qrCode","transactionRef") VALUES
('PAY-002','QR-VIETQR-001','TXN-MOMO-20260307-001')
ON CONFLICT DO NOTHING;

-- SUPPLIERS
INSERT INTO Supplier("supplierID",name_supplier,"isCooperating",contact_phone,address) VALUES
('SUP-001','Công ty Xi Măng Hà Tiên',1,'028-3821-0001','Nhà Bè, TP.HCM'),
('SUP-002','Thép Pomina',1,'028-3844-0002','Phú Mỹ, Bà Rịa'),
('SUP-003','Gạch Đồng Tâm',1,'028-3822-0003','Long An')
ON CONFLICT DO NOTHING;

