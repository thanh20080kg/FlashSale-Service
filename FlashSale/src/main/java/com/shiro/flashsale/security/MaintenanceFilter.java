package com.shiro.flashsale.security;

import com.shiro.flashsale.service.MaintenanceConfigService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MaintenanceFilter extends OncePerRequestFilter {
  private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
  private static final List<Route> ROUTES =
      List.of(
          new Route("GET", "/api/v1/flash-sales/current", "MAINTENANCE_FLASH_SALES_CURRENT"),
          new Route("POST", "/api/v1/flash-sales/purchase", "MAINTENANCE_FLASH_SALES_PURCHASE"),
          new Route("GET", "/api/v1/me/balance", "MAINTENANCE_ME_BALANCE"),
          new Route("GET", "/api/v1/me/purchases", "MAINTENANCE_ME_PURCHASES"),
          new Route("POST", "/api/v1/admin/products", "MAINTENANCE_ADMIN_PRODUCTS_CREATE"),
          new Route("GET", "/api/v1/admin/products", "MAINTENANCE_ADMIN_PRODUCTS_LIST"),
          new Route(
              "PATCH", "/api/v1/admin/products/{productId}", "MAINTENANCE_ADMIN_PRODUCTS_UPDATE"),
          new Route(
              "POST",
              "/api/v1/admin/products/{productId}/inventory",
              "MAINTENANCE_ADMIN_INVENTORY_ADJUST"),
          new Route(
              "GET",
              "/api/v1/admin/products/{productId}/movements",
              "MAINTENANCE_ADMIN_INVENTORY_MOVEMENTS"),
          new Route("POST", "/api/v1/admin/slots", "MAINTENANCE_ADMIN_SLOTS_CREATE"),
          new Route("GET", "/api/v1/admin/slots", "MAINTENANCE_ADMIN_SLOTS_LIST"),
          new Route("PATCH", "/api/v1/admin/slots/{slotId}", "MAINTENANCE_ADMIN_SLOTS_UPDATE"),
          new Route("POST", "/api/v1/admin/slots/{slotId}/items", "MAINTENANCE_ADMIN_ITEMS_CREATE"),
          new Route("GET", "/api/v1/admin/items", "MAINTENANCE_ADMIN_ITEMS_LIST"),
          new Route("PATCH", "/api/v1/admin/items/{itemId}", "MAINTENANCE_ADMIN_ITEMS_UPDATE"),
          new Route(
              "POST",
              "/api/v1/admin/customers/{userId}/balance",
              "MAINTENANCE_ADMIN_CUSTOMER_TOP_UP"),
          new Route(
              "GET",
              "/api/v1/admin/inventory-sync/status",
              "MAINTENANCE_ADMIN_INVENTORY_SYNC_STATUS"));

  private final MaintenanceConfigService maintenanceConfig;
  private final ObjectMapper objectMapper;

  public MaintenanceFilter(MaintenanceConfigService maintenanceConfig, ObjectMapper objectMapper) {
    this.maintenanceConfig = maintenanceConfig;
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (!request.getRequestURI().startsWith("/api/")) {
      chain.doFilter(request, response);
      return;
    }
    Route route = routeFor(request);
    if (maintenanceConfig.isMaintenance(ObjectUtils.isEmpty(route) ? null : route.configKey())) {
      writeMaintenanceResponse(response);
      return;
    }
    chain.doFilter(request, response);
  }

  private static Route routeFor(HttpServletRequest request) {
    return ROUTES.stream()
        .filter(
            route ->
                route.method().equals(request.getMethod())
                    && PATH_MATCHER.match(route.path(), request.getRequestURI()))
        .findFirst()
        .orElse(null);
  }

  private void writeMaintenanceResponse(HttpServletResponse response) throws IOException {
    response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("timestamp", Instant.now().toString());
    body.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
    body.put("code", "SERVICE_MAINTENANCE");
    body.put("message", "Service is temporarily unavailable for maintenance");
    objectMapper.writeValue(response.getOutputStream(), body);
  }

  private record Route(String method, String path, String configKey) {}
}
