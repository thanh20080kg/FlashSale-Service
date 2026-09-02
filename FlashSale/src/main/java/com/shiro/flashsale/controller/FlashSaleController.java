package com.shiro.flashsale.controller;

import com.shiro.flashsale.dto.SaleDtos;
import com.shiro.flashsale.security.CurrentUser;
import com.shiro.flashsale.service.SaleService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/flash-sales")
@AllArgsConstructor
public class FlashSaleController {
  private final SaleService service;

  @GetMapping("/current-flashSale")
  public List<SaleDtos.SaleItemResponse> getCurrentFlashSaleItems() {
    return service.currentItems();
  }

  @PostMapping("/purchase")
  public SaleDtos.PurchaseResponse purchase(
      Authentication authentication, @Valid @RequestBody SaleDtos.PurchaseRequest request) {
    return service.purchase(CurrentUser.id(authentication), request);
  }
}
