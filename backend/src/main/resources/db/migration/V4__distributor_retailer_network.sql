-- Closed distributor<->retailer network:
-- each distributor (supplier) has a unique invite code; each retailer is linked to one distributor.

ALTER TABLE suppliers
    ADD COLUMN invite_code VARCHAR(16) NULL,
    ADD COLUMN whatsapp_number VARCHAR(20) NULL;

ALTER TABLE suppliers
    ADD CONSTRAINT uq_supplier_invite_code UNIQUE (invite_code);

ALTER TABLE retailers
    ADD COLUMN distributor_id BIGINT UNSIGNED NULL;

ALTER TABLE retailers
    ADD CONSTRAINT fk_retailer_distributor FOREIGN KEY (distributor_id) REFERENCES suppliers(id);

CREATE INDEX idx_retailer_distributor ON retailers (distributor_id);
