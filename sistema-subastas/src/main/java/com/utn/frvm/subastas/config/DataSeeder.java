package com.utn.frvm.subastas.config;

import com.utn.frvm.subastas.entities.Categoria;
import com.utn.frvm.subastas.entities.Usuario;
import com.utn.frvm.subastas.enums.EstadoUsuario;
import com.utn.frvm.subastas.enums.RolUsuario;
import com.utn.frvm.subastas.repositories.CategoriaRepository;
import com.utn.frvm.subastas.repositories.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final CategoriaRepository categoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(CategoriaRepository categoriaRepository,
                      UsuarioRepository usuarioRepository,
                      PasswordEncoder passwordEncoder) {
        this.categoriaRepository = categoriaRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Seed Categories
        if (categoriaRepository.count() == 0) {
            List<Categoria> categorias = Arrays.asList(
                Categoria.builder().nombre("Electrónica y Tecnología").descripcion("Notebooks, celulares, consolas y accesorios tecnológicos").build(),
                Categoria.builder().nombre("Vehículos y Accesorios").descripcion("Autos, motos, repuestos y accesorios de vehículos").build(),
                Categoria.builder().nombre("Hogar y Muebles").descripcion("Artículos para el hogar, decoración, sillones, mesas y electrodomésticos").build(),
                Categoria.builder().nombre("Moda y Vestimenta").descripcion("Ropa, calzado, carteras y accesorios de moda").build(),
                Categoria.builder().nombre("Coleccionables y Arte").descripcion("Antigüedades, pinturas, monedas, cartas coleccionables y arte").build()
            );
            categoriaRepository.saveAll(categorias);
            System.out.println("Categorías iniciales pobladas correctamente.");
        }

        // Seed Admin User
        if (!usuarioRepository.existsByUsername("admin")) {
            Usuario admin = Usuario.builder()
                    .username("admin")
                    .email("admin@subastas.com")
                    .password(passwordEncoder.encode("admin123"))
                    .rol(RolUsuario.ROLE_ADMIN)
                    .estado(EstadoUsuario.ACTIVO)
                    .incidenciasAcumuladas(0)
                    .build();
            usuarioRepository.save(admin);
            System.out.println("Usuario administrador por defecto (admin / admin123) creado correctamente.");
        }
    }
}
