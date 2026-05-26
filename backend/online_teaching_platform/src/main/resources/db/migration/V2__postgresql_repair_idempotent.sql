-- Idempotent repair for PostgreSQL when V1 was marked applied but data/schema drifted,
-- or when search_path/schema checks caused earlier scripts to no-op. Uses public schema explicitly.

DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = 'public' AND table_name = 'quiz_questions'
  ) AND EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = 'public' AND table_name = 'quizzes'
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
    WHERE table_schema = 'public' AND table_name = 'quiz_questions'
  ) THEN
    IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = 'public' AND table_name = 'quiz_questions' AND column_name = 'correct_answer'
    ) THEN
      ALTER TABLE quiz_questions ADD COLUMN correct_answer varchar(2000);
    END IF;
    UPDATE quiz_questions SET correct_answer = '' WHERE correct_answer IS NULL;
    ALTER TABLE quiz_questions ALTER COLUMN correct_answer SET NOT NULL;

    IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = 'public' AND table_name = 'quiz_questions' AND column_name = 'question_type'
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
    WHERE table_schema = 'public' AND table_name = 'teacher_profiles'
  ) THEN
    IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = 'public' AND table_name = 'teacher_profiles' AND column_name = 'verified'
    ) THEN
      ALTER TABLE teacher_profiles ADD COLUMN verified boolean;
    END IF;
    UPDATE teacher_profiles SET verified = false WHERE verified IS NULL;
    ALTER TABLE teacher_profiles ALTER COLUMN verified SET NOT NULL;
  END IF;
END $$;
