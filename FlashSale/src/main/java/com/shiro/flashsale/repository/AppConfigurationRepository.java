package com.shiro.flashsale.repository;

import com.shiro.flashsale.entity.AppConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppConfigurationRepository extends JpaRepository<AppConfiguration, String> {}
