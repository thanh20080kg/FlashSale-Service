package com.shiro.flashsale.service.impl;

import com.shiro.flashsale.client.WarehouseClient;
import com.shiro.flashsale.constants.WareHouseOperation;
import com.shiro.flashsale.constants.WarehouseStatus;
import com.shiro.flashsale.dto.client.WarehouseRequest;
import com.shiro.flashsale.dto.client.WarehouseResult;
import com.shiro.flashsale.exception.ApiException;
import com.shiro.flashsale.exception.ErrorCode;
import com.shiro.flashsale.service.WarehouseService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {
  private static final Logger log = LoggerFactory.getLogger(WarehouseServiceImpl.class);
  private final WarehouseClient warehouseClient;
  private final ObjectMapper objectMapper;

  @Override
  public WarehouseResult reserve(UUID productId, String reservationKey) {
    WarehouseResult response =
        send(new WarehouseRequest(WareHouseOperation.RESERVED, reservationKey, productId, 1));
    if (response.success() || WarehouseStatus.ALREADY_RESERVED.equals(response.status())) {
      return response;
    }
    switch (response.status()) {
      case ALREADY_SOLD -> throw ApiException.of(ErrorCode.INVALID_REQUEST);
      case OUT_OF_STOCK -> throw ApiException.of(ErrorCode.OUT_OF_STOCK);
      case NOT_EXIST -> throw ApiException.of(ErrorCode.PRODUCT_NOT_FOUND);
      default -> throw ApiException.of(ErrorCode.INTERNAL_ERROR);
    }
  }

  @Override
  public WarehouseResult sold(UUID productId, String reservationKey) {
    WarehouseResult response =
        send(new WarehouseRequest(WareHouseOperation.SOLD, reservationKey, productId, 1));
    if (response.success() || WarehouseStatus.ALREADY_SOLD.equals(response.status())) {
      return response;
    }
    switch (response.status()) {
      case RELEASED -> throw ApiException.of(ErrorCode.INVALID_REQUEST);
      case NOT_EXIST -> throw ApiException.of(ErrorCode.PRODUCT_NOT_FOUND);
      default -> throw ApiException.of(ErrorCode.INTERNAL_ERROR);
    }
  }

  @Override
  public WarehouseResult release(String reservationKey, UUID productId) {
    WarehouseResult response =
        send(new WarehouseRequest(WareHouseOperation.RELEASED, reservationKey, productId, 0));
    if (response.success() || WarehouseStatus.ALREADY_RELEASED.equals(response.status())) {
      return response;
    }
    switch (response.status()) {
      case SOLD -> throw ApiException.of(ErrorCode.INVALID_REQUEST);
      case NOT_EXIST -> throw ApiException.of(ErrorCode.PRODUCT_NOT_FOUND);
      default -> throw ApiException.of(ErrorCode.INTERNAL_ERROR);
    }
  }

  private WarehouseResult send(WarehouseRequest request) {
    try {
      return objectMapper.readValue(warehouseClient.send(request), WarehouseResult.class);
    } catch (ApiException exception) {
      throw exception;
    } catch (Exception exception) {
      log.error("Warehouse communication failed, operation={}", request.operation(), exception);
      throw ApiException.of(ErrorCode.INTERNAL_ERROR);
    }
  }

  private WarehouseResult send(WarehouseRequest request, int retry) {
    try {
      return objectMapper.readValue(warehouseClient.send(request, retry), WarehouseResult.class);
    } catch (ApiException exception) {
      throw exception;
    } catch (Exception exception) {
      log.error(
          "Warehouse communication failed, operation={}, retry={}",
          request.operation(),
          retry,
          exception);
      throw ApiException.of(ErrorCode.INTERNAL_ERROR);
    }
  }
}
