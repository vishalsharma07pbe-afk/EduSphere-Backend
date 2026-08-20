ALTER TABLE school_provisioning
    ADD COLUMN authority_first_name VARCHAR(100),
    ADD COLUMN authority_middle_name VARCHAR(100),
    ADD COLUMN authority_last_name VARCHAR(100);

-- Preserve existing data:
-- the first word becomes the first name;
-- the remaining text becomes the last name.
UPDATE school_provisioning
SET authority_first_name =
        LEFT(
                SPLIT_PART(TRIM(authority_name), ' ', 1),
                100
        ),
    authority_last_name =
        NULLIF(
                LEFT(
                        REGEXP_REPLACE(
                                TRIM(authority_name),
                                '^[^[:space:]]+[[:space:]]*',
                                ''
                        ),
                        100
                ),
                ''
        );

ALTER TABLE school_provisioning
    ALTER COLUMN authority_first_name SET NOT NULL;

ALTER TABLE school_provisioning
    DROP COLUMN authority_name;

ALTER TABLE school_provisioning
    ADD CONSTRAINT ck_school_provisioning_authority_first_name
        CHECK (BTRIM(authority_first_name) <> '');