package com.shiro.flashsale.controller;

import com.shiro.flashsale.dto.SaleDtos;
import com.shiro.flashsale.security.CurrentUser;
import com.shiro.flashsale.service.SaleService;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me")
@AllArgsConstructor
public class MeController {
  private final SaleService service;

  @GetMapping("/purchases")
  public List<SaleDtos.PurchaseHistoryResponse> purchases(
      Authentication authentication, @RequestParam(defaultValue = "20") int limit) {
    return service.purchaseHistory(CurrentUser.id(authentication), limit);
  }
}
