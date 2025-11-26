package com.example.lamontana;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.PopupMenu;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.lamontana.data.CartStore;
import com.example.lamontana.model.CartItem;
import com.example.lamontana.model.Product;
import com.example.lamontana.ui.LoginActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/*
 * ============================================================
 * Archivo: CartActivity.java
 * Paquete: com.example.lamontana
 * ------------------------------------------------------------
 * ¿De qué se encarga?
 *   - Controla la pantalla "Carrito". Muestra los ítems agregados,
 *     permite modificar cantidades (+/-), eliminar productos del carrito
 *     y realizar todos los pedidos (simulado).
 *   - Verifica que el usuario esté logueado con Firebase Auth antes
 *     de mostrar el carrito.
 *
 * ¿Qué clases usa?
 *   - CartStore (data): singleton en memoria con el estado del carrito.
 *   - CartItem/Product (model): modelos de dominio que se renderizan en UI.
 *   - FirebaseAuth/FirebaseUser: para comprobar que haya usuario autenticado.
 *
 * Métodos principales:
 *   - onCreate():
 *       * Verifica que el usuario esté logueado (ensureUserLoggedIn()).
 *       * Si no hay usuario -> redirige a LoginActivity y termina.
 *       * Si hay usuario -> configura la UI y dibuja el carrito.
 *   - ensureUserLoggedIn():
 *       * Consulta FirebaseAuth y, si no hay usuario, navega a LoginActivity.
 *   - renderCart():
 *       * Infla dinámicamente filas (item_cart_detail) por cada CartItem.
 *       * Conecta listeners de +/-, eliminar producto y “realizar todos”.
 *   - rebindRowAfterChange(View, Product):
 *       * Refresca cantidad/total de una fila tras un cambio puntual.
 *   - updateGrandTotal():
 *       * Recalcula total global y habilita/deshabilita el botón “Realizar todos”.
 *   - onPlaceAllOrders():
 *       * Simula realizar todos los pedidos y limpia el carrito.
 *
 * ¿Cómo se relaciona con otras vistas?
 *   - Viene desde Catálogo (MainActivity) vía "Ver carrito".
 *   - Comparte el estado con MainActivity a través de CartStore.
 *
 * Notas:
 *   - No hay backend; todo es en memoria.
 *   - Se eliminaron referencias a botones que no existen en el layout
 *     (btnDetectFromPdf y btnPlaceOrder) para evitar errores de compilación.
 * ============================================================
 */
public class CartActivity extends AppCompatActivity {

    // Contenedor vertical donde se agregan dinámicamente las filas del carrito
    private LinearLayout llCartListContainer;

    // Texto de total general ($)
    private TextView tvCartGrandTotal;

