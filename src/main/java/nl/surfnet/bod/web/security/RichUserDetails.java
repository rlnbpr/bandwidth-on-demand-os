/**
 * The owner of the original code is SURFnet BV.
 *
 * Portions created by the original owner are Copyright (C) 2011-2012 the
 * original owner. All Rights Reserved.
 *
 * Portions created by other contributors are Copyright (C) the contributor.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   (Contributors insert name & email here)
 *
 * This file is part of the SURFnet7 Bandwidth on Demand software.
 *
 * The SURFnet7 Bandwidth on Demand software is free software: you can
 * redistribute it and/or modify it under the terms of the BSD license
 * included with this distribution.
 *
 * If the BSD license cannot be found with this distribution, it is available
 * at the following location <http://www.opensource.org/licenses/BSD-3-Clause>
 */
package nl.surfnet.bod.web.security;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.google.common.base.Objects;

@SuppressWarnings("serial")
public class RichUserDetails implements UserDetails {

  private final String username;
  private final String displayName;
  private final Collection<GrantedAuthority> authorities;

  public RichUserDetails(String username, String displayName, Collection<GrantedAuthority> authorities) {
    this.username = username;
    this.displayName = displayName;
    this.authorities = authorities;
  }

  @Override
  public Collection<GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public String getPassword() {
    return "N/A";
  }

  @Override
  public String getUsername() {
    return username;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getNameId() {
    return getUsername();
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

  @Override
  public String toString() {
    return Objects.toStringHelper(RichUserDetails.class).add("nameId", getNameId())
        .add("displayName", getDisplayName()).toString();
  }

}
