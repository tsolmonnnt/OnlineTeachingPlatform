package com.tsolmon.online_teaching_platform.admin.application;

import com.tsolmon.online_teaching_platform.admin.api.dto.AdminStatsResponse;
import com.tsolmon.online_teaching_platform.admin.api.dto.AdminTeacherRowResponse;
import com.tsolmon.online_teaching_platform.auth.domain.Role;
import com.tsolmon.online_teaching_platform.booking.domain.BookingRepository;
import com.tsolmon.online_teaching_platform.teacher.domain.TeacherProfile;
import com.tsolmon.online_teaching_platform.teacher.domain.TeacherRepository;
import com.tsolmon.online_teaching_platform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final BookingRepository bookingRepository;

    @Transactional(readOnly = true)
    public AdminStatsResponse getStats() {
        return new AdminStatsResponse(
                userRepository.count(),
                userRepository.countByRole(Role.STUDENT),
                userRepository.countByRole(Role.TEACHER),
                userRepository.countByRole(Role.ADMIN),
                bookingRepository.count(),
                teacherRepository.countByVerifiedTrue(),
                teacherRepository.count()
        );
    }

    @Transactional(readOnly = true)
    public List<AdminTeacherRowResponse> listTeachers() {
        return teacherRepository.findAll().stream()
                .map(AdminTeacherRowResponse::from)
                .toList();
    }

    @Transactional
    public AdminTeacherRowResponse setTeacherVerified(Long teacherProfileId, boolean verified) {
        TeacherProfile profile = teacherRepository.findById(teacherProfileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher profile not found"));
        profile.setVerified(verified);
        return AdminTeacherRowResponse.from(teacherRepository.save(profile));
    }
}
