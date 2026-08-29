package com.shiro.flashsale.repository;

import com.shiro.flashsale.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
  Optional<User> findByIdentifier(String identifier);

  boolean existsByIdentifier(String identifier);
}
