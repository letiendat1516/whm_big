-- ============================================================
-- CROSS-MODULE SEED DATA
-- Load Order: LAST (after all module DDL + module seeds)
-- Contains INSERTs that reference tables across modules
-- ============================================================

-- PromotionApplicationLog rows referencing Order (module1) + Promotion (module4)
INSERT OR IGNORE INTO PromotionApplicationLog VALUES
('PLOG-001','PROMO-001','ORD-002',225000.0,datetime('2026-03-07 10:30:46'),datetime('now'),'SYNCED',1);

-- PointTransaction rows referencing Order (module1) + LoyaltyAccount (module4)
INSERT OR IGNORE INTO PointTransaction VALUES
('PTX-001','ACC-001','RULE-001','ORD-002',4500,'EARN',datetime('2026-03-07 10:30:47'),'+4500 pts from order ORD-002',datetime('now'),'SYNCED',1),
('PTX-002','ACC-002','RULE-001','ORD-001',950,'EARN',datetime('2026-03-07 09:15:31'),'+950 pts from order ORD-001',datetime('now'),'SYNCED',1);

