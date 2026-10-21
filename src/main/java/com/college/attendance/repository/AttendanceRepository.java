package com.college.attendance.repository;

import com.college.attendance.model.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRepository extends JpaRepository<AttendanceRecord, Long> {

    boolean existsByStudentIdAndSessionId(Long studentId, Long sessionId);

    long countBySessionId(Long sessionId);
    java.util.List<AttendanceRecord> findBySessionId(Long sessionId);
}