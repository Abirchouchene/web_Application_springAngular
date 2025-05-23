package com.abirch.security.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "_user")
public class User implements UserDetails {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  private String firstname;
  private String lastname;

  @Column(unique = true, nullable = false)
  private String email;

  private String password;

  @Column(name = "mfa_enabled")
  private boolean mfaEnabled;

  private String secret;

  @Column(name = "actif")
  private boolean actif;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "role_id")
  private Role role;

  @OneToMany(mappedBy = "utilisateur", cascade = CascadeType.ALL)
  @JsonManagedReference

  private List<HistoriqueAction> actions;

  // Spring Security UserDetails implementation
  @Override
  @JsonIgnore

  public Collection<? extends GrantedAuthority> getAuthorities() {
    if (role == null || role.getNom() == null) {
      return List.of(); // ou retourner un rôle par défaut si besoin
    }
    return List.of(new SimpleGrantedAuthority("ROLE_" + role.getNom()));
  }


  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getUsername() {
    return email;
  }

  @Override
  public boolean isAccountNonExpired() {
    return actif;
  }

  @Override
  public boolean isAccountNonLocked() {
    return actif;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return actif;
  }

  @Override
  public boolean isEnabled() {
    return actif;
  }
}
