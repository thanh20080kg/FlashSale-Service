package com.shiro.flashsale.service.impl;

import static com.shiro.flashsale.dto.client.WarehouseDtos.Status.*;

import com.shiro.flashsale.client.WarehouseClient;
import com.shiro.flashsale.dto.client.WarehouseDtos;
import com.shiro.flashsale.exception.ApiException;
import com.shiro.flashsale.exception.ErrorCode;
import com.shiro.flashsale.service.WarehouseService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/** Adapts warehouse commands to the remote warehouse service. */
@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {
  private static final Logger log = LoggerFactory.getLogger(WarehouseServiceImpl.class);
  private final WarehouseClient warehouseClient;
  private final ObjectMapper objectMapper;

  @Override
  public WarehouseDtos.Response reserve(UUID productId, UUID reservationKey, Integer quantity) {
    WarehouseDtos.Response response =
        send(
            new WarehouseDtos.Request(
                WarehouseDtos.Operation.RESERVE, reservationKey, productId, quantity));
    if (response.isSuccess() || ALREADY_RESERVED.equals(response.getStatus())) {
      return response;
    }
    switch (response.getStatus()) {
      case ALREADY_SOLD -> throw ApiException.of(ErrorCode.INVALID_REQUEST);
      case OUT_OF_STOCK -> throw ApiException.of(ErrorCode.OUT_OF_STOCK);
      case NOT_EXIST -> throw ApiException.of(ErrorCode.PRODUCT_NOT_FOUND);
      default -> throw ApiException.of(ErrorCode.INTERNAL_ERROR);
    }
  }

  @Override
  public WarehouseDtos.Response sold(UUID productId, UUID reservationKey) {
    WarehouseDtos.Response response =
        send(
            new WarehouseDtos.Request(
                WarehouseDtos.Operation.CONFIRM, reservationKey, productId, null));
    if (response.isSuccess() || ALREADY_SOLD.equals(response.getStatus())) {
      return response;
    }
    switch (response.getStatus()) {
      case RELEASED -> throw ApiException.of(ErrorCode.INVALID_REQUEST);
      case NOT_EXIST -> throw ApiException.of(ErrorCode.PRODUCT_NOT_FOUND);
      default -> throw ApiException.of(ErrorCode.INTERNAL_ERROR);
    }
  }

  @Override
  public WarehouseDtos.Response release(UUID reservationKey, UUID productId) {
    WarehouseDtos.Response response =
        send(
            new WarehouseDtos.Request(
                WarehouseDtos.Operation.RELEASE, reservationKey, productId, null));
    if (response.isSuccess() || ALREADY_RELEASED.equals(response.getStatus())) {
      return response;
    }
    switch (response.getStatus()) {
      case SOLD -> throw ApiException.of(ErrorCode.INVALID_REQUEST);
      case NOT_EXIST -> throw ApiException.of(ErrorCode.PRODUCT_NOT_FOUND);
      default -> throw ApiException.of(ErrorCode.INTERNAL_ERROR);
    }
  }

  private WarehouseDtos.Response send(WarehouseDtos.Request request) {
    try {
      return objectMapper.readValue(warehouseClient.send(request), WarehouseDtos.Response.class);
    } catch (ApiException exception) {
      throw exception;
    } catch (Exception exception) {
      log.error("Warehouse communication failed, operation={}", request.getOperation(), exception);
      throw ApiException.of(ErrorCode.INTERNAL_ERROR);
    }
  }

  private WarehouseDtos.Response send(WarehouseDtos.Request request, int retry) {
    try {
      return objectMapper.readValue(
          warehouseClient.send(request, retry), WarehouseDtos.Response.class);
    } catch (ApiException exception) {
      throw exception;
    } catch (Exception exception) {
      log.error(
          "Warehouse communication failed, operation={}, retry={}",
          request.getOperation(),
          retry,
          exception);
      throw ApiException.of(ErrorCode.INTERNAL_ERROR);
    }
  }
}
