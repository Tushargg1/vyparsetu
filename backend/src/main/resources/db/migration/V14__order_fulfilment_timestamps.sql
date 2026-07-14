-- When an order was accepted, packed and delivered (for fulfilment views).
ALTER TABLE orders
    ADD COLUMN accepted_at TIMESTAMP NULL,
    ADD COLUMN packed_at TIMESTAMP NULL,
    ADD COLUMN delivered_at TIMESTAMP NULL;

-- Backfill completed/delivered orders so history views have data.
UPDATE orders SET delivered_at = placed_at, packed_at = placed_at, accepted_at = placed_at
WHERE status IN ('DELIVERED', 'CASH_COLLECTED', 'COMPLETED');
