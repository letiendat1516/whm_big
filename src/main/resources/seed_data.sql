-- ============================================================
-- CROSS-MODULE SEED DATA
-- Load Order: LAST (after all module DDL + module seeds)
-- Contains INSERTs that reference tables across modules
-- ============================================================

-- PromotionApplicationLog rows referencing Order (module1) + Promotion (module4)
INSERT OR IGNORE INTO PromotionApplicationLog VALUES
('PLOG-001','PROMO-003','ORD-002',2800.0,datetime('2026-03-07 10:30:46'),datetime('now'),'SYNCED',1);

-- PointTransaction rows referencing Order (module1) + LoyaltyAccount (module4)
INSERT OR IGNORE INTO PointTransaction VALUES
('PTX-001','ACC-001','RULE-001','ORD-002',252,'EARN',datetime('2026-03-07 10:30:47'),'+252 pts from order ORD-002',datetime('now'),'SYNCED',1),
('PTX-002','ACC-005','RULE-002',NULL,500,'BONUS',datetime('2026-03-01 00:00:00'),'March bonus points',datetime('now'),'SYNCED',1),
('PTX-003','ACC-002','RULE-001','ORD-001',45,'EARN',datetime('2026-03-07 09:15:31'),'+45 pts from order ORD-001',datetime('now'),'SYNCED',1);

