-- Repair existing PostgreSQL databases before Hibernate ddl-auto=update adds NOT NULL/FK constraints.
-- No-ops on fresh installs (tables absent). Tests use H2 with Flyway disabled.

DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = current_schema() AND table_name = 'quiz_questions'
  ) AND EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = current_schema() AND table_name = 'quizzes'
  ) THEN
    DELETE FROM quiz_questions qq
    WHERE qq.quiz_id IS NULL
       OR NOT EXISTS (SELECT 1 FROM quizzes q WHERE q.id = qq.quiz_id);
  END IF;
END $$;

DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = current_schema() AND table_name = 'quiz_questions'
  ) THEN
    IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = current_schema() AND table_name = 'quiz_questions' AND column_name = 'correct_answer'
    ) THEN
      ALTER TABLE quiz_questions ADD COLUMN correct_answer varchar(2000);
    END IF;
    UPDATE quiz_questions SET correct_answer = '' WHERE correct_answer IS NULL;
    ALTER TABLE quiz_questions ALTER COLUMN correct_answer SET NOT NULL;

    IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = current_schema() AND table_name = 'quiz_questions' AND column_name = 'question_type'
    ) THEN
      ALTER TABLE quiz_questions ADD COLUMN question_type varchar(32);
    END IF;
    UPDATE quiz_questions SET question_type = 'MCQ' WHERE question_type IS NULL;
    ALTER TABLE quiz_questions ALTER COLUMN question_type SET NOT NULL;
  END IF;
END $$;

DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = current_schema() AND table_name = 'teacher_profiles'
  ) THEN
    IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = current_schema() AND table_name = 'teacher_profiles' AND column_name = 'verified'
    ) THEN
      ALTER TABLE teacher_profiles ADD COLUMN verified boolean;
    END IF;
    UPDATE teacher_profiles SET verified = false WHERE verified IS NULL;
    ALTER TABLE teacher_profiles ALTER COLUMN verified SET NOT NULL;
  END IF;
END $$;
