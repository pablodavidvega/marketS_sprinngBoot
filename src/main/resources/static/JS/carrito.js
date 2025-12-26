document.addEventListener("DOMContentLoaded", function () {
    // ============================================
    // PARTE 1: FUNCIONALIDAD ORIGINAL DEL CARRITO
    // ============================================

    //    const cartList = document.getElementById('cart-list');
    const cartList = document.querySelector('.cart-items ul'); // Más específico
    const subtotalEl = document.getElementById('subtotal');
    const totalEl = document.getElementById('total');

    // Mostrar notificación flotante estilo Temu
    function mostrarNotificacion(mensaje) {
        const noti = document.createElement('div');
        noti.className = 'hana-toast';
        noti.textContent = mensaje;
        document.body.appendChild(noti);
        setTimeout(() => noti.classList.add('visible'), 100);
        setTimeout(() => {
            noti.classList.remove('visible');
            setTimeout(() => noti.remove(), 300);
        }, 2000);
    }

    // Formato moneda
    function formatear(valor) {
        return valor.toLocaleString('es-CO', { minimumFractionDigits: 2 });
    }

    //Agregar producto al carrito
    document.querySelectorAll('.add-to-cart').forEach(button => {
        button.addEventListener('click', function (event) {
            event.preventDefault();

            const id = this.dataset.id;
            const nombre = this.dataset.name;
            const precio = parseFloat(this.dataset.price);

            fetch('/carrito/agregar', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-CSRF-TOKEN': document.querySelector('meta[name="csrf-token"]').content
                },
                body: JSON.stringify({ id, nombre, precio })
            })
                .then(res => res.json())
                .then(data => {
                    if (data.carrito) {
                        actualizarCarrito(data.carrito);
                        subtotalEl.textContent = formatear(data.subtotal);
                        totalEl.textContent = formatear(data.subtotal);
                        mostrarNotificacion('Producto agregado al carrito');
                    }
                })
                .catch(err => console.error('Error al agregar:', err));
        });
    });

    //Actualizar la vista del carrito
    function actualizarCarrito(carrito) {
        cartList.innerHTML = '';

        Object.values(carrito).forEach(producto => {
            const li = document.createElement('li');
            li.className = 'cart-item';
            li.innerHTML = `
                <img src="/images/${producto.imagen}" alt="${producto.nombre}" class="product-image">
                <div class="item-details">
                    <strong>${producto.nombre}</strong>
                    <p>Precio: $<span class="precio" data-id="${producto.id}">${formatear(producto.precio)}</span></p>
                    <div class="quantity-controls">
                        <button class="btn decrease" data-id="${producto.id}">-</button>
                        <span class="cantidad" data-id="${producto.id}">${producto.cantidad}</span>
                        <button class="btn increase" data-id="${producto.id}">+</button>
                    </div>
                    <p>Total: $<span class="total-product" data-id="${producto.id}">${formatear(producto.precio * producto.cantidad)}</span></p>
                    <form action="/carrito/eliminar/${producto.id}" method="POST">
                        <input type="hidden" name="_token" value="${document.querySelector('meta[name="csrf-token"]').content}">
                        <button type="submit" class="btn btn-danger">Eliminar</button>
                    </form>
                </div>
            `;
            cartList.appendChild(li);
        });

        activarBotonesCantidad();
    }

    //Recalcular el total
    function recalcularTotales() {
        let subtotal = 0;
        document.querySelectorAll(".total-product").forEach(el => {
            const valor = parseFloat(el.textContent.replace(/\./g, '').replace(/,/g, '.')) || 0;
            subtotal += valor;
        });
        subtotalEl.textContent = formatear(subtotal);
        totalEl.textContent = formatear(subtotal);
    }

    //Botones de + y -
    function activarBotonesCantidad() {
        document.querySelectorAll(".increase").forEach(btn => {
            btn.onclick = () => {
                const id = btn.dataset.id;
                const cantidadEl = document.querySelector(`.cantidad[data-id="${id}"]`);
                const precio = parseFloat(document.querySelector(`.precio[data-id="${id}"]`).textContent.replace(/\./g, '').replace(/,/g, '.'));

                let nuevaCantidad = parseInt(cantidadEl.textContent) + 1;
                cantidadEl.textContent = nuevaCantidad;
                document.querySelector(`.total-product[data-id="${id}"]`).textContent = formatear(precio * nuevaCantidad);
                recalcularTotales();
            };
        });

        document.querySelectorAll(".decrease").forEach(btn => {
            btn.onclick = () => {
                const id = btn.dataset.id;
                const cantidadEl = document.querySelector(`.cantidad[data-id="${id}"]`);
                const precio = parseFloat(document.querySelector(`.precio[data-id="${id}"]`).textContent.replace(/\./g, '').replace(/,/g, '.'));

                let nuevaCantidad = Math.max(1, parseInt(cantidadEl.textContent) - 1);
                cantidadEl.textContent = nuevaCantidad;
                document.querySelector(`.total-product[data-id="${id}"]`).textContent = formatear(precio * nuevaCantidad);
                recalcularTotales();
            };
        });
    }

    // Inicialización en caso de render inicial del backend
    activarBotonesCantidad();

    // ============================================
    // PARTE 2: FUNCIONALIDAD DE CHECKOUT (NUEVO)
    // ============================================

    // Interceptar el formulario de crear pedido para hacerlo con AJAX
    const formCrearPedido = document.getElementById('form-crear-pedido');

    if (formCrearPedido) {
        formCrearPedido.addEventListener('submit', function(e) {
            e.preventDefault();

            // Validar que el carrito no esté vacío
            const carritoVacio = document.querySelector('.cart-items ul li p')?.textContent === 'El carrito está vacío.';
            if (carritoVacio) {
                mostrarNotificacion('Tu carrito está vacío. Agrega productos antes de continuar.');
                return;
            }

            // Mostrar loading
            const btn = this.querySelector('button[type="submit"]');
            const originalText = btn.innerHTML;
            btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Creando pedido...';
            btn.disabled = true;

            // Usar endpoint AJAX (versión simplificada que funciona con tu controlador actual)
            fetch('/pedido/crear', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]')?.content ||
                                   document.querySelector('input[name="_csrf"]')?.value
                }
            })
            .then(response => {
                if (response.redirected) {
                    // Si hay redirección, seguimos el flujo normal
                    window.location.href = response.url;
                } else {
                    return response.text();
                }
            })
            .then(html => {
                if (html) {
                    // Parsear el HTML para extraer los datos del pedido
                    const parser = new DOMParser();
                    const doc = parser.parseFromString(html, 'text/html');

                    // Extraer datos del pedido del HTML (pago.html)
                    const pedidoElement = doc.querySelector('.pedido-card');
                    if (pedidoElement) {
                        const pedidoId = pedidoElement.querySelector('[th\\:text="${pedido.id}"]')?.textContent ||
                                        pedidoElement.querySelector('span')?.textContent;
                        const pedidoFecha = pedidoElement.querySelector('[th\\:text="${pedido.fecha}"]')?.textContent ||
                                           new Date().toLocaleString();
                        const pedidoEstado = pedidoElement.querySelector('[th\\:text="${pedido.estado}"]')?.textContent || 'PENDIENTE';
                        const pedidoTotal = pedidoElement.querySelector('[th\\:text="${pedido.total}"]')?.textContent ||
                                           document.getElementById('total-final')?.textContent;

                        // Extraer detalles de la tabla
                        const detalles = [];
                        const filas = doc.querySelectorAll('.tabla-pedido tbody tr');
                        filas.forEach(fila => {
                            const celdas = fila.querySelectorAll('td');
                            if (celdas.length >= 4) {
                                detalles.push({
                                    producto: celdas[0].textContent.trim(),
                                    cantidad: celdas[1].textContent.trim(),
                                    precio: celdas[2].textContent.trim().replace('$', ''),
                                    subtotal: celdas[3].textContent.trim().replace('$', '')
                                });
                            }
                        });

                        // Llenar la tab de pago con los datos
                        llenarPasoPago({
                            id: pedidoId,
                            fecha: pedidoFecha,
                            estado: pedidoEstado,
                            total: pedidoTotal,
                            detalles: detalles
                        });

                        // Ir al step de pago
                        mostrarStep(2);
                    } else {
                        // Si no se pudo extraer, usar datos básicos
                        llenarPasoPagoBasico();
                        mostrarStep(2);
                    }
                }
            })
            .catch(error => {
                console.error('Error:', error);
                mostrarNotificacion('Error al crear el pedido: ' + error.message);
            })
            .finally(() => {
                btn.innerHTML = originalText;
                btn.disabled = false;
            });
        });
    }

    // Función para llenar el paso de pago con datos básicos (si falla la extracción)
    function llenarPasoPagoBasico() {
        // Información básica del pedido
        const pedidoId = 'PED-' + Date.now().toString().substr(-6);
        const pedidoFecha = new Date().toLocaleString();
        const pedidoTotal = document.getElementById('total-final')?.textContent || '0';

        // Extraer detalles del carrito actual
        const detalles = [];
        document.querySelectorAll('.cart-item').forEach(item => {
            const nombre = item.querySelector('strong').textContent;
            const cantidad = item.querySelector('.cantidad').textContent;
            const precio = item.querySelector('.precio').textContent.replace(/\./g, '').replace(/,/g, '.');
            const subtotal = item.querySelector('.total-product').textContent.replace(/\./g, '').replace(/,/g, '.');

            detalles.push({
                producto: nombre,
                cantidad: cantidad,
                precio: precio,
                subtotal: subtotal
            });
        });

        llenarPasoPago({
            id: pedidoId,
            fecha: pedidoFecha,
            estado: 'PENDIENTE',
            total: pedidoTotal,
            detalles: detalles
        });
    }

    // ============================================
    // PARTE 3: MODO OSCURO (EXISTENTE)
    // ============================================

    const toggleDarkModeBtn = document.getElementById("toggle-dark-mode");

    const savedTheme = localStorage.getItem("theme");
    if (savedTheme === "dark-mode") {
        document.body.classList.add("dark-mode");
    } else {
        document.body.classList.remove("dark-mode");
    }

    toggleDarkModeBtn.addEventListener("click", function () {
        document.body.classList.toggle("dark-mode");

        if (document.body.classList.contains("dark-mode")) {
            localStorage.setItem("theme", "dark-mode");
        } else {
            localStorage.setItem("theme", "light");
        }
    });
});

// ============================================
// FUNCIONES GLOBALES PARA EL CHECKOUT
// ============================================

// Funciones para manejar los steps del checkout
function mostrarStep(stepNumber) {
    // Ocultar todos los steps
    document.querySelectorAll('.step-content').forEach(step => {
        step.style.display = 'none';
        step.classList.remove('active');
    });

    // Mostrar el step seleccionado
    const stepToShow = document.getElementById(`step-${stepNumber}`);
    if (stepToShow) {
        stepToShow.style.display = 'flex';
        stepToShow.classList.add('active');
    }

    // Actualizar las tabs
    document.querySelectorAll('.step').forEach(step => {
        step.classList.remove('active');
    });

    const stepTab = document.getElementById(`step-${stepNumber}-tab`);
    if (stepTab) {
        stepTab.classList.add('active');
    }
}

// Llenar el paso de pago con los datos del pedido
function llenarPasoPago(pedido) {
    // Información del pedido
    const pagoPedidoInfo = document.getElementById('pago-pedido-info');
    if (pagoPedidoInfo) {
        pagoPedidoInfo.innerHTML = `
            <p><strong>N° Pedido:</strong> <span>${pedido.id}</span></p>
            <p><strong>Fecha:</strong> <span>${pedido.fecha}</span></p>
            <p><strong>Estado:</strong> <span>${pedido.estado}</span></p>
        `;
    }

    // Guardar ID del pedido para el pago
    const pedidoIdInput = document.getElementById('pedido-id');
    if (pedidoIdInput) {
        // Extraer solo el número si es algo como "PED-123"
        const soloNumero = pedido.id.toString().replace(/\D/g, '');
        pedidoIdInput.value = soloNumero || pedido.id;
    }

    // Tabla de productos
    const tablaBody = document.getElementById('pago-tabla-body');
    if (tablaBody) {
        tablaBody.innerHTML = '';

        if (pedido.detalles && pedido.detalles.length > 0) {
            pedido.detalles.forEach(detalle => {
                const fila = document.createElement('tr');
                fila.innerHTML = `
                    <td>${detalle.producto}</td>
                    <td>${detalle.cantidad}</td>
                    <td>$${parseFloat(detalle.precio).toLocaleString('es-CO', {minimumFractionDigits: 2})}</td>
                    <td>$${parseFloat(detalle.subtotal).toLocaleString('es-CO', {minimumFractionDigits: 2})}</td>
                `;
                tablaBody.appendChild(fila);
            });
        }
    }

    // Total
    const pagoTotal = document.getElementById('pago-total');
    if (pagoTotal) {
        pagoTotal.textContent = pedido.total ||
            document.getElementById('total-final')?.textContent || '0';
    }
}

