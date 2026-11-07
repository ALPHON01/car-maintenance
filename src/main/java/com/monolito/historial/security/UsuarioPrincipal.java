package com.monolito.historial.security;

import com.monolito.historial.model.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Usuario autenticado. Guarda el id para poder filtrar los datos por propietario
 * sin volver a consultar la base de datos en cada petición.
 */
public class UsuarioPrincipal implements UserDetails {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String nombre;
    private final String email;
    private final String password;

    public UsuarioPrincipal(Usuario usuario) {
        this.id = usuario.getId();
        this.nombre = usuario.getNombre();
        this.email = usuario.getEmail();
        this.password = usuario.getPassword();
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }
}