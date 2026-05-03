package com.tsolmon.online_teaching_platform.dashboard.api;

import com.tsolmon.online_teaching_platform.dashboard.api.dto.PublicPlatformStatsResponse;
import com.tsolmon.online_teaching_platform.dashboard.application.PublicStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicStatsController {
    private final PublicStatsService publicStatsService;

    @GetMapping("/stats")
    public PublicPlatformStatsResponse platformStats() {
        return publicStatsService.getPlatformStats();
    }
}