// Procesar el pago del pedido
function procesarPago() {
    const pedidoId = document.getElementById('pedido-id')?.value;

    if (!pedidoId) {
        alert('No hay un pedido para pagar');
        return;
    }

    const btn = document.querySelector('#form-pagar-pedido button');
    if (!btn) return;

    const originalText = btn.innerHTML;
    btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Procesando pago...';
    btn.disabled = true;

    // Usar el endpoint existente de pago
    fetch(`/pedido/pagar/${pedidoId}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]')?.content ||
                           document.querySelector('input[name="_csrf"]')?.value
        }
    })
    .then(response => {
        if (response.redirected) {
            // Si hay redirección, obtener la página de confirmación
            return fetch(response.url);
        } else {
            return response.text();
        }
    })
    .then(html => {
        if (html) {
            const parser = new DOMParser();
            const doc = parser.parseFromString(html, 'text/html');

            // Extraer datos de confirmación de confirmacion.html
            const confirmacionElement = doc.querySelector('.pedido-card');
            if (confirmacionElement) {
                const confirmacionId = confirmacionElement.querySelector('strong')?.textContent ||
                                     pedidoId;
                const confirmacionEstado = confirmacionElement.querySelector('[th\\:text="${pedido.estado}"]')?.textContent ||
                                         'PAGADO';
                const confirmacionTotal = confirmacionElement.querySelector('[th\\:text="${pedido.total}"]')?.textContent ||
                                        document.getElementById('pago-total')?.textContent;

                // Llenar paso de confirmación
                llenarPasoConfirmacion({
                    id: confirmacionId,
                    estado: confirmacionEstado,
                    total: confirmacionTotal
                });

                // Ir al step de confirmación
                mostrarStep(3);

                // Limpiar carrito visualmente
                limpiarCarritoVisual();
            } else {
                // Si no se pudo extraer, usar datos básicos
                llenarPasoConfirmacionBasico(pedidoId);
                mostrarStep(3);
                limpiarCarritoVisual();
            }
        }
    })
    .catch(error => {
        console.error('Error:', error);
        alert('Error al procesar el pago: ' + error.message);
    })
    .finally(() => {
        btn.innerHTML = originalText;
        btn.disabled = false;
    });
}

// Llenar el paso de confirmación con datos básicos
function llenarPasoConfirmacionBasico(pedidoId) {
    const confirmacionTotal = document.getElementById('pago-total')?.textContent ||
                             document.getElementById('total-final')?.textContent || '0';

    llenarPasoConfirmacion({
        id: pedidoId,
        estado: 'PAGADO',
        total: confirmacionTotal
    });
}

// Llenar el paso de confirmación
function llenarPasoConfirmacion(pedido) {
    const confirmacionMensaje = document.getElementById('confirmacion-mensaje');
    if (confirmacionMensaje) {
        confirmacionMensaje.innerHTML = `
            Tu pedido número <strong>${pedido.id}</strong>
            ha sido pagado correctamente.
        `;
    }

    const confirmacionPedidoInfo = document.getElementById('confirmacion-pedido-info');
    if (confirmacionPedidoInfo) {
        confirmacionPedidoInfo.innerHTML = `
            <p><strong>Estado:</strong> <span>${pedido.estado}</span></p>
            <p><strong>Total pagado:</strong> $<span>${pedido.total}</span></p>
        `;
    }
}

// Limpiar carrito visualmente
function limpiarCarritoVisual() {
    // Limpiar lista de productos
    const listaCarrito = document.querySelector('.cart-items ul');
    if (listaCarrito) {
        listaCarrito.innerHTML = '<li><p>El carrito está vacío.</p></li>';
    }

    // Limpiar totales
    document.querySelectorAll('.totals span').forEach(span => {
        span.textContent = '0,00';
    });

    // Ocultar botón de realizar pedido
    const btnRealizarPedido = document.querySelector('#form-crear-pedido button');
    if (btnRealizarPedido) {
        btnRealizarPedido.style.display = 'none';
    }

    // Mostrar notificación
    if (typeof mostrarNotificacion === 'function') {
        mostrarNotificacion('¡Pedido confirmado! El carrito ha sido vaciado.');
    }
}

// Función auxiliar para obtener el token CSRF
function getCSRFToken() {
    return document.querySelector('meta[name="_csrf"]')?.content ||
           document.querySelector('input[name="_csrf"]')?.value ||
           document.querySelector('meta[name="csrf-token"]')?.content;
}