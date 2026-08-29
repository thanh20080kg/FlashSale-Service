package com.shiro.flashsale.repository;

import com.shiro.flashsale.entity.Product;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, UUID> {
  Optional<Product> findBySku(String sku);

  boolean existsBySku(String sku);
}
