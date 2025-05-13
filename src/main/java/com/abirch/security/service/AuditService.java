package com.abirch.security.service;

import com.abirch.security.user.HistoriqueAction;
import com.abirch.security.user.HistoriqueActionRepository;
import com.abirch.security.user.User;
import com.abirch.security.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final HistoriqueActionRepository historiqueRepo;
    private final UserRepository userRepository;

    public void logAction(String email, String action) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        HistoriqueAction log = new HistoriqueAction();
        log.setUtilisateur(user);
        log.setAction(action);
        log.setDate(LocalDateTime.now());

        historiqueRepo.save(log);
    }

    public List<HistoriqueAction> getAllLogs() {
        return historiqueRepo.findAll();
    }
}
