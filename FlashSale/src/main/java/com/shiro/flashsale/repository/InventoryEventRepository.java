package com.shiro.flashsale.repository;

import com.shiro.flashsale.entity.InventoryEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryEventRepository extends JpaRepository<InventoryEvent, UUID> {

  /**
   * Claims a batch of due events for this instance.
   *
   * <p>{@code FOR UPDATE SKIP LOCKED} is what makes the worker multi-instance safe without any
   * leader election: rows locked by another instance are skipped rather than waited on, so every
   * instance makes progress and no event is ever handed to two workers at once.
   */
  @Query(
      value =
          """
          select * from inventory_sync_events
          where status = 'PENDING' and available_at <= :now
          order by created_at
          limit :batchSize
          for update skip locked
          """,
      nativeQuery = true)
  List<InventoryEvent> claimPending(@Param("now") Instant now, @Param("batchSize") int batchSize);

  long countByStatus(com.shiro.flashsale.constants.InventoryEventStatus status);

  long countByProductId(UUID productId);

  List<InventoryEvent> findByProductId(UUID productId);

  boolean existsByEventKey(String eventKey);
}
