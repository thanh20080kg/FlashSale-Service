package com.shiro.warehouse.repository;

import com.shiro.warehouse.entity.Inventory;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {
  Optional<Inventory> findByProductId(UUID productId);

  @Modifying
  @Query(
      value =
          "UPDATE inventories SET available_quantity = available_quantity - :quantity "
              + "WHERE product_id = :productId AND available_quantity >= :quantity",
      nativeQuery = true)
  int reserve(@Param("productId") String productId, @Param("quantity") long quantity);

  @Modifying
  @Query(
      value =
          "UPDATE inventories SET available_quantity = available_quantity + :quantity "
              + "WHERE product_id = :productId",
      nativeQuery = true)
  int release(@Param("productId") String productId, @Param("quantity") long quantity);

  @Modifying
  @Query(
      value =
          "UPDATE inventories SET sold_quantity = sold_quantity + :quantity "
              + "WHERE product_id = :productId",
      nativeQuery = true)
  int sold(@Param("productId") String productId, @Param("quantity") long quantity);
}
