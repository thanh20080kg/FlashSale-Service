package com.shiro.flashsale.security;

import com.shiro.flashsale.constants.Permission;
import com.shiro.flashsale.constants.Role;
import java.util.EnumSet;
import java.util.Set;

public final class RolePermissions {
  private RolePermissions() {}

  public static Set<Permission> permissions(Role role) {
    return switch (role) {
      case ADMIN -> EnumSet.allOf(Permission.class);
      case SELLER -> EnumSet.of(Permission.SALE_MANAGE);
      case BUYER -> EnumSet.of(Permission.SALE_PURCHASE);
    };
  }
}
