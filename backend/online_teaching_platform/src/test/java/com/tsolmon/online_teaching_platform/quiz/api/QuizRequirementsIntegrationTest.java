package com.tsolmon.online_teaching_platform.quiz.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.tsolmon.online_teaching_platform.RequirementsIntegrationTestSupport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QuizRequirementsIntegrationTest extends RequirementsIntegrationTestSupport {

    @Test
    void fr14Fr15Fr16QuizShouldCreatePublishAndScoreStudentAttempt() throws Exception {
        ConfirmedCourseAccess access = createConfirmedCourseAccess("quiz");

        String quizBody = exchange("POST", "/api/quizzes", access.teacherToken(), """
                {
                  "courseSubjectId": %d,
                  "title": "Java Basics",
                  "description": "Core Java quiz",
                  "timeLimitMinutes": 15,
                  "questions": [
                    {
                      "type": "MCQ",
                      "prompt": "Which keyword creates a subclass?",
                      "optionsJson": "[\\"extends\\",\\"implements\\",\\"package\\"]",
                      "correctAnswer": "extends"
                    },
                    {
                      "type": "TRUE_FALSE",
                      "prompt": "Java is case sensitive.",
                      "correctAnswer": "true"
                    }
                  ]
                }
                """.formatted(access.courseSubjectId()));
        JsonNode quiz = objectMapper.readTree(quizBody);
        long quizId = quiz.get("id").asLong();
        assertThat(quiz.get("published").asBoolean()).isTrue();
        assertThat(quiz.get("questionCount").asInt()).isEqualTo(2);

        String publicListBody = exchange("GET", "/api/quizzes/teacher/" + access.teacherProfileId() + "/published", null, null);
        assertThat(objectMapper.readTree(publicListBody)).hasSize(1);

        RegisteredUser noAccessStudent = registerUser("No Quiz Access", "quiz.no.access@test.mn", "STUDENT");
        assertThat(exchangeStatus("GET", "/api/quizzes/" + quizId + "/public", noAccessStudent.token(), null)).isEqualTo(403);

        // A confirmed-but-not-yet-started lesson must not grant quiz access.
        assertThat(exchangeStatus("GET", "/api/quizzes/" + quizId + "/public", access.studentToken(), null)).isEqualTo(403);

        // After attending the lesson (within the access window), the student can open the quiz.
        attendBookingNow(access.slotId());
        String publicQuizBody = exchange("GET", "/api/quizzes/" + quizId + "/public", access.studentToken(), null);
        JsonNode publicQuiz = objectMapper.readTree(publicQuizBody);
        JsonNode questions = publicQuiz.get("questions");
        assertThat(questions).hasSize(2);
        assertThat(questions.get(0).has("correctAnswer")).isFalse();

        String attemptBody = exchange("POST", "/api/quizzes/" + quizId + "/attempts", access.studentToken(), """
                {
                  "answers": [
                    {
                      "questionId": %d,
                      "answer": "extends"
                    },
                    {
                      "questionId": %d,
                      "answer": "false"
                    }
                  ]
                }
                """.formatted(
                questions.get(0).get("id").asLong(),
                questions.get(1).get("id").asLong()
        ));
        JsonNode attempt = objectMapper.readTree(attemptBody);
        assertThat(attempt.get("quizId").asLong()).isEqualTo(quizId);
        assertThat(attempt.get("score").asInt()).isEqualTo(1);
        assertThat(attempt.get("maxScore").asInt()).isEqualTo(2);
        assertThat(attempt.get("percent").asInt()).isEqualTo(50);
    }
}
