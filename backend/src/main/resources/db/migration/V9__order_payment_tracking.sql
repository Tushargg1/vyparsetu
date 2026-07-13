-- Track how much of each order has been paid and when the last payment was made.
ALTER TABLE orders
    ADD COLUMN amount_paid DECIMAL(12,2) NOT NULL DEFAULT 0,
    ADD COLUMN last_payment_at TIMESTAMP NULL;

-- Backfill: orders already marked PAID are considered fully paid as of when they were placed.
UPDATE orders SET amount_paid = total_amount, last_payment_at = placed_at
WHERE payment_status = 'PAID';
