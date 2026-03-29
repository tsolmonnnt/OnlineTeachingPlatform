package com.tsolmon.online_teaching_platform.booking.api;

import com.tsolmon.online_teaching_platform.auth.domain.AuthUser;
import com.tsolmon.online_teaching_platform.booking.api.dto.BookingResponse;
import com.tsolmon.online_teaching_platform.booking.api.dto.CreateBookingRequest;
import com.tsolmon.online_teaching_platform.booking.application.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;

    @PostMapping
    public BookingResponse createBooking(Authentication authentication, @Valid @RequestBody CreateBookingRequest request) {
        AuthUser authUser = (AuthUser) authentication.getPrincipal();
        return bookingService.createBooking(authUser, request);
    }

    @GetMapping("/me")
    public List<BookingResponse> myBookings(Authentication authentication) {
        AuthUser authUser = (AuthUser) authentication.getPrincipal();
        return bookingService.myBookings(authUser);
    }

    @PatchMapping("/{bookingId}/confirm")
    public BookingResponse confirmBooking(Authentication authentication, @PathVariable Long bookingId) {
        AuthUser authUser = (AuthUser) authentication.getPrincipal();
        return bookingService.confirmBooking(authUser, bookingId);
    }

    @PatchMapping("/{bookingId}/cancel")
    public BookingResponse cancelBooking(Authentication authentication, @PathVariable Long bookingId) {
        AuthUser authUser = (AuthUser) authentication.getPrincipal();
        return bookingService.cancelBooking(authUser, bookingId);
    }
}

