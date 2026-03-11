-- ============================================================
-- PostgreSQL MIGRATIONS
-- Adds missing sync columns to existing tables.
-- Each ALTER TABLE uses a sub-select check so it's idempotent.
-- ============================================================

-- OrderItem: add sync columns
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='orderitem' AND column_name='last_modified') THEN
        ALTER TABLE OrderItem ADD COLUMN last_modified TIMESTAMP NOT NULL DEFAULT NOW();
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='orderitem' AND column_name='sync_status') THEN
        ALTER TABLE OrderItem ADD COLUMN sync_status TEXT NOT NULL DEFAULT 'PENDING';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='orderitem' AND column_name='version') THEN
        ALTER TABLE OrderItem ADD COLUMN version INTEGER NOT NULL DEFAULT 1;
    END IF;
END $$;

-- CashPayment: add sync columns
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='cashpayment' AND column_name='last_modified') THEN
        ALTER TABLE CashPayment ADD COLUMN last_modified TIMESTAMP NOT NULL DEFAULT NOW();
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='cashpayment' AND column_name='sync_status') THEN
        ALTER TABLE CashPayment ADD COLUMN sync_status TEXT NOT NULL DEFAULT 'PENDING';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='cashpayment' AND column_name='version') THEN
        ALTER TABLE CashPayment ADD COLUMN version INTEGER NOT NULL DEFAULT 1;
    END IF;
END $$;

-- QRPayment: add sync columns
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='qrpayment' AND column_name='last_modified') THEN
        ALTER TABLE QRPayment ADD COLUMN last_modified TIMESTAMP NOT NULL DEFAULT NOW();
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='qrpayment' AND column_name='sync_status') THEN
        ALTER TABLE QRPayment ADD COLUMN sync_status TEXT NOT NULL DEFAULT 'PENDING';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='qrpayment' AND column_name='version') THEN
        ALTER TABLE QRPayment ADD COLUMN version INTEGER NOT NULL DEFAULT 1;
    END IF;
END $$;

-- ReturnOrderItem: add sync columns
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='returnorderitem' AND column_name='last_modified') THEN
        ALTER TABLE ReturnOrderItem ADD COLUMN last_modified TIMESTAMP NOT NULL DEFAULT NOW();
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='returnorderitem' AND column_name='sync_status') THEN
        ALTER TABLE ReturnOrderItem ADD COLUMN sync_status TEXT NOT NULL DEFAULT 'PENDING';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='returnorderitem' AND column_name='version') THEN
        ALTER TABLE ReturnOrderItem ADD COLUMN version INTEGER NOT NULL DEFAULT 1;
    END IF;
END $$;

-- SalesOutboundItem: add sync columns
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='salesoutbounditem' AND column_name='last_modified') THEN
        ALTER TABLE SalesOutboundItem ADD COLUMN last_modified TIMESTAMP NOT NULL DEFAULT NOW();
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='salesoutbounditem' AND column_name='sync_status') THEN
        ALTER TABLE SalesOutboundItem ADD COLUMN sync_status TEXT NOT NULL DEFAULT 'PENDING';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='salesoutbounditem' AND column_name='version') THEN
        ALTER TABLE SalesOutboundItem ADD COLUMN version INTEGER NOT NULL DEFAULT 1;
    END IF;
END $$;

