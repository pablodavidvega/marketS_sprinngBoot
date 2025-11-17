/**
 * @jest-environment jsdom
 */

const { recalcularTotales } = require('../../js/carrito');

describe("recalcularTotales", () => {

    beforeEach(() => {
        document.body.innerHTML = `
            <span id="subtotal"></span>
            <span id="total"></span>

            <span class="total-product">10.000</span>
            <span class="total-product">5.000</span>
        `;

        global.subtotalEl = document.getElementById("subtotal");
        global.totalEl = document.getElementById("total");

        global.formatear = (num) => num.toString();
    });

    test("calcula correctamente el subtotal y actualiza el DOM", () => {
        recalcularTotales();

      expect(subtotalEl.textContent).toBe("15.000,00");
      expect(totalEl.textContent).toBe("15.000,00");

    });

});
