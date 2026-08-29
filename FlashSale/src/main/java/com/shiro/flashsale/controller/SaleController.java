package com.shiro.flashsale.controller;

import com.shiro.flashsale.dto.SaleDtos;
import com.shiro.flashsale.security.CurrentUser;
import com.shiro.flashsale.service.SaleService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/flash-sales")
public class SaleController {
  private final SaleService service;

  public SaleController(SaleService service) {
    this.service = service;
  }

  /** Public storefront: what is on sale right now, with today's remaining quota. */
  @GetMapping("/current")
  public List<SaleDtos.SaleItemResponse> current() {
    return service.currentItems();
  }

  @PostMapping("/purchase")
  public SaleDtos.PurchaseResponse purchase(
      Authentication authentication, @Valid @RequestBody SaleDtos.PurchaseRequest request) {
    return service.purchase(CurrentUser.id(authentication), request);
  }
}
