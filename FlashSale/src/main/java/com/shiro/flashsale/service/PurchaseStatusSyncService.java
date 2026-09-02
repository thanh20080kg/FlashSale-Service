package com.shiro.flashsale.service;

import com.shiro.flashsale.dto.PurchaseStatusSyncDtos;
import com.shiro.flashsale.repository.PurchaseRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PurchaseStatusSyncService {
  private final PurchaseRepository purchases;

  @Transactional(readOnly = true)
  public PurchaseStatusSyncDtos.Response statuses(List<UUID> purchaseIds) {
    if (purchaseIds == null || purchaseIds.isEmpty()) {
      return new PurchaseStatusSyncDtos.Response(List.of());
    }
    List<PurchaseStatusSyncDtos.Entry> entries =
        purchases.findAllById(purchaseIds).stream()
            .map(
                purchase ->
                    new PurchaseStatusSyncDtos.Entry(purchase.getId(), purchase.getStatus().name()))
            .toList();
    return new PurchaseStatusSyncDtos.Response(entries);
  }
}
