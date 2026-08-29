package com.shiro.flashsale.repository;

import com.shiro.flashsale.entity.InventoryMovement;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, UUID> {

  boolean existsByEventId(UUID eventId);

  List<InventoryMovement> findByProductIdOrderByCreatedAtDesc(UUID productId, Pageable pageable);
}
