package com.reminderU.reminderU.service.impl;

import com.reminderU.reminderU.modelo.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Clase personalizada que implementa OAuth2User y contiene nuestra entidad Usuario.
 * Esto permite que Spring Security maneje tanto usuarios OAuth2 como locales
 * de manera unificada.
 */
public class CustomOAuth2User implements OAuth2User {

    private final OAuth2User oauth2User;
    private final Usuario usuario;

    public CustomOAuth2User(OAuth2User oauth2User, Usuario usuario) {
        this.oauth2User = oauth2User;
        this.usuario = usuario;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return oauth2User.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Retornar el rol del usuario de la BD
        return Collections.singletonList(
            new SimpleGrantedAuthority("ROLE_" + usuario.getRol())
        );
    }

    @Override
    public String getName() {
        return usuario.getEmail();
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
}