package com.example.callcenter.Repository;

import com.example.callcenter.Entity.Report;
import com.example.callcenter.Entity.Request;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {
    Optional<Report> findByRequest(Request request);
}