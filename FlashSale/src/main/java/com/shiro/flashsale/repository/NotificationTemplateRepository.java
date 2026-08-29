package com.shiro.flashsale.repository;

import com.shiro.flashsale.entity.NotificationTemplate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {
  Optional<NotificationTemplate> findByCode(String code);
}
