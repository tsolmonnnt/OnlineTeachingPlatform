ALTER TABLE teaching_materials
    ADD COLUMN IF NOT EXISTS cloudinary_resource_type VARCHAR(20);

ALTER TABLE teaching_materials
    ADD COLUMN IF NOT EXISTS cloudinary_delivery_type VARCHAR(20);

UPDATE teaching_materials
SET cloudinary_resource_type = CASE
        WHEN content_type LIKE 'image/%' THEN 'image'
        WHEN content_type LIKE 'video/%' THEN 'video'
        WHEN content_type IS NOT NULL AND content_type <> '' THEN 'raw'
        ELSE 'image'
    END,
    cloudinary_delivery_type = 'upload'
WHERE cloudinary_resource_type IS NULL;
