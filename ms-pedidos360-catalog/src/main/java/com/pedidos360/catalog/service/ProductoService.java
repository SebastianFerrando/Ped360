package com.pedidos360.catalog.service;

import com.pedidos360.catalog.dto.ProductoRequest;
import com.pedidos360.catalog.dto.ProductoResponse;
import com.pedidos360.catalog.exception.RecursoNoEncontradoException;
import com.pedidos360.catalog.exception.StockInsuficienteException;
import com.pedidos360.catalog.model.Producto;
import com.pedidos360.catalog.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;

    @Transactional(readOnly = true)
    public List<ProductoResponse> listar() {
        return productoRepository.findAll().stream()
                .map(ProductoResponse::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtener(Long id) {
        return ProductoResponse.desde(buscarPorId(id));
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        Producto producto = Producto.builder()
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .precio(request.getPrecio())
                .stock(request.getStock())
                .activo(true)
                .build();
        return ProductoResponse.desde(productoRepository.save(producto));
    }

    @Transactional
    public ProductoResponse editar(Long id, ProductoRequest request) {
        Producto producto = buscarPorId(id);
        producto.setNombre(request.getNombre());
        producto.setDescripcion(request.getDescripcion());
        producto.setPrecio(request.getPrecio());
        producto.setStock(request.getStock());
        return ProductoResponse.desde(productoRepository.save(producto));
    }

    /**
     * Descuento atómico de stock, invocado por ms-pedidos360-orders cuando un
     * pedido pasa a ACEPTADO. Vive acá (y no en Orders) porque el stock es
     * responsabilidad exclusiva de este servicio ahora que están separados.
     */
    @Transactional
    public ProductoResponse descontarStock(Long id, int cantidad) {
        Producto producto = buscarPorId(id);
        if (cantidad > producto.getStock()) {
            throw new StockInsuficienteException(
                    "Stock insuficiente para '" + producto.getNombre() + "': disponible="
                            + producto.getStock() + ", solicitado=" + cantidad);
        }
        producto.descontarStock(cantidad);
        return ProductoResponse.desde(productoRepository.save(producto));
    }

    private Producto buscarPorId(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado: id=" + id));
    }
}
