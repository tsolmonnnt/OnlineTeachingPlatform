package com.tsolmon.online_teaching_platform.dashboard.application;

import com.tsolmon.online_teaching_platform.auth.domain.Role;
import com.tsolmon.online_teaching_platform.booking.domain.BookingRepository;
import com.tsolmon.online_teaching_platform.dashboard.api.dto.PublicPlatformStatsResponse;
import com.tsolmon.online_teaching_platform.teacher.domain.TeacherRepository;
import com.tsolmon.online_teaching_platform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PublicStatsService {
    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final BookingRepository bookingRepository;

    @Transactional(readOnly = true)
    public PublicPlatformStatsResponse getPlatformStats() {
        return new PublicPlatformStatsResponse(
                teacherRepository.countByVerifiedTrue(),
                bookingRepository.count(),
                userRepository.countByRole(Role.STUDENT),
                teacherRepository.count()
        );
    }
}
