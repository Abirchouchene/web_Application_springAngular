package com.abirch.security.controller;

import com.abirch.security.service.AuditService;
import com.abirch.security.user.HistoriqueAction;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class HistoriqueActionController {

    private final AuditService auditService;

    @GetMapping
    public List<HistoriqueAction> getAllLogs() {
        return auditService.getAllLogs();
    }
}

