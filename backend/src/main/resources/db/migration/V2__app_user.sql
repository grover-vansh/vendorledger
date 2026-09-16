CREATE TABLE app_user (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(16) NOT NULL,
    supplier_id     BIGINT REFERENCES supplier (id),
    buyer_id        BIGINT REFERENCES buyer (id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (role IN ('BUYER', 'SELLER', 'ADMIN')),
    CHECK (
        (role = 'SELLER' AND supplier_id IS NOT NULL AND buyer_id IS NULL)
        OR (role = 'BUYER' AND buyer_id IS NOT NULL AND supplier_id IS NULL)
        OR (role = 'ADMIN' AND supplier_id IS NULL AND buyer_id IS NULL)
    )
);

CREATE INDEX idx_app_user_supplier ON app_user (supplier_id);
CREATE INDEX idx_app_user_buyer ON app_user (buyer_id);
