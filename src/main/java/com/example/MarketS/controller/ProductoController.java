package com.example.MarketS.controller;

import com.example.MarketS.dto.ProductoDto;
import com.example.MarketS.model.User;
import com.example.MarketS.repository.CategoriaRepository;
import com.example.MarketS.repository.MarcaRepository;
import com.example.MarketS.repository.ProductoRepository;
import com.example.MarketS.model.Categorias;
import com.example.MarketS.model.Producto;
import com.example.MarketS.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/productos")
public class ProductoController {

    private final Logger logg = LoggerFactory.getLogger(ProductoController.class);
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private MarcaRepository marcaRepository;
    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private UserService userService;

    @GetMapping("/index")
    public String mostrarProductosAleatorios(Model model) {
        List<Producto> todos = productoRepository.findByActivoTrue();

        Collections.shuffle(todos);

        List<Producto> productosAleatorios = todos.stream()
                .limit(30)
                .toList();

        model.addAttribute("productos", productosAleatorios);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        if (email != null && !email.equals("anonymousUser")) {
            User user = userService.buscarPorEmail(email).orElse(null);
            if (user != null) {
                model.addAttribute("nombreUsuario", user.getUsername());
                model.addAttribute("rolUsuario", user.getRol().getRolname());
            }
        }

        return "index";
    }




    @GetMapping("/register")
    public String create(Model model) {
        model.addAttribute("productoDto", new ProductoDto());
        model.addAttribute("categorias", categoriaRepository.findAll());
        return "registerProducto";
    }




    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Integer id){
        Producto p = productoRepository.findById(id).orElse(null);
        if(p != null){
            productoRepository.delete(p);
            logg.info("Objeto eliminado: {}", p);
        } else {
            logg.warn("No se encontró producto con id: {}", id);
        }
        return "redirect:/productos";
    }



    @GetMapping("/categoria/{id}")
    public String ProductosPorCategoria(@PathVariable("id") Long id, Model model) {

        List<Producto> productos = productoRepository.findByCategoriasIdAndActivoTrue(id);


        for (Producto producto : productos) {
            if (producto.getImagen() != null && !producto.getImagen().startsWith("/uploads/")) {
                producto.setImagen("/uploads/" + producto.getImagen());
            }
        }


        Optional<Categorias> categoriaOpt = categoriaRepository.findById(id);
        String nombreCategoria = categoriaOpt.map(Categorias::getNombreCategoria).orElse("Categoría no encontrada");

        model.addAttribute("productos", productos);
        model.addAttribute("categoriaNombre", nombreCategoria);

        return "FiltrosCategorias";
    }
    }
