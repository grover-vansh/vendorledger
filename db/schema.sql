-- MSME pay-due tracker — Postgres schema (seller catalog + invoices)
-- Run: psql -U postgres -d paydue -f db/schema.sql

CREATE TABLE supplier (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    email           VARCHAR(255),
    gstin           VARCHAR(15),
    msme            BOOLEAN NOT NULL DEFAULT TRUE,
    registered_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE buyer (
    id               BIGSERIAL PRIMARY KEY,
    supplier_id      BIGINT NOT NULL REFERENCES supplier (id),
    name             VARCHAR(200) NOT NULL,
    email            VARCHAR(255),
    gstin            VARCHAR(15),
    phone            VARCHAR(20),
    contact_name     VARCHAR(200),
    billing_address  VARCHAR(500),
    registered_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_buyer_supplier ON buyer (supplier_id);
CREATE UNIQUE INDEX uq_buyer_supplier_name ON buyer (supplier_id, LOWER(name));
CREATE UNIQUE INDEX uq_buyer_supplier_email ON buyer (supplier_id, LOWER(email)) WHERE email IS NOT NULL;
CREATE UNIQUE INDEX uq_buyer_supplier_gstin ON buyer (supplier_id, gstin) WHERE gstin IS NOT NULL;

CREATE TABLE product (
    id              BIGSERIAL PRIMARY KEY,
    supplier_id     BIGINT NOT NULL REFERENCES supplier (id),
    sku_code        VARCHAR(64) NOT NULL,
    name            VARCHAR(200) NOT NULL,
    product_type    VARCHAR(32) NOT NULL DEFAULT 'OTHER',
    shelf_life_days INTEGER,
    registered_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (supplier_id, sku_code),
    CHECK (shelf_life_days IS NULL OR shelf_life_days > 0)
);

CREATE INDEX idx_product_supplier ON product (supplier_id);

CREATE TABLE invoice (
    id              BIGSERIAL PRIMARY KEY,
    supplier_id     BIGINT NOT NULL REFERENCES supplier (id),
    buyer_id        BIGINT NOT NULL REFERENCES buyer (id),
    invoice_number  VARCHAR(64) NOT NULL,
    invoice_date    DATE NOT NULL,
    due_date        DATE,
    status          VARCHAR(16) NOT NULL DEFAULT 'OPEN',
    UNIQUE (supplier_id, invoice_number),
    CHECK (due_date IS NULL OR due_date >= invoice_date),
    CHECK (status IN ('OPEN', 'DUE_SOON', 'OVERDUE', 'NOTICED', 'PAID'))
);

CREATE INDEX idx_invoice_supplier ON invoice (supplier_id);
CREATE INDEX idx_invoice_buyer ON invoice (buyer_id);
CREATE INDEX idx_invoice_status ON invoice (status);
CREATE INDEX idx_invoice_due_date ON invoice (due_date);

CREATE TABLE invoice_line (
    id              BIGSERIAL PRIMARY KEY,
    invoice_id      BIGINT NOT NULL REFERENCES invoice (id) ON DELETE CASCADE,
    product_id      BIGINT REFERENCES product (id) ON DELETE SET NULL,
    sku_code        VARCHAR(64) NOT NULL,
    description     VARCHAR(200) NOT NULL,
    quantity        NUMERIC(12, 3) NOT NULL,
    unit_price      NUMERIC(12, 2) NOT NULL,
    line_total      NUMERIC(12, 2) NOT NULL,
    CHECK (quantity > 0),
    CHECK (unit_price >= 0),
    CHECK (line_total = ROUND(quantity * unit_price, 2))
);

CREATE INDEX idx_invoice_line_invoice ON invoice_line (invoice_id);
CREATE INDEX idx_invoice_line_product ON invoice_line (product_id);

-- If due_date is omitted, default to invoice_date + 45 days (MSME demo rule)
CREATE OR REPLACE FUNCTION set_invoice_due_date()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.due_date IS NULL THEN
        NEW.due_date := NEW.invoice_date + 45;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_invoice_due_date
BEFORE INSERT OR UPDATE OF invoice_date, due_date ON invoice
FOR EACH ROW
EXECUTE FUNCTION set_invoice_due_date();

CREATE TABLE app_user (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(16) NOT NULL,
    supplier_id     BIGINT REFERENCES supplier (id),
    company_name    VARCHAR(200),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (role IN ('BUYER', 'SELLER', 'ADMIN')),
    CHECK (
        (role = 'SELLER' AND supplier_id IS NOT NULL)
        OR (role = 'BUYER' AND supplier_id IS NULL)
        OR (role = 'ADMIN' AND supplier_id IS NULL)
    )
);

CREATE INDEX idx_app_user_supplier ON app_user (supplier_id);
