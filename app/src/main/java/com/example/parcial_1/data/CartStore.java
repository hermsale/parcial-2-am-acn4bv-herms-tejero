package com.example.parcial_1.data;

import com.example.parcial_1.model.CartItem;
import com.example.parcial_1.model.Product;

import java.util.ArrayList;
import java.util.List;

/**
 * Carrito en memoria (singleton) para compartir entre Activities sin backend.
 * NOTA: Esto es solo para demo, sin persistencia.
 */
public class CartStore {

    private static CartStore INSTANCE;
    private final List<CartItem> items = new ArrayList<>();

    private CartStore() {}

    public static synchronized CartStore get() {
        if (INSTANCE == null) INSTANCE = new CartStore();
        return INSTANCE;
    }

    public List<CartItem> getItems() { return items; }

    public void clear() { items.clear(); }

    public void add(Product p) {
        CartItem existing = findByProductName(p.name);
        if (existing == null) {
            items.add(new CartItem(p, 1));
        } else {
            existing.qty++;
        }
    }

    public void setQty(Product p, int qty) {
        if (qty < 0) qty = 0;
        CartItem existing = findByProductName(p.name);
        if (existing == null && qty > 0) {
            items.add(new CartItem(p, qty));
        } else if (existing != null) {
            if (qty == 0) items.remove(existing);
            else existing.qty = qty;
        }
    }

    public void inc(Product p) {
        CartItem existing = findByProductName(p.name);
        if (existing == null) items.add(new CartItem(p, 1));
        else existing.qty++;
    }

    public void dec(Product p) {
        CartItem existing = findByProductName(p.name);
        if (existing == null) return;
        existing.qty--;
        if (existing.qty <= 0) items.remove(existing);
    }

    public int getTotalAmount() {
        int total = 0;
        for (CartItem ci : items) total += ci.qty * ci.product.price;
        return total;
    }

    public int getTotalQty() {
        int q = 0;
        for (CartItem ci : items) q += ci.qty;
        return q;
    }

    private CartItem findByProductName(String name) {
        for (CartItem ci : items) if (ci.product.name.equals(name)) return ci;
        return null;
    }
}
