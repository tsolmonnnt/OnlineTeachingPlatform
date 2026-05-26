-- Optional FKs to course_subjects for schedule, bookings, materials, quizzes (PostgreSQL).
-- Guard each ALTER so this migration no-ops when tables do not exist yet (greenfield uses Hibernate bootstrap first).

DO $$
BEGIN
  IF EXISTS (
      SELECT 1 FROM information_schema.tables
      WHERE table_schema = current_schema() AND table_name = 'teacher_availability_slots'
  ) THEN
    ALTER TABLE teacher_availability_slots ADD COLUMN IF NOT EXISTS course_subject_id BIGINT REFERENCES course_subjects(id);
  END IF;
END $$;

DO $$
BEGIN
  IF EXISTS (
      SELECT 1 FROM information_schema.tables
      WHERE table_schema = current_schema() AND table_name = 'bookings'
  ) THEN
    ALTER TABLE bookings ADD COLUMN IF NOT EXISTS course_subject_id BIGINT REFERENCES course_subjects(id);
  END IF;
END $$;

DO $$
BEGIN
  IF EXISTS (
      SELECT 1 FROM information_schema.tables
      WHERE table_schema = current_schema() AND table_name = 'teaching_materials'
  ) THEN
    ALTER TABLE teaching_materials ADD COLUMN IF NOT EXISTS course_subject_id BIGINT REFERENCES course_subjects(id);
  END IF;
END $$;

DO $$
BEGIN
  IF EXISTS (
      SELECT 1 FROM information_schema.tables
      WHERE table_schema = current_schema() AND table_name = 'quizzes'
  ) THEN
    ALTER TABLE quizzes ADD COLUMN IF NOT EXISTS course_subject_id BIGINT REFERENCES course_subjects(id);
  END IF;
END $$;

DO $$
BEGIN
  IF EXISTS (
      SELECT 1 FROM information_schema.tables
      WHERE table_schema = current_schema() AND table_name = 'bookings'
  ) AND EXISTS (
      SELECT 1 FROM information_schema.tables
      WHERE table_schema = current_schema() AND table_name = 'teacher_availability_slots'
  ) THEN
    UPDATE bookings b
    SET course_subject_id = s.course_subject_id
    FROM teacher_availability_slots s
    WHERE b.availability_slot_id = s.id
      AND b.course_subject_id IS NULL
      AND s.course_subject_id IS NOT NULL;
  END IF;
END $$;
