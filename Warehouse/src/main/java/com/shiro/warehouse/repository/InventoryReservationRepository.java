package com.shiro.warehouse.repository;

import com.shiro.warehouse.entity.InventoryReservation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, UUID> {
  Optional<InventoryReservation> findByReservationKey(UUID reservationKey);

  List<InventoryReservation> findByStatusOrderByCreatedAtAsc(String status, Pageable pageable);

  @Modifying
  @Query(
      value =
          "UPDATE inventory_reservations SET status = :status WHERE reservation_key = :key AND status = 'RESERVED'",
      nativeQuery = true)
  int updateReservedStatus(@Param("key") String key, @Param("status") String status);
}
