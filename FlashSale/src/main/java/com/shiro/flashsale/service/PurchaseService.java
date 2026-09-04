package com.shiro.flashsale.service;

import com.shiro.flashsale.dto.PurchaseDtos;
import com.shiro.flashsale.dto.PurchaseStatusSyncDtos;
import com.shiro.flashsale.dto.SaleItemResponse;
import java.util.List;
import java.util.UUID;

public interface PurchaseService {
  List<SaleItemResponse> currentItems();

  PurchaseDtos.PurchaseResponse purchase(UUID userId, PurchaseDtos.PurchaseRequest request);

  List<PurchaseDtos.PurchaseHistoryResponse> purchaseHistory(UUID userId, int limit);

  void reloadQuota();

  PurchaseStatusSyncDtos.Response getStatus(List<UUID> purchaseIds);
}
