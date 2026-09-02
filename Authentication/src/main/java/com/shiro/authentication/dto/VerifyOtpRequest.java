package com.shiro.authentication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VerifyOtpRequest {
  @NotBlank
  @Size(max = 190)
  private String identifier;

  @NotBlank
  @Pattern(regexp = "\\d{6}", message = "must be 6 digits")
  private String otp;
}
