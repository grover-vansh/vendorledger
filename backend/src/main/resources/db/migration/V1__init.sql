CREATE TABLE supplier (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    email           VARCHAR(255),
    gstin           VARCHAR(15),
    msme            BOOLEAN NOT NULL DEFAULT TRUE,
    registered_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE buyer (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    email           VARCHAR(255),
    gstin           VARCHAR(15),
    registered_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE supplier_buyer (
    id              BIGSERIAL PRIMARY KEY,
    supplier_id     BIGINT NOT NULL REFERENCES supplier (id),
    buyer_id        BIGINT NOT NULL REFERENCES buyer (id),
    linked_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (supplier_id, buyer_id)
);

CREATE INDEX idx_supplier_buyer_supplier ON supplier_buyer (supplier_id);
CREATE INDEX idx_supplier_buyer_buyer ON supplier_buyer (buyer_id);

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
