/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.global.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.sku.aissue.global.client.UserServiceClient.UserCredentialsDto;

public record CustomUserDetails(UserCredentialsDto credentials) implements UserDetails {

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority(credentials.role()));
  }

  @Override
  public String getPassword() {
    return credentials.password();
  }

  @Override
  public String getUsername() {
    return credentials.username();
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }
}
