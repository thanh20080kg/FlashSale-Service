package com.shiro.flashsale.repository;

import com.shiro.flashsale.entity.FlashSaleSlot;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlashSaleSlotRepository extends JpaRepository<FlashSaleSlot, UUID> {
  List<FlashSaleSlot> findAllByOrderByStartTimeAsc();
}
