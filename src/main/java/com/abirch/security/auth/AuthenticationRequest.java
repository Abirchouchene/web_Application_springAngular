package com.abirch.security.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
// mtaa login
public class AuthenticationRequest {

  private String email;
  private String password;
}
