CREATE TABLE organizations (
   id BIGINT PRIMARY KEY,
   school_code VARCHAR(50) NOT NULL,
   name VARCHAR(150) NOT NULL,
   email VARCHAR(150),
   status VARCHAR(30) NOT NULL,
   created_at TIMESTAMP WITH TIME ZONE
       NOT NULL DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP WITH TIME ZONE
       NOT NULL DEFAULT CURRENT_TIMESTAMP,
   CONSTRAINT uk_organizations_school_code
       UNIQUE (school_code),
   CONSTRAINT ck_organizations_id_positive
       CHECK (id > 0),
   CONSTRAINT ck_organizations_school_code_not_blank
       CHECK (BTRIM(school_code) <> ''),

   CONSTRAINT ck_organizations_name_not_blank
       CHECK (BTRIM(name) <> ''),

   CONSTRAINT ck_organizations_status
       CHECK (
           status IN (
                      'PROVISIONING',
                      'ACTIVE',
                      'INACTIVE'
               )
           )
);