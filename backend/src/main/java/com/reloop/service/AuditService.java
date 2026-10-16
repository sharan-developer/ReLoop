package com.reloop.service;

import com.reloop.entity.AuditLog;
import com.reloop.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditLogRepository repo;

    public void log(Long actorId, String action, String entityType, Long entityId, String details) {
        AuditLog a = new AuditLog();
        a.setActorId(actorId);
        a.setAction(action);
        a.setEntityType(entityType);
        a.setEntityId(entityId);
        a.setDetails(details != null && details.length() > 500 ? details.substring(0, 500) : details);
        repo.save(a);
    }
}
