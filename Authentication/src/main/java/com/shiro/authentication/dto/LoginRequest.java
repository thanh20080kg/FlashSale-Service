package com.shiro.authentication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
  @NotBlank
  @Size(max = 190)
  private String identifier;

  @NotBlank
  @Size(max = 72)
  private String password;
}
