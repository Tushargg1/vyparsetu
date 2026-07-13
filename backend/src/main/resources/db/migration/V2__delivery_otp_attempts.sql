-- Brute-force protection for delivery OTP verification
ALTER TABLE deliveries
    ADD COLUMN otp_attempts INT NOT NULL DEFAULT 0 AFTER otp_hash;
