package com.college.attendance.controller;

import com.college.attendance.model.ClassSession;
import com.college.attendance.model.User;
import com.college.attendance.repository.AttendanceRepository;
import com.college.attendance.repository.ClassSessionRepository;
import com.college.attendance.repository.UserRepository;
import com.college.attendance.service.QrTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/teacher")
public class TeacherController {

    private final UserRepository userRepository;
    private final ClassSessionRepository classSessionRepository;
    private final AttendanceRepository attendanceRepository;
    private final QrTokenService qrTokenService;

    public TeacherController(UserRepository userRepository,
                             ClassSessionRepository classSessionRepository,
                             AttendanceRepository attendanceRepository,
                             QrTokenService qrTokenService) {
        this.userRepository = userRepository;
        this.classSessionRepository = classSessionRepository;
        this.attendanceRepository = attendanceRepository;
        this.qrTokenService = qrTokenService;
    }

    private ClassSession ownSession(Long id, Authentication auth) {
        ClassSession s = classSessionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        User me = userRepository.findByEmail(auth.getName());
        if (me == null || !me.getId().equals(s.getProfessorId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return s;
    }

    @GetMapping
    public String home() {
        return "teacher";
    }

    @PostMapping("/start")
    public String start(@RequestParam String subject,
                        @RequestParam(required = false) Double latitude,
                        @RequestParam(required = false) Double longitude,
                        @RequestParam(required = false) Double accuracy,
                        Authentication auth) {
        if (latitude == null || longitude == null) {
            return "redirect:/teacher";
        }
        User me = userRepository.findByEmail(auth.getName());
        ClassSession s = new ClassSession();
        s.setProfessorId(me.getId());
        s.setSubject(subject);
        s.setStartTime(LocalDateTime.now());
        s.setStatus("ACTIVE");
        s.setLatitude(latitude);
        s.setLongitude(longitude);
        s.setAccuracy(accuracy);
        classSessionRepository.save(s);
        return "redirect:/teacher/session/" + s.getId();
    }

    @GetMapping("/session/{id}")
    public String session(@PathVariable Long id, Authentication auth, Model model) {
        model.addAttribute("classSession", ownSession(id, auth));
        return "teacher-session";
    }

    @GetMapping("/session/{id}/qr")
    @ResponseBody
    public Map<String, Object> qr(@PathVariable Long id, Authentication auth) {
        ClassSession s = ownSession(id, auth);
        boolean active = "ACTIVE".equals(s.getStatus());
        Map<String, Object> out = new HashMap<>();
        out.put("active", active);
        if (active) {
            out.put("payload", qrTokenService.currentPayload(id));
        }
        out.put("count", attendanceRepository.countBySessionId(id));
        return out;
    }

    @PostMapping("/session/{id}/close")
    public String close(@PathVariable Long id, Authentication auth) {
        ClassSession s = ownSession(id, auth);
        s.setStatus("CLOSED");
        classSessionRepository.save(s);
        return "redirect:/teacher/session/" + id;
    }

    @GetMapping("/session/{id}/list")
    @ResponseBody
    public java.util.List<Map<String, Object>> list(@PathVariable Long id, Authentication auth) {
        ownSession(id, auth);
        java.util.List<Map<String, Object>> out = new java.util.ArrayList<>();
        for (com.college.attendance.model.AttendanceRecord r : attendanceRepository.findBySessionId(id)) {
            User u = userRepository.findById(r.getStudentId()).orElse(null);
            Map<String, Object> row = new HashMap<>();
            row.put("name", u == null ? "Unknown" : u.getName());
            row.put("roll", u == null ? "" : u.getRollNumber());
            row.put("time", String.valueOf(r.getMarkedAt()));
            out.add(row);
        }
        return out;
    }
}