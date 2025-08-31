package com.example.MarketS.service;

import com.example.MarketS.model.Producto;
import com.example.MarketS.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ProductoService {

    @Autowired
    private ProductoRepository productoRepository;

    public Page<Producto> buscarPorTodosLosCampos(String keyword, Long vendedorId, Pageable pageable) {
        return productoRepository.buscarPorVendedorYKeyword(vendedorId, keyword, pageable);
    }

    public Page<Producto> listarProductosPorVendedor(Long vendedorId, Pageable pageable) {
        return productoRepository.findByVendedorId(vendedorId, pageable);
    }
}