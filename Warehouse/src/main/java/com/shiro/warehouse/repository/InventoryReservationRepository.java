package com.shiro.warehouse.repository;

import com.shiro.warehouse.entity.InventoryReservation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, UUID> {
  Optional<InventoryReservation> findByReservationKey(UUID reservationKey);

  List<InventoryReservation> findByStatus(String status);

  @Modifying
  @Query("UPDATE InventoryReservation r SET r.status = :status WHERE r.reservationKey = :key AND r.status = 'RESERVED'")
  int updateReservedStatus(@Param("key") UUID key, @Param("status") String status);
}
