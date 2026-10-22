package com.college.attendance.controller;

import com.college.attendance.model.User;
import com.college.attendance.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class FaceController {

    private final UserRepository userRepository;

    public FaceController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/student/face-data")
    public Map<String, Object> faceData(Authentication auth) {
        User me = userRepository.findAll().stream()
                .filter(u -> auth.getName().equalsIgnoreCase(u.getEmail()))
                .findFirst().orElse(null);
        String fd = (me == null) ? null : me.getFaceData();
        Map<String, Object> out = new HashMap<>();
        out.put("hasFace", fd != null && !fd.isBlank());
        out.put("faceData", fd);
        return out;
    }
}