    // Formateador de moneda en ARS
    private final NumberFormat ars = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1) Verificar si el usuario está logueado en Firebase Auth.
        if (!ensureUserLoggedIn()) {
            return;
        }

        setContentView(R.layout.activity_cart);

        // ---- Toolbar / AppBar (si está presente en el layout) ----
        Toolbar toolbar = findViewById(R.id.appToolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                // Dejamos el título vacío para posible “user chip” futuro
                getSupportActionBar().setTitle("");
            }
            // Click del ícono hamburguesa (derecha) -> opciones
            toolbar.setOnMenuItemClickListener(this::onToolbarMenuClick);
        }

        // ---- Referencias de UI ----
        llCartListContainer = findViewById(R.id.llCartListContainer);
        tvCartGrandTotal = findViewById(R.id.tvCartGrandTotal);

        MaterialButton btnBack = findViewById(R.id.btnBackToCatalog);
        if (btnBack != null) {
            // Volver a Catálogo (cierra la Activity actual)
            btnBack.setOnClickListener(v -> finish());
        }

        MaterialButton btnPlaceAll = findViewById(R.id.btnPlaceAllOrders);
        if (btnPlaceAll != null) {
            btnPlaceAll.setOnClickListener(v -> onPlaceAllOrders());
        }

        // Primer render de la lista
        renderCart();
    }

    /**
     * Verifica si hay un usuario logueado en FirebaseAuth.
     * - Si NO hay usuario:
     *     * Redirige a LoginActivity.
     *     * Cierra esta Activity (finish()).
     *     * Devuelve false para indicar que no se debe continuar.
     * - Si hay usuario:
     *     * Devuelve true y permite seguir con la configuración de la UI.
     */
    private boolean ensureUserLoggedIn() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return false;
        }
        return true;
    }

    /**
     * Maneja el menú de la toolbar (ícono hamburguesa).
     * Despliega un PopupMenu con navegación a Catálogo/Carrito.
     */
    private boolean onToolbarMenuClick(MenuItem item) {
        if (item.getItemId() == R.id.action_nav) {
            View anchor = findViewById(R.id.action_nav);
            if (anchor == null) anchor = findViewById(R.id.appToolbar);

            PopupMenu pm = new PopupMenu(this, anchor);
            pm.getMenu().add(0, 1, 0, getString(R.string.menu_catalog));
            pm.getMenu().add(0, 2, 1, getString(R.string.menu_cart));

            pm.setOnMenuItemClickListener(m -> {
                if (m.getItemId() == 1) {
                    // Ir a Catálogo
                    startActivity(new Intent(this, MainActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                    return true;
                } else if (m.getItemId() == 2) {
                    // Ya estamos en Carrito: refrescamos
                    renderCart();
                    return true;
                }
                return false;
            });
            pm.show();
            return true;
        }
        return false;
    }

    /**
     * Dibuja toda la lista del carrito.
     * - Limpia el contenedor y vuelve a inflar una fila por cada CartItem.
     * - Conecta listeners de +/-, eliminar producto.
     */
    private void renderCart() {
        if (llCartListContainer == null) return;

        llCartListContainer.removeAllViews();

        List<CartItem> items = CartStore.get().getItems();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (CartItem ci : items) {
            // Infla la plantilla visual de cada ítem del carrito
            View row = inflater.inflate(R.layout.item_cart_detail, llCartListContainer, false);

            // Referencias a sub-vistas
            ImageView iv = row.findViewById(R.id.ivThumb);
            TextView tvName = row.findViewById(R.id.tvName);
            TextView tvDesc = row.findViewById(R.id.tvDesc);
            TextView tvUnitPrice = row.findViewById(R.id.tvUnitPrice);
            TextView tvItemTotal = row.findViewById(R.id.tvItemTotal);
            TextView tvQty = row.findViewById(R.id.tvQty);
            MaterialButton btnMinus = row.findViewById(R.id.btnMinus);
            MaterialButton btnPlus = row.findViewById(R.id.btnPlus);
            // Botón para eliminar items (este sí existe en tu layout)
            MaterialButton btnRemoveItem = row.findViewById(R.id.btnRemoveItem);

            Product p = ci.product;
            if (p == null) continue; // defensa

            // Bind básico de datos a la UI
            if (iv != null) iv.setImageResource(p.imageRes);
            if (tvName != null) tvName.setText(p.name);
            if (tvDesc != null) tvDesc.setText(p.desc);
            if (tvUnitPrice != null) {
                tvUnitPrice.setText(
                        getString(R.string.unit_price_format, ars.format(p.price))
                );
            }
            if (tvQty != null) tvQty.setText(String.valueOf(ci.qty));
            if (tvItemTotal != null) {
                tvItemTotal.setText(
                        getString(R.string.item_total_format, ars.format(ci.qty * p.price))
                );
            }

            // Listener: disminuir cantidad
            if (btnMinus != null) {
                btnMinus.setOnClickListener(v -> {
                    CartStore.get().dec(p);
                    rebindRowAfterChange(row, p);
                    updateGrandTotal();
                });
            }

            // Listener: aumentar cantidad
            if (btnPlus != null) {
                btnPlus.setOnClickListener(v -> {
                    CartStore.get().inc(p);
                    rebindRowAfterChange(row, p);
                    updateGrandTotal();
                });
            }

            // Listener: eliminar producto del carrito
            if (btnRemoveItem != null) {
                btnRemoveItem.setOnClickListener(v -> {
                    new AlertDialog.Builder(this)
                            .setTitle("Eliminar producto")
                            .setMessage("¿Deseás quitar \"" + p.name + "\" del carrito?")
                            .setPositiveButton("Eliminar", (dialog, which) -> {
                                CartStore.get().remove(p); // elimina del carrito
                                renderCart(); // repinta la vista
                            })
                            .setNegativeButton("Cancelar", null)
                            .show();
                });
            }

            // Agregamos la fila ya configurada
            llCartListContainer.addView(row);
        }

        // Total global al terminar de pintar
        updateGrandTotal();
    }

    /**
     * Refresca la cantidad y el total de una fila específica después de un cambio.
     * Si el ítem ya no existe (por qty=0), re-renderiza toda la lista.
     */
    private void rebindRowAfterChange(View row, Product p) {
        if (row == null || p == null) return;

        CartItem current = findCartItemByProductName(p.name);
        if (current == null) {
            // El ítem fue removido (qty llegó a 0): re-dibujamos la lista completa
            renderCart();
            return;
        }

        TextView tvQty = row.findViewById(R.id.tvQty);
        TextView tvItemTotal = row.findViewById(R.id.tvItemTotal);

        if (tvQty != null) tvQty.setText(String.valueOf(current.qty));
        if (tvItemTotal != null) {
            tvItemTotal.setText(
                    getString(R.string.item_total_format, ars.format(current.qty * p.price))
            );
        }
    }

    /**
     * Recalcula y muestra el total global.
     * También habilita/deshabilita el botón “Realizar todos”.
     */
    private void updateGrandTotal() {
        int total = CartStore.get().getTotalAmount();
        if (tvCartGrandTotal != null) {
            tvCartGrandTotal.setText(
                    getString(R.string.cart_total_label_format, ars.format(total))
            );
        }

        MaterialButton btnPlaceAll = findViewById(R.id.btnPlaceAllOrders);
        if (btnPlaceAll != null) {
            boolean enabled = CartStore.get().getTotalQty() > 0;
            btnPlaceAll.setEnabled(enabled);
            btnPlaceAll.setAlpha(enabled ? 1f : 0.5f);
        }
    }

    /**
     * Acción “Realizar todos los pedidos”:
     * Muestra confirmación, y si el usuario acepta, muestra mensaje final
     * y limpia el carrito (simulado).
     */
    private void onPlaceAllOrders() {
        int items = CartStore.get().getTotalQty();
        int total = CartStore.get().getTotalAmount();
        if (items <= 0) return;

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.confirm_all_orders_title))
                .setMessage(getString(R.string.confirm_all_orders_msg, items, ars.format(total)))
                .setPositiveButton(getString(R.string.ok), (d, w) -> {
                    new AlertDialog.Builder(this)
                            .setMessage(getString(R.string.all_orders_done))
                            .setPositiveButton(getString(R.string.ok), (d2, w2) -> {
                                CartStore.get().clear();
                                renderCart();
                            })
                            .show();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    // ============================================================
    // Helpers privados
    // ============================================================

    /**
     * Busca un CartItem actual por el nombre del producto.
     * (En producción, preferir una clave ID inmutable en vez del nombre).
     */
    private CartItem findCartItemByProductName(String name) {
        if (name == null) return null;
        for (CartItem x : CartStore.get().getItems()) {
            if (x.product != null && name.equals(x.product.name)) {
                return x;
            }
        }
        return null;
    }
}
