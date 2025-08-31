
image.png
image.png
document.addEventListener("DOMContentLoaded", function () {
    const cartList = document.getElementById('cart-list');
    const subtotalEl = document.getElementById('subtotal');
    const totalEl = document.getElementById('total');

    // ✅ Mostrar notificación flotante estilo Temu
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

    // ✅ Formato moneda
    function formatear(valor) {
        return valor.toLocaleString('es-CO', { minimumFractionDigits: 2 });
    }

    // ✅ Agregar producto al carrito
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

    // ✅ Actualizar la vista del carrito
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

    // ✅ Recalcular el total
    function recalcularTotales() {
        let subtotal = 0;
        document.querySelectorAll(".total-product").forEach(el => {
            const valor = parseFloat(el.textContent.replace(/\./g, '').replace(/,/g, '.')) || 0;
            subtotal += valor;
        });
        subtotalEl.textContent = formatear(subtotal);
        totalEl.textContent = formatear(subtotal);
    }

    // ✅ Botones de + y -
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
});

