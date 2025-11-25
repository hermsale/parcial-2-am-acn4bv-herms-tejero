package com.example.parcial_1.model;

/*
 * ============================================================
 * Archivo: CartItem.java
 * Paquete: com.example.parcial_1.model
 * ------------------------------------------------------------
 * ¿De qué se encarga?
 *   - Representa una línea individual del carrito de compras.
 *     Cada instancia asocia un objeto Product con una cantidad (qty).
 *
 * ¿Qué clase contiene?
 *   - CartItem: clase pública simple (modelo de datos).
 *
 * ¿Qué atributos tiene?
 *   - Product product → referencia al producto asociado.
 *   - int qty → cantidad de unidades del producto en el carrito.
 *
 * ¿Qué métodos expone?
 *   - Constructor CartItem(Product product, int qty)
 *     Crea un ítem de carrito con validaciones básicas.
 *
 * ¿Qué función cumple en las vistas?
 *   - En la vista Catálogo (MainActivity):
 *       * Se crea indirectamente al agregar productos al carrito
 *         mediante CartStore.add() o inc().
 *   - En la vista Carrito (CartActivity):
 *       * Se muestra cada CartItem como una fila del listado dinámico.
 *       * Los botones +, -, o Subir PDF modifican su qty.
 *
 * Notas:
 *   - Es una clase de datos (modelo) sin dependencias de UI.
 *   - La validación en el constructor previene estados inválidos.
 * ============================================================
 */

public class CartItem {

    /** Producto asociado a este ítem del carrito. */
    public final Product product;

    /** Cantidad de unidades del producto (mínimo 1). */
    public int qty;

    /**
     * Crea un nuevo ítem del carrito.
     *
     * @param product producto asociado (no debe ser null)
     * @param qty     cantidad inicial (si es menor que 1, se normaliza a 1)
     * @throws IllegalArgumentException si el producto es null
     */
    public CartItem(Product product, int qty) {
        if (product == null) {
            throw new IllegalArgumentException("El producto no puede ser null");
        }
        this.product = product;
        this.qty = (qty < 1) ? 1 : qty;
    }

    /**
     * Devuelve una representación legible del ítem.
     */
    @Override
    public String toString() {
        return "CartItem{producto=" + product.name + ", cantidad=" + qty + "}";
    }
}
