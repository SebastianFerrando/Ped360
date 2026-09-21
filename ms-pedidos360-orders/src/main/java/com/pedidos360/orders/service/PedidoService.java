package com.pedidos360.orders.service;

import com.pedidos360.orders.client.ProductoClient;
import com.pedidos360.orders.client.dto.ProductoDTO;
import com.pedidos360.orders.dto.CrearPedidoRequest;
import com.pedidos360.orders.dto.PedidoResponse;
import com.pedidos360.orders.exception.RecursoNoEncontradoException;
import com.pedidos360.orders.exception.TransicionEstadoInvalidaException;
import com.pedidos360.orders.model.EstadoPedido;
import com.pedidos360.orders.model.Pedido;
import com.pedidos360.orders.model.PedidoItem;
import com.pedidos360.orders.repository.PedidoRepository;
import com.pedidos360.orders.security.ActorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ProductoClient productoClient;
    private final ActorContext actorContext;

    @Transactional
    public PedidoResponse crear(CrearPedidoRequest request) {
        Pedido pedido = Pedido.builder()
                .clienteId(actorContext.obtenerIdActor())
                .estado(EstadoPedido.CREADO)
                .fechaCreacion(Instant.now())
                .build();

        for (CrearPedidoRequest.ItemPedidoRequest itemReq : request.getItems()) {
            // Se consulta Catálogo (otro servicio) para obtener nombre y precio
            // vigentes; se "congelan" en el pedido al momento de crearlo.
            ProductoDTO producto = productoClient.obtenerProducto(itemReq.getProductoId());

            PedidoItem item = PedidoItem.builder()
                    .productoId(producto.getId())
                    .nombreProducto(producto.getNombre())
                    .cantidad(itemReq.getCantidad())
                    .precioUnitario(producto.getPrecio())
                    .build();
            pedido.agregarItem(item);
        }

        return PedidoResponse.desde(pedidoRepository.save(pedido));
    }

    /**
     * Lista pedidos según el actor autenticado: un CLIENTE solo ve los suyos;
     * ADMIN/OPERADOR ven todos (para supervisión y gestión operativa).
     */
    @Transactional(readOnly = true)
    public List<PedidoResponse> listarSegunActor() {
        List<Pedido> pedidos = actorContext.esCliente()
                ? pedidoRepository.findByClienteId(actorContext.obtenerIdActor())
                : pedidoRepository.findAll();

        return pedidos.stream().map(PedidoResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public PedidoResponse obtenerDetalle(Long id) {
        Pedido pedido = buscarPorId(id);

        if (actorContext.esCliente() && !pedido.getClienteId().equals(actorContext.obtenerIdActor())) {
            throw new AccessDeniedException("No puedes consultar pedidos de otro cliente");
        }

        return PedidoResponse.desde(pedido);
    }

    @Transactional
    public PedidoResponse cambiarEstado(Long id, EstadoPedido nuevoEstado) {
        Pedido pedido = buscarPorId(id);
        EstadoPedido estadoActual = pedido.getEstado();

        if (!estadoActual.puedeTransicionarA(nuevoEstado)) {
            throw new TransicionEstadoInvalidaException(
                    "No se puede pasar de " + estadoActual + " a " + nuevoEstado
                            + " (recuerda: un pedido no puede DESPACHARSE si no fue ACEPTADO antes)");
        }

        // Regla mínima: al aceptar el pedido, se descuenta el stock en Catálogo.
        // Nota: esta llamada ya NO es parte de la misma transacción de BD que el
        // cambio de estado (son dos servicios, dos bases de datos distintas).
        // Si el descuento de stock falla, se aborta el cambio de estado local
        // (la transacción @Transactional hace rollback), pero un fallo de red
        // *después* de que Catálogo confirme el descuento podría dejar el pedido
        // sin actualizar su estado: es el tipo de caso que en EP2 se resuelve
        // mejor con un evento en RabbitMQ que con esta llamada síncrona.
        if (nuevoEstado == EstadoPedido.ACEPTADO) {
            for (PedidoItem item : pedido.getItems()) {
                productoClient.descontarStock(item.getProductoId(), item.getCantidad());
            }
        }

        pedido.setEstado(nuevoEstado);
        pedido.setFechaActualizacion(Instant.now());
        return PedidoResponse.desde(pedidoRepository.save(pedido));
    }

    private Pedido buscarPorId(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido no encontrado: id=" + id));
    }
}
