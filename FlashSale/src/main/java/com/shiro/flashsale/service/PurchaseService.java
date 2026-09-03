package com.shiro.flashsale.service;

import com.shiro.flashsale.dto.PurchaseStatusSyncDtos;
import com.shiro.flashsale.dto.SaleDtos;
import java.util.List;
import java.util.UUID;

public interface PurchaseService {
  List<SaleDtos.SaleItemResponse> currentItems();

  SaleDtos.PurchaseResponse purchase(UUID userId, SaleDtos.PurchaseRequest request);

  List<SaleDtos.PurchaseHistoryResponse> purchaseHistory(UUID userId, int limit);

  void reloadQuota();

  PurchaseStatusSyncDtos.Response getStatus(List<UUID> purchaseIds);
}
