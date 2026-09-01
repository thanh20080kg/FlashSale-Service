package com.shiro.flashsale.controller;

import com.shiro.flashsale.dto.SaleDtos;
import com.shiro.flashsale.security.CurrentUser;
import com.shiro.flashsale.service.SaleService;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me")
public class MeController {
  private final SaleService service;

  public MeController(SaleService service) {
    this.service = service;
  }

  @GetMapping("/purchases")
  public List<SaleDtos.PurchaseHistoryResponse> purchases(
      Authentication authentication, @RequestParam(defaultValue = "20") int limit) {
    return service.purchaseHistory(CurrentUser.id(authentication), limit);
  }
}
