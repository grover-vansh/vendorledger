ALTER TABLE buyer
    ADD COLUMN supplier_id BIGINT REFERENCES supplier (id),
    ADD COLUMN phone VARCHAR(20),
    ADD COLUMN contact_name VARCHAR(200),
    ADD COLUMN billing_address VARCHAR(500);

ALTER TABLE app_user
    ADD COLUMN company_name VARCHAR(200);

UPDATE app_user u
SET company_name = s.name
FROM supplier s
WHERE u.supplier_id = s.id;

UPDATE app_user u
SET company_name = b.name
FROM buyer b
WHERE u.buyer_id = b.id;

DO $$
DECLARE
    rec RECORD;
    new_buyer_id BIGINT;
    owner_id BIGINT;
BEGIN
    FOR rec IN
        SELECT sb.supplier_id, sb.buyer_id
        FROM supplier_buyer sb
        ORDER BY sb.buyer_id, sb.id
    LOOP
        SELECT supplier_id INTO owner_id FROM buyer WHERE id = rec.buyer_id;
        IF owner_id IS NULL THEN
            UPDATE buyer SET supplier_id = rec.supplier_id WHERE id = rec.buyer_id;
        ELSIF owner_id <> rec.supplier_id THEN
            INSERT INTO buyer (name, email, gstin, registered_at, supplier_id, phone, contact_name, billing_address)
            SELECT name, email, gstin, registered_at, rec.supplier_id, phone, contact_name, billing_address
            FROM buyer
            WHERE id = rec.buyer_id
            RETURNING id INTO new_buyer_id;

            UPDATE invoice
            SET buyer_id = new_buyer_id
            WHERE supplier_id = rec.supplier_id
              AND buyer_id = rec.buyer_id;
        END IF;
    END LOOP;
END $$;

DO $$
DECLARE
    rec RECORD;
    new_buyer_id BIGINT;
    owner_id BIGINT;
BEGIN
    FOR rec IN
        SELECT DISTINCT i.supplier_id, i.buyer_id
        FROM invoice i
        ORDER BY i.buyer_id, i.supplier_id
    LOOP
        SELECT supplier_id INTO owner_id FROM buyer WHERE id = rec.buyer_id;
        IF owner_id IS NULL THEN
            UPDATE buyer SET supplier_id = rec.supplier_id WHERE id = rec.buyer_id;
        ELSIF owner_id <> rec.supplier_id THEN
            INSERT INTO buyer (name, email, gstin, registered_at, supplier_id, phone, contact_name, billing_address)
            SELECT name, email, gstin, registered_at, rec.supplier_id, phone, contact_name, billing_address
            FROM buyer
            WHERE id = rec.buyer_id
            RETURNING id INTO new_buyer_id;

            UPDATE invoice
            SET buyer_id = new_buyer_id
            WHERE supplier_id = rec.supplier_id
              AND buyer_id = rec.buyer_id;
        END IF;
    END LOOP;
END $$;

DO $$
DECLARE
    cname text;
BEGIN
    FOR cname IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'app_user'::regclass
          AND contype = 'c'
    LOOP
        EXECUTE format('ALTER TABLE app_user DROP CONSTRAINT %I', cname);
    END LOOP;
END $$;

ALTER TABLE app_user DROP CONSTRAINT IF EXISTS app_user_buyer_id_fkey;
ALTER TABLE app_user DROP COLUMN IF EXISTS buyer_id;

ALTER TABLE app_user
    ADD CONSTRAINT app_user_role_check CHECK (role IN ('BUYER', 'SELLER', 'ADMIN'));

ALTER TABLE app_user
    ADD CONSTRAINT app_user_party_check CHECK (
        (role = 'SELLER' AND supplier_id IS NOT NULL)
        OR (role = 'BUYER' AND supplier_id IS NULL)
        OR (role = 'ADMIN' AND supplier_id IS NULL)
    );

DELETE FROM buyer
WHERE supplier_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM invoice i WHERE i.buyer_id = buyer.id);

ALTER TABLE buyer
    ALTER COLUMN supplier_id SET NOT NULL;

WITH named AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY supplier_id, LOWER(name) ORDER BY id) AS rn
    FROM buyer
)
UPDATE buyer b
SET name = b.name || ' (' || named.rn || ')'
FROM named
WHERE b.id = named.id
  AND named.rn > 1;

WITH emailed AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY supplier_id, LOWER(email) ORDER BY id) AS rn
    FROM buyer
    WHERE email IS NOT NULL
)
UPDATE buyer b
SET email = split_part(b.email, '@', 1) || '+' || emailed.rn || '@' || split_part(b.email, '@', 2)
FROM emailed
WHERE b.id = emailed.id
  AND emailed.rn > 1;

WITH gstinned AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY supplier_id, gstin ORDER BY id) AS rn
    FROM buyer
    WHERE gstin IS NOT NULL
)
UPDATE buyer b
SET gstin = NULL
FROM gstinned
WHERE b.id = gstinned.id
  AND gstinned.rn > 1;

CREATE INDEX idx_buyer_supplier ON buyer (supplier_id);
CREATE UNIQUE INDEX uq_buyer_supplier_name ON buyer (supplier_id, LOWER(name));
CREATE UNIQUE INDEX uq_buyer_supplier_email ON buyer (supplier_id, LOWER(email)) WHERE email IS NOT NULL;
CREATE UNIQUE INDEX uq_buyer_supplier_gstin ON buyer (supplier_id, gstin) WHERE gstin IS NOT NULL;

DROP TABLE IF EXISTS supplier_buyer;
