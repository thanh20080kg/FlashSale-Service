package com.shiro.authentication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor
public class RegisterRequest {
  @NotBlank
  @Size(max = 190)
  private String identifier;

  @NotBlank
  @Size(min = 8, max = 72)
  private String password;
}
