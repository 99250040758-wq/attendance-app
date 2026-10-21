package com.college.attendance.config;

import com.college.attendance.model.User;
import com.college.attendance.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner createProfessor(UserRepository users, PasswordEncoder encoder) {
        return args -> {
            if (users.findByEmail("prof@college.com") == null) {
                User p = new User();
                p.setName("Professor");
                p.setEmail("prof@college.com");
                p.setPassword(encoder.encode("prof1234"));
                p.setRole("TEACHER");
                users.save(p);
            }
        };
    }
}