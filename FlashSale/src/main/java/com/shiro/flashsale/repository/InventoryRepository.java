package com.shiro.flashsale.repository;

import com.shiro.flashsale.entity.Inventory;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

  Optional<Inventory> findByProductId(UUID productId);

  /** Atomic reservation of one unit; returns 0 when stock has run out. */
  @Modifying
  @Query(
      """
      update Inventory i set i.availableQuantity = i.availableQuantity - 1
      where i.product.id = :productId and i.availableQuantity > 0
      """)
  int reserve(@Param("productId") UUID productId);

  /** Operator stock change. Guarded so on-hand stock can never be driven negative. */
  @Modifying
  @Query(
      """
      update Inventory i set i.availableQuantity = i.availableQuantity + :delta
      where i.product.id = :productId and i.availableQuantity + :delta >= 0
      """)
  int adjust(@Param("productId") UUID productId, @Param("delta") long delta);

  /** Applied by the sync worker only, from the outbox. */
  @Modifying
  @Query(
      """
      update Inventory i set i.soldQuantity = i.soldQuantity + :delta where i.product.id = :productId
      """)
  int addSold(@Param("productId") UUID productId, @Param("delta") long delta);
}
