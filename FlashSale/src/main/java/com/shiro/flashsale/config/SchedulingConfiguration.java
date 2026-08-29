package com.shiro.flashsale.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * The inventory sync worker is scheduled, but it claims work with {@code FOR UPDATE SKIP LOCKED} so
 * every instance can run its own scheduler without coordinating.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.inventory-sync.enabled", havingValue = "true")
public class SchedulingConfiguration {}
