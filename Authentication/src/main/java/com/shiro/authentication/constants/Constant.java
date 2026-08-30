package com.shiro.authentication.constants;

import java.util.regex.Pattern;

public class Constant {
  private Constant() {}

  public static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
  public static final Pattern PHONE = Pattern.compile("^\\+?\\d{8,15}$");
}
