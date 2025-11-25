package com.example.parcial_1.data;

import com.example.parcial_1.model.CartItem;
import com.example.parcial_1.model.Product;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import java.util.Iterator;

/*
 * ============================================================
 * Archivo: CartStore.java
 * Paquete: com.example.parcial_1.data
 * ------------------------------------------------------------
 * ¿De qué se encarga?
 *   - Provee un "carrito" en memoria (patrón Singleton) para
 *     compartir el estado entre pantallas sin backend ni DB.
 *
 * ¿Qué clase contiene?
 *   - CartStore: única clase pública del archivo.
 *
 * ¿Qué métodos expone ?
 *   - static CartStore get(): obtiene la instancia única.
 *   - List<CartItem> getItems(): lista actual del carrito (solo lectura).
 *   - void clear(): vacía el carrito.
 *   - void add(Product p): agrega un producto (o incrementa si ya existe).
 *   - void setQty(Product p, int qty): fija cantidad; elimina si qty==0.
 *   - void inc(Product p): incrementa en 1 la cantidad del producto.
 *   - void dec(Product p): decrementa en 1; elimina si queda en 0.
 *   - int getTotalAmount(): total $ (suma de qty * price por ítem).
 *   - int getTotalQty(): total de unidades (suma de qty).
 *
 * ¿Cómo se relaciona con las vistas?
 *   - Vista Catálogo (MainActivity):
 *       * Llama a add(p) al presionar "Agregar".
 *       * Muestra resumen (getTotalQty / getTotalAmount).
 *   - Vista Carrito (CartActivity):
 *       * Dibuja la lista con getItems().
 *       * Usa inc/dec/setQty/clear para actualizar cantidades.
 *       * Vuelve a calcular totales con getTotalAmount().
 *
 * Notas:
 *   - Las búsquedas se hacen por nombre de producto (p.name).
 *
 * ============================================================
 */
public class CartStore {

    // ------------------------------
    // Singleton
    // ------------------------------
    private static CartStore INSTANCE;

    /**
     * Lista en memoria de líneas del carrito.
     * Se mantiene encapsulada; se devuelve copia inmutable en getItems().
     */
    private final List<CartItem> items = new ArrayList<>();

    /** Constructor privado para forzar Singleton. */
    private CartStore() { }

    /**
     * Devuelve la instancia única (thread-safe al crearla).
     */
    public static synchronized CartStore get() {
        if (INSTANCE == null) {
            INSTANCE = new CartStore();
        }
        return INSTANCE;
    }

    // ------------------------------
    // Lecturas
    // ------------------------------

    /**
     * Devuelve una vista inmutable de los ítems actuales del carrito.
     */
    public synchronized List<CartItem> getItems() {
        return Collections.unmodifiableList(new ArrayList<>(items));
    }

    /**
     * Retorna el monto total (sumatoria de qty * price).
     */
    public synchronized int getTotalAmount() {
        int total = 0;
        for (CartItem ci : items) {
            total += ci.qty * ci.product.price;
        }
        return total;
    }

    /**
     * Retorna la cantidad total de unidades en el carrito.
     */
    public synchronized int getTotalQty() {
        int q = 0;
        for (CartItem ci : items) {
            q += ci.qty;
        }
        return q;
    }

    // ------------------------------
    // Mutaciones
    // ------------------------------

    /**
     * Elimina todos los ítems del carrito.
     */
    public synchronized void clear() {
        items.clear();
    }

//    elimina 1 item del carrito
    public void remove(Product p) {
        if (p == null) return;
        Iterator<CartItem> iterator = items.iterator();
        while (iterator.hasNext()) {
            CartItem item = iterator.next();
            if (item.product != null && item.product.equals(p)) {
                iterator.remove();
                break;
            }
        }
    }

    /**
     * Agrega el producto p al carrito.
     * Si ya existe (por nombre), incrementa su cantidad.
     * Lanza IllegalArgumentException si p es nulo.
     */
    public synchronized void add(Product p) {
        validateProduct(p);
        CartItem existing = findByProductName(p.name);
        if (existing == null) {
            items.add(new CartItem(p, 1));
        } else {
            existing.qty++;
        }
    }

    /**
     * Fija la cantidad del producto p.
     * - Si qty < 0 => se normaliza a 0.
     * - Si no existe y qty > 0 => se crea la línea.
     * - Si existe y qty == 0 => se elimina la línea.
     * - Si existe y qty > 0 => se actualiza la cantidad.
     */
    public synchronized void setQty(Product p, int qty) {
        validateProduct(p);
        if (qty < 0) qty = 0;

        CartItem existing = findByProductName(p.name);
        if (existing == null && qty > 0) {
            items.add(new CartItem(p, qty));
        } else if (existing != null) {
            if (qty == 0) {
                items.remove(existing);
            } else {
                existing.qty = qty;
            }
        }
    }

    /**
     * Incrementa en 1 la cantidad del producto p.
     * Si el producto no existe, lo agrega con qty = 1.
     */
    public synchronized void inc(Product p) {
        validateProduct(p);
        CartItem existing = findByProductName(p.name);
        if (existing == null) {
            items.add(new CartItem(p, 1));
        } else {
            existing.qty++;
        }
    }

    /**
     * Decrementa en 1 la cantidad del producto p.
     * Si al decrementar queda en 0, elimina la línea.
     * Si no existe, no hace nada.
     */
    public synchronized void dec(Product p) {
        validateProduct(p);
        CartItem existing = findByProductName(p.name);
        if (existing == null) return;

        existing.qty--;
        if (existing.qty <= 0) {
            items.remove(existing);
        }
    }

    // ------------------------------
    // Utilitarios internos
    // ------------------------------

    /**
     * Busca una línea del carrito por nombre de producto (clave lógica actual).
     */
    private CartItem findByProductName(String name) {
        if (name == null) return null;
        for (CartItem ci : items) {
            if (ci.product != null && name.equals(ci.product.name)) {
                return ci;
            }
        }
        return null;
        // Alternativa: si existiera un "productId" único, comparar por ID.
    }

    /**
     * Valida que el producto no sea nulo y que tenga nombre no vacío.
     */
    private static void validateProduct(Product p) {
        if (p == null) {
            throw new IllegalArgumentException("Product no puede ser null");
        }
        if (p.name == null || p.name.trim().isEmpty()) {
            throw new IllegalArgumentException("Product.name no puede ser null/empty");
        }
    }
}
