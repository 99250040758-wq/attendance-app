package com.college.attendance.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        model.addAttribute("email", auth.getName());
        model.addAttribute("role", auth.getAuthorities().iterator().next()
                .getAuthority().replace("ROLE_", ""));
        return "dashboard";
    }
}