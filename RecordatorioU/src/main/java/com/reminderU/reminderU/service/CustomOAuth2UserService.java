package com.reminderU.reminderU.service;

import com.reminderU.reminderU.modelo.Usuario;
import com.reminderU.reminderU.repository.UsuarioRepository;
import com.reminderU.reminderU.service.impl.CustomOAuth2User;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UsuarioRepository usuarioRepository;

    public CustomOAuth2UserService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        
        // Obtener información del usuario de Google
        String email = oauth2User.getAttribute("email");
        String nombre = oauth2User.getAttribute("given_name");
        String apellido = oauth2User.getAttribute("family_name");
        String fotoPerfil = oauth2User.getAttribute("picture");
        
        System.out.println("=== OAUTH2 LOGIN ===");
        System.out.println("Email: " + email);
        System.out.println("Nombre: " + nombre);
        
        // Buscar usuario existente
        Usuario usuario = usuarioRepository.findByEmail(email);
        
        if (usuario == null) {
            // Crear nuevo usuario
            System.out.println("Creando nuevo usuario de Google");
            usuario = new Usuario();
            usuario.setEmail(email);
            usuario.setNombre(nombre != null ? nombre : "Usuario");
            usuario.setApellido(apellido != null ? apellido : "Google");
            usuario.setFotoPerfil(fotoPerfil);
            usuario.setProveedor("GOOGLE");
            usuario.setRol("USER");
            // NO establecer password para usuarios OAuth
            usuario = usuarioRepository.save(usuario);
            System.out.println("Usuario creado exitosamente con ID: " + usuario.getIdUsuario());
        } else {
            System.out.println("Usuario ya existe en BD con ID: " + usuario.getIdUsuario());
            // Actualizar foto de perfil si cambió
            if (fotoPerfil != null && !fotoPerfil.equals(usuario.getFotoPerfil())) {
                usuario.setFotoPerfil(fotoPerfil);
                usuario = usuarioRepository.save(usuario);
            }
        }
        
        System.out.println("=== FIN OAUTH2 ===");
        
        // CLAVE: Retornar CustomOAuth2User en lugar de oauth2User
        return new CustomOAuth2User(oauth2User, usuario);
    }
}