document.addEventListener("DOMContentLoaded", function () {
    // ============================================
    // PARTE 1: FUNCIONALIDAD ORIGINAL DEL CARRITO
    // ============================================

    //    const cartList = document.getElementById('cart-list');
    const cartList = document.querySelector('.cart-items ul'); // Más específico
    const subtotalEl = document.getElementById('subtotal');
    const totalEl = document.getElementById('total');


    // Función para manejar el envío del formulario de crear pedido
    function manejarCrearPedido(event) {
        console.log("🛒 Iniciando creación de pedido...");

        // Prevenir el envío normal del formulario
        event.preventDefault();

        const form = document.getElementById('form-crear-pedido');
        const boton = document.getElementById('btn-realizar-pedido');

        if (!form || !boton) {
            console.error("No se encontró el formulario o el botón");
            return false;
        }

        // Guardar texto original del botón
        const textoOriginal = boton.innerHTML;

        // Mostrar estado de carga
        boton.innerHTML = '<i class="fa fa-spinner fa-spin"></i> Creando pedido...';
        boton.disabled = true;

        console.log("Enviando datos al servidor...");

        // Enviar el formulario usando Fetch API
        fetch(form.action, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: new URLSearchParams(new FormData(form))
        })
        .then(response => {
            console.log("Respuesta recibida, status:", response.status);

            if (response.redirected) {
                // Si el servidor redirige, seguir la redirección
                console.log("Redirigiendo a:", response.url);
                window.location.href = response.url;
            } else {
                // Si no hay redirección, intentar leer la respuesta
                return response.text().then(html => {
                    console.log("Contenido HTML recibido");
                    // Reemplazar el contenido actual con la respuesta
                    document.open();
                    document.write(html);
                    document.close();
                });
            }
        })
        .catch(error => {
            console.error("❌ Error al crear pedido:", error);

            // Restaurar el botón
            boton.innerHTML = textoOriginal;
            boton.disabled = false;

            // Mostrar mensaje de error
            alert('Error al crear el pedido. Por favor, intenta nuevamente.\n' + error.message);
        });

        return false;
    }
    // Mostrar notificación flotante
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
// Función para procesar el pago
function procesarPago() {
    console.log("🔍 procesarPago() llamado");

    // Buscar el ID del pedido de TODAS las formas posibles
    let pedidoId = null;

    // 1. Del campo oculto (si existe)
    const pedidoIdInput = document.getElementById('pedido-id');
    if (pedidoIdInput) {
        pedidoId = pedidoIdInput.value;
        console.log("1. Pedido ID del input: " + pedidoId);
    }

    // 2. De window.pedidoData (si existe)
    if (!pedidoId && window.pedidoData && window.pedidoData.id) {
        pedidoId = window.pedidoData.id;
        console.log("2. Pedido ID de window.pedidoData: " + pedidoId);
    }

    // 3. De window.pedidoIdFromModel (si existe)
    if (!pedidoId && window.pedidoIdFromModel) {
        pedidoId = window.pedidoIdFromModel;
        console.log("3. Pedido ID de window.pedidoIdFromModel: " + pedidoId);
    }

    // 4. De la URL
    if (!pedidoId) {
        const urlParams = new URLSearchParams(window.location.search);
        pedidoId = urlParams.get('pedidoId');
        console.log("4. Pedido ID de URL: " + pedidoId);
    }

    // Si aún no tenemos ID
    if (!pedidoId) {
        alert('❌ No se pudo encontrar el ID del pedido. Por favor:\n1. Vuelve al carrito\n2. Haz clic en "Realizar Pedido" nuevamente');
        console.error("No se encontró ID de pedido");
        return;
    }

    console.log("✅ Pedido ID encontrado: " + pedidoId);

    // Verificar si ya tenemos paymentUrl
    if (window.paymentUrl) {
        console.log("✅ Redirigiendo a paymentUrl existente");
        window.location.href = window.paymentUrl;
        return;
    }

    // Mostrar loading
    const btnPagar = document.querySelector('#form-pagar-pedido button');
    if (btnPagar) {
        const originalText = btnPagar.innerHTML;
        btnPagar.innerHTML = '<i class="fa fa-spinner fa-spin"></i> Conectando...';
        btnPagar.disabled = true;

        // Llamar al backend
        console.log("🔄 Llamando a /pedido/iniciar-mercadopago/" + pedidoId);

        fetch('/pedido/iniciar-mercadopago/' + pedidoId, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            }
        })
        .then(response => {
            console.log("Response status:", response.status);
            if (!response.ok) {
                throw new Error('Error del servidor: ' + response.status);
            }
            return response.json();
        })
        .then(data => {
            console.log("Respuesta:", data);

            if (data.success && data.paymentUrl) {
                console.log("✅ Redirigiendo a MercadoPago");
                window.location.href = data.paymentUrl;
            } else {
                throw new Error(data.error || 'No se pudo obtener URL de pago');
            }
        })
        .catch(error => {
            console.error('❌ Error:', error);
            alert('Error al procesar el pago:\n' + error.message +
                  '\n\nPor favor, contacta soporte o intenta nuevamente.');

            // Restaurar botón
            btnPagar.innerHTML = originalText;
            btnPagar.disabled = false;
        });
    }
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

//para procesar pago con MercadoPago
function procesarPagoConMercadoPago(pedidoId) {
    console.log("Iniciando pago MercadoPago para pedido #" + pedidoId);

    // Mostrar loading
    const btnPagar = document.querySelector('#form-pagar-pedido button');
    if (btnPagar) {
        const originalText = btnPagar.innerHTML;
        btnPagar.innerHTML = '<i class="fa fa-spinner fa-spin"></i> Conectando con MercadoPago...';
        btnPagar.disabled = true;
    }

    // Llamar al endpoint para iniciar pago con MercadoPago
    fetch('/pedido/iniciar-mercadopago/' + pedidoId, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        }
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Error en la respuesta del servidor');
        }
        return response.json();
    })
    .then(data => {
        if (data.success) {
            // Redirigir a MercadoPago
            console.log("Redirigiendo a MercadoPago:", data.paymentUrl);
            window.location.href = data.paymentUrl;
        } else {
            alert('Error al procesar el pago: ' + data.error);
            if (btnPagar) {
                btnPagar.innerHTML = 'Pagar ahora';
                btnPagar.disabled = false;
            }
        }
    })
    .catch(error => {
        console.error('Error:', error);
        alert('Error al conectar con MercadoPago: ' + error.message);
        if (btnPagar) {
            btnPagar.innerHTML = 'Pagar ahora';
            btnPagar.disabled = false;
        }
    });
}

// Modificar la función procesarPago existente
function procesarPago() {
    const pedidoId = document.getElementById('pedido-id').value;

    if (!pedidoId) {
        alert('No hay un pedido para pagar');
        return;
    }

    console.log("Procesando pago para pedido #" + pedidoId);
    procesarPagoConMercadoPago(pedidoId);
}

// Agregar al DOMContentLoaded
document.addEventListener('DOMContentLoaded', function() {
    // ... código existente ...

    // Si estamos en el paso 2 y tenemos paymentUrl de MercadoPago
    if (window.paymentUrl && window.location.search.includes('step=2')) {
        console.log("Tiene URL de MercadoPago, redirigiendo automáticamente...");
        // Redirigir automáticamente después de 2 segundos
        setTimeout(() => {
            window.location.href = window.paymentUrl;
        }, 2000);
    }
});

// Función para reintentar MercadoPago
function reintentarMercadoPago() {
    const pedidoId = document.getElementById('pedido-id')?.value ||
                     new URLSearchParams(window.location.search).get('pedidoId');

    if (!pedidoId) {
        alert('No se encontró el ID del pedido');
        return;
    }

    console.log("🔄 Reintentando MercadoPago para pedido #" + pedidoId);

    // Mostrar loading
    const btn = event.target;
    const originalText = btn.innerHTML;
    btn.innerHTML = '<i class="fa fa-spinner fa-spin"></i> Reintentando...';
    btn.disabled = true;

    // Llamar al endpoint para generar nueva preferencia
    fetch('/pedido/iniciar-mercadopago/' + pedidoId, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        }
    })
    .then(response => response.json())
    .then(data => {
        console.log("Respuesta:", data);

        if (data.success && data.paymentUrl) {
            // Redirigir a MercadoPago
            window.location.href = data.paymentUrl;
        } else {
            alert('Error al generar pago: ' + (data.error || 'Error desconocido'));
            btn.innerHTML = originalText;
            btn.disabled = false;
        }
    })
    .catch(error => {
        console.error('Error:', error);
        alert('Error de conexión: ' + error.message);
        btn.innerHTML = originalText;
        btn.disabled = false;
    });
}