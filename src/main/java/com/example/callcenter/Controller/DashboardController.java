package com.example.callcenter.Controller;

import com.example.callcenter.DTO.DashboardDTO;
import com.example.callcenter.Service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public ResponseEntity<DashboardDTO> getDashboardStats() {
        return ResponseEntity.ok(dashboardService.getDashboardStats());
    }

    /** Force a real‑time push of the dashboard to all WebSocket subscribers */
    @PostMapping("/refresh")
    public ResponseEntity<Void> refreshDashboard() {
        dashboardService.pushDashboardUpdate();
        return ResponseEntity.ok().build();
    }
}
