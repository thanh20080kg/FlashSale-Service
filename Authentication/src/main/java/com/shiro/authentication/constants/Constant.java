package com.shiro.authentication.constants;

import java.util.regex.Pattern;

public class Constant {
  public static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
  public static final Pattern PHONE = Pattern.compile("^\\+?\\d{8,15}$");
  public static final String IDENTIFIER = "identifier";
  public static final String CHANNEL = "channel";
  public static final String PASSWORD_HASH = "passwordHash";
  public static final String DISPLAY_NAME = "displayName";
  public static final String ROLE = "role";
  public static final String PERMISSIONS = "permissions";
  public static final String TOKEN_TYPE = "token_type";
  public static final String ACCESS = "access";
  public static final String BEARER = "Bearer";
  public static final String VIETNAM = "VN";
  public static final String SHA_256 = "SHA-256";

  private Constant() {}
}
