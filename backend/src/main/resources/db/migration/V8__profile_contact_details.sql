-- Extra contact numbers and a shared location link for retailers and distributors.
ALTER TABLE retailers
    ADD COLUMN alt_phones VARCHAR(512) NULL,
    ADD COLUMN location_url VARCHAR(1024) NULL;

ALTER TABLE suppliers
    ADD COLUMN alt_phones VARCHAR(512) NULL,
    ADD COLUMN location_url VARCHAR(1024) NULL;
