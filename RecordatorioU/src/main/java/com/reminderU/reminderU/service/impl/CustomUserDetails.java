package com.reminderU.reminderU.service.impl;

import com.reminderU.reminderU.modelo.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Clase personalizada que implementa UserDetails y contiene nuestra entidad Usuario.
 * Esto permite unificar el manejo de usuarios locales y OAuth2.
 */
public class CustomUserDetails implements UserDetails {

    private final Usuario usuario;

    public CustomUserDetails(Usuario usuario) {
        this.usuario = usuario;
    }

    // ===== MÉTODO CRÍTICO: permite acceder al Usuario completo =====
    public Usuario getUsuario() {
        return usuario;
    }

    // ===== Métodos de conveniencia =====
    public Long getId() {
        return usuario.getIdUsuario();
    }

    public String getNombre() {
        return usuario.getNombre();
    }

    public String getEmail() {
        return usuario.getEmail();
    }

    public String getFotoPerfil() {
        return usuario.getFotoPerfil();
    }

    public String getRol() {
        return usuario.getRol();
    }

    public String getProveedor() {
        return usuario.getProveedor();
    }

    // ===== Implementación de UserDetails =====
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol()));
    }

    @Override
    public String getPassword() {
        return usuario.getPassword();
    }

    @Override
    public String getUsername() {
        return usuario.getEmail();
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