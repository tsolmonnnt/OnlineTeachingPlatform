ALTER TABLE teaching_materials
    ADD COLUMN IF NOT EXISTS cloudinary_version BIGINT;

UPDATE teaching_materials
SET cloudinary_version = CAST(
        SUBSTRING(secure_url FROM '/v([0-9]+)/')
    AS BIGINT)
WHERE cloudinary_version IS NULL
  AND secure_url ~ '/v[0-9]+/';
