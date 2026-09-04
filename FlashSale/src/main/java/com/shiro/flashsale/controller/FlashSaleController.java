package com.shiro.flashsale.controller;

import com.shiro.flashsale.dto.PurchaseDtos;
import com.shiro.flashsale.dto.SaleItemResponse;
import com.shiro.flashsale.security.CurrentUser;
import com.shiro.flashsale.service.PurchaseService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/flash-sales")
@AllArgsConstructor
public class FlashSaleController {
  private final PurchaseService service;

  @GetMapping("/current-flashSale")
  public List<SaleItemResponse> getCurrentFlashSaleItems() {
    return service.currentItems();
  }

  @PostMapping("/purchase")
  public PurchaseDtos.PurchaseResponse purchase(
      Authentication authentication, @Valid @RequestBody PurchaseDtos.PurchaseRequest request) {
    return service.purchase(CurrentUser.id(authentication), request);
  }
}
