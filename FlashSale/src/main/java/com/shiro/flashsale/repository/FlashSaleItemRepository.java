package com.shiro.flashsale.repository;

import com.shiro.flashsale.entity.FlashSaleItem;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FlashSaleItemRepository extends JpaRepository<FlashSaleItem, UUID> {

  String ACTIVE_WINDOW =
      """
      i.active = true and s.active = true and p.active = true
      and (s.saleDate is null or s.saleDate = :saleDate)
      and ((s.startTime <= s.endTime and s.startTime <= :time and s.endTime > :time)
        or (s.startTime > s.endTime and (:time >= s.startTime or :time < s.endTime)))
      """;

  @Query(
      "select i from FlashSaleItem i join fetch i.slot s join fetch i.product p where "
          + ACTIVE_WINDOW
          + " order by s.startTime, p.sku")
  List<FlashSaleItem> findCurrent(
      @Param("saleDate") LocalDate saleDate, @Param("time") LocalTime time);

  @Query(
      "select i from FlashSaleItem i join fetch i.slot s join fetch i.product p where i.id = :id and "
          + ACTIVE_WINDOW)
  Optional<FlashSaleItem> findActiveById(
      @Param("id") UUID id, @Param("saleDate") LocalDate saleDate, @Param("time") LocalTime time);

  @Query("select i from FlashSaleItem i join fetch i.slot join fetch i.product order by i.id")
  List<FlashSaleItem> findAllWithRelations();

  @Query("select i.id from FlashSaleItem i where i.product.id = :productId")
  List<UUID> findIdsByProductId(@Param("productId") UUID productId);
}
