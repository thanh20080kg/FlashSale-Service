package com.shiro.authentication.security;

import com.shiro.authentication.constants.Permission;
import com.shiro.authentication.constants.Role;
import java.util.HashSet;
import java.util.Set;

public final class RolePermissions {
  private RolePermissions() {}

  public static Set<Permission> permissions(Role role) {
    return new HashSet<>();
  }
}
