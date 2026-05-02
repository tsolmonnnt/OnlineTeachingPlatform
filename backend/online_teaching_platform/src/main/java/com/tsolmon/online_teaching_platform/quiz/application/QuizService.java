package com.tsolmon.online_teaching_platform.quiz.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsolmon.online_teaching_platform.auth.domain.AuthUser;
import com.tsolmon.online_teaching_platform.auth.domain.Role;
import com.tsolmon.online_teaching_platform.booking.application.CourseAccessService;
import com.tsolmon.online_teaching_platform.course.domain.CourseSubject;
import com.tsolmon.online_teaching_platform.course.domain.CourseSubjectRepository;
import com.tsolmon.online_teaching_platform.quiz.api.dto.*;
import com.tsolmon.online_teaching_platform.quiz.domain.*;
import com.tsolmon.online_teaching_platform.teacher.domain.TeacherProfile;
import com.tsolmon.online_teaching_platform.teacher.domain.TeacherRepository;
import com.tsolmon.online_teaching_platform.user.entity.User;
import com.tsolmon.online_teaching_platform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizService {
    private final QuizRepository quizRepository;
    private final QuizAttemptRepository attemptRepository;
    private final TeacherRepository teacherRepository;
    private final CourseSubjectRepository courseSubjectRepository;
    private final CourseAccessService courseAccessService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public QuizSummaryResponse create(AuthUser authUser, CreateQuizRequest request) {
        TeacherProfile teacher = teacherRepository.findByUser_Id(authUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher profile not found"));

        if (request.questions().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Add at least one question");
        }

        CourseSubject courseSubject = courseSubjectRepository.findById(request.courseSubjectId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown course subject"));
        if (!teacherOffersSubject(teacher, courseSubject)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Add this subject to your profile before creating a quiz for it");
        }

        Quiz quiz = new Quiz();
        quiz.setTeacherProfile(teacher);
        quiz.setCourseSubject(courseSubject);
        quiz.setTitle(request.title().trim());
        quiz.setDescription(request.description() != null ? request.description().trim() : null);
        quiz.setTimeLimitMinutes(request.timeLimitMinutes());

        for (int i = 0; i < request.questions().size(); i++) {
            CreateQuestionRequest q = request.questions().get(i);
            validateQuestionRequest(q);
            QuizQuestion entity = new QuizQuestion();
            entity.setQuiz(quiz);
            entity.setOrderIndex(i);
            entity.setQuestionType(q.type());
            entity.setPrompt(q.prompt().trim());
            entity.setOptionsJson(q.optionsJson() != null ? q.optionsJson().trim() : null);
            entity.setCorrectAnswer(q.correctAnswer().trim());
            quiz.getQuestions().add(entity);
        }

        Quiz saved = quizRepository.save(quiz);
        return QuizSummaryResponse.from(reloadWithQuestions(saved.getId()));
    }

    private void validateQuestionRequest(CreateQuestionRequest q) {
        if (q.type() == QuestionType.MCQ && (q.optionsJson() == null || q.optionsJson().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MCQ questions require optionsJson");
        }
    }

    @Transactional(readOnly = true)
    public List<QuizSummaryResponse> listMine(AuthUser authUser) {
        TeacherProfile teacher = teacherRepository.findByUser_Id(authUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher profile not found"));
        return quizRepository.findByTeacherProfile_IdOrderByCreatedAtDesc(teacher.getId()).stream()
                .map(this::ensureQuestionCount)
                .map(QuizSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<QuizSummaryResponse> listPublishedForTeacher(Long teacherProfileId) {
        return quizRepository
                .findByTeacherProfile_IdAndPublishedIsTrueOrderByCreatedAtDesc(teacherProfileId)
                .stream()
                .map(this::ensureQuestionCount)
                .map(QuizSummaryResponse::from)
                .toList();
    }

    private static boolean teacherOffersSubject(TeacherProfile teacher, CourseSubject subject) {
        return teacher.getSubjects().stream().anyMatch(s -> s.equalsIgnoreCase(subject.getName()));
    }

    @Transactional(readOnly = true)
    public QuizPublicResponse getPublishedQuiz(Long quizId, AuthUser user) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found"));
        if (!quiz.isPublished()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found");
        }
        Quiz full = reloadWithQuestions(quizId);
        assertCanViewPublishedQuiz(user, full);
        List<QuizQuestionPublicDto> qs = full.getQuestions().stream()
                .map(q -> new QuizQuestionPublicDto(
                        q.getId(),
                        q.getOrderIndex(),
                        q.getQuestionType(),
                        q.getPrompt(),
                        q.getOptionsJson()
                ))
                .toList();
        return new QuizPublicResponse(
                full.getId(),
                full.getTeacherProfile().getId(),
                full.getTitle(),
                full.getDescription(),
                full.getTimeLimitMinutes(),
                qs
        );
    }

    private void assertCanViewPublishedQuiz(AuthUser user, Quiz quiz) {
        if (user.role() == Role.ADMIN) {
            return;
        }
        if (user.role() == Role.TEACHER && quiz.getTeacherProfile().getUser().getId().equals(user.id())) {
            return;
        }
        if (user.role() != Role.STUDENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot access this quiz");
        }
        assertStudentCourseAccess(user, quiz);
    }

    private void assertStudentCourseAccess(AuthUser student, Quiz quiz) {
        if (quiz.getCourseSubject() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Quiz is not linked to a course");
        }
        if (!courseAccessService.hasConfirmedAccess(
                student.id(),
                quiz.getTeacherProfile().getId(),
                quiz.getCourseSubject().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You need a confirmed booking for this course with this teacher"
            );
        }
    }

    @Transactional(readOnly = true)
    public QuizTeacherDetailResponse getTeacherQuizDetail(AuthUser authUser, Long quizId) {
        TeacherProfile teacher = teacherRepository.findByUser_Id(authUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher profile not found"));
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found"));
        if (!quiz.getTeacherProfile().getId().equals(teacher.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your quiz");
        }
        Quiz full = reloadWithQuestions(quizId);
        List<QuizTeacherDetailResponse.QuizQuestionTeacherDto> qs = full.getQuestions().stream()
                .map(q -> new QuizTeacherDetailResponse.QuizQuestionTeacherDto(
                        q.getId(),
                        q.getOrderIndex(),
                        q.getQuestionType(),
                        q.getPrompt(),
                        q.getOptionsJson(),
                        q.getCorrectAnswer()
                ))
                .toList();
        return new QuizTeacherDetailResponse(
                full.getId(),
                full.getTitle(),
                full.getDescription(),
                full.getTimeLimitMinutes(),
                full.isPublished(),
                full.getCreatedAt(),
                qs
        );
    }

    @Transactional
    public void delete(AuthUser authUser, Long quizId) {
        TeacherProfile teacher = teacherRepository.findByUser_Id(authUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher profile not found"));
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found"));
        if (!quiz.getTeacherProfile().getId().equals(teacher.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your quiz");
        }
        quizRepository.delete(quiz);
    }

    @Transactional
    public AttemptResultResponse submitAttempt(AuthUser authUser, Long quizId, SubmitAttemptRequest request) {
        if (authUser.role() != Role.STUDENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only students can submit quiz attempts");
        }

        Quiz quiz = reloadWithQuestions(quizId);
        if (!quiz.isPublished()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found");
        }
        assertStudentCourseAccess(authUser, quiz);

        User student = userRepository.findById(authUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Map<Long, String> answerByQuestionId = request.answers().stream()
                .collect(Collectors.toMap(
                        SubmitAttemptRequest.AnswerItem::questionId,
                        a -> a.answer() != null ? a.answer() : "",
                        (a, b) -> b
                ));

        int score = 0;
        int max = quiz.getQuestions().size();
        for (QuizQuestion q : quiz.getQuestions()) {
            String ans = answerByQuestionId.get(q.getId());
            if (isCorrect(q, ans)) {
                score++;
            }
        }

        QuizAttempt attempt = new QuizAttempt();
        attempt.setQuiz(quiz);
        attempt.setStudentUser(student);
        attempt.setScore(score);
        attempt.setMaxScore(max);
        try {
            attempt.setSubmittedAnswersJson(objectMapper.writeValueAsString(answerByQuestionId));
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store answers");
        }

        QuizAttempt saved = attemptRepository.save(attempt);
        int percent = max == 0 ? 0 : (int) Math.round(100.0 * score / max);
        return new AttemptResultResponse(saved.getId(), quiz.getId(), score, max, percent);
    }

    private boolean isCorrect(QuizQuestion q, String studentAnswer) {
        if (studentAnswer == null) {
            studentAnswer = "";
        }
        String correct = q.getCorrectAnswer();
        return switch (q.getQuestionType()) {
            case MCQ -> correct.trim().equals(studentAnswer.trim());
            case TRUE_FALSE -> correct.trim().equalsIgnoreCase(studentAnswer.trim());
            case SHORT_ANSWER -> correct.trim().equalsIgnoreCase(studentAnswer.trim());
        };
    }

    private Quiz reloadWithQuestions(Long id) {
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found"));
        quiz.getQuestions().size(); // fetch
        return quiz;
    }

    private Quiz ensureQuestionCount(Quiz q) {
        q.getQuestions().size();
        return q;
    }
}
