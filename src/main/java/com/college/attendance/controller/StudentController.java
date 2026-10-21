package com.college.attendance.controller;

import com.college.attendance.model.AttendanceRecord;
import com.college.attendance.model.ClassSession;
import com.college.attendance.model.User;
import com.college.attendance.repository.AttendanceRepository;
import com.college.attendance.repository.ClassSessionRepository;
import com.college.attendance.repository.UserRepository;
import com.college.attendance.service.QrTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@Controller
@RequestMapping("/student")
public class StudentController {

    private static final double BASE_METERS = 25.0;
    private static final double MAX_ACCURACY_BONUS = 50.0;
    private static final double REJECT_ACCURACY = 100.0;

    private final UserRepository userRepository;
    private final ClassSessionRepository classSessionRepository;
    private final AttendanceRepository attendanceRepository;
    private final QrTokenService qrTokenService;

    public StudentController(UserRepository userRepository,
                             ClassSessionRepository classSessionRepository,
                             AttendanceRepository attendanceRepository,
                             QrTokenService qrTokenService) {
        this.userRepository = userRepository;
        this.classSessionRepository = classSessionRepository;
        this.attendanceRepository = attendanceRepository;
        this.qrTokenService = qrTokenService;
    }

    private static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    @GetMapping("/scan")
    public String scanPage() {
        return "student-scan";
    }

    @PostMapping("/mark")
    @ResponseBody
    public ResponseEntity<Map<String, String>> mark(@RequestParam String payload,
                                                    @RequestParam(required = false) Double latitude,
                                                    @RequestParam(required = false) Double longitude,
                                                    @RequestParam(required = false) Double accuracy,
                                                    Authentication auth) {
        User user = userRepository.findByEmail(auth.getName());
        if (user == null || !"STUDENT".equals(user.getRole())) {
            return ResponseEntity.status(403).body(Map.of("message", "Only students can mark attendance"));
        }

        Long sessionId;
        try {
            sessionId = Long.parseLong(payload.split("\\.")[0]);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "This is not an attendance QR"));
        }

        ClassSession session = classSessionRepository.findById(sessionId).orElse(null);
        if (session == null || !"ACTIVE".equals(session.getStatus())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Session is closed"));
        }

        if (!qrTokenService.isValid(payload, sessionId)) {
            return ResponseEntity.badRequest().body(Map.of("message", "QR expired, scan again"));
        }

        if (latitude == null || longitude == null || accuracy == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Location is required. Allow location and try again"));
        }
        if (session.getLatitude() == null || session.getLongitude() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "This session has no class location"));
        }
        double profAcc = session.getAccuracy() == null ? 0.0 : session.getAccuracy();
        if (accuracy > REJECT_ACCURACY || profAcc > REJECT_ACCURACY) {
            return ResponseEntity.badRequest().body(Map.of("message", "GPS accuracy too low, try again"));
        }
        double allowed = BASE_METERS
                + Math.min(accuracy, MAX_ACCURACY_BONUS)
                + Math.min(profAcc, MAX_ACCURACY_BONUS);
        double dist = distanceMeters(latitude, longitude, session.getLatitude(), session.getLongitude());
        if (dist > allowed) {
            return ResponseEntity.badRequest().body(Map.of("message", "You are too far from the class"));
        }

        if (attendanceRepository.existsByStudentIdAndSessionId(user.getId(), sessionId)) {
            return ResponseEntity.ok(Map.of("message", "Already marked present"));
        }

        AttendanceRecord record = new AttendanceRecord();
        record.setStudentId(user.getId());
        record.setSessionId(sessionId);
        record.setMarkedAt(LocalDateTime.now());
        attendanceRepository.save(record);

        return ResponseEntity.ok(Map.of("message", "Attendance marked for " + session.getSubject()));
    }
}