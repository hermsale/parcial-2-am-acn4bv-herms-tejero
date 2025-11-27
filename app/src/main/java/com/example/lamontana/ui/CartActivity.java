package com.example.lamontana.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.lamontana.R;
import com.example.lamontana.data.CartStore;
import com.example.lamontana.model.CartItem;
import com.example.lamontana.model.Product;
import com.example.lamontana.ui.navbar.MenuDesplegableHelper;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/*
 * ============================================================
 * Archivo: CartActivity.java
 * Paquete: com.example.lamontana.ui
 * ------------------------------------------------------------
 * ¿De qué se encarga?
 *   - Controla la pantalla "Carrito". Muestra los ítems agregados,
 *     permite modificar cantidades (+/-), eliminar productos del carrito
 *     y realizar todos los pedidos (simulado).
 *   - Verifica que el usuario esté logueado con Firebase Auth antes
 *     de mostrar el carrito.
 *   - Usa MenuDesplegableHelper para manejar el menú deslizante
 *     (top sheet) del navbar con opciones:
 *       · Mis datos
 *       · Mi carrito
 *       · Cerrar sesión
 *   - Carga las imágenes de los productos desde la URL remota
 *     (Product.imageUrl, proveniente de Firestore/Storage) usando Glide,
 *     con fallback al recurso drawable local (imageRes).
 *
 * Clases usadas:
 *   - CartStore (data): estado del carrito en memoria.
 *   - CartItem/Product (model): datos de dominio.
 *   - FirebaseAuth/FirebaseUser: autenticación de usuario.
 *   - Glide: carga de imágenes desde URL.
 *   - MenuDesplegableHelper: lógica común del menú top-sheet.
 *
 * Métodos presentes:
 *   - onCreate():
 *       * Verifica usuario logueado (ensureUserLoggedIn()).
 *       * Configura UI, menú del navbar (via helper) y dibuja el carrito.
 *   - ensureUserLoggedIn():
 *       * Si no hay usuario -> LoginActivity y finish().
 *   - renderCart():
 *       * Infla filas (item_cart_detail) por cada CartItem y renderiza datos.
 *   - rebindRowAfterChange(View, Product):
 *       * Refresca cantidad/total de una fila tras un cambio (+/-).
 *   - updateGrandTotal():
 *       * Recalcula total global y habilita/deshabilita “Realizar todos”.
 *   - onPlaceAllOrders():
 *       * Confirma y limpia carrito (simulado).
 *   - findCartItemByProductName(String):
 *       * Busca el CartItem asociado a un Product por nombre.
 * ============================================================
 */
public class CartActivity extends AppCompatActivity {

    // Contenedor vertical donde se agregan dinámicamente las filas del carrito
    private LinearLayout llCartListContainer;

    // Texto de total general ($)
    private TextView tvCartGrandTotal;

    // Helper para el menú deslizante del navbar
    private MenuDesplegableHelper menuHelper;

    // Formateador de moneda en ARS
    private final NumberFormat ars =
            NumberFormat.getCurrencyInstance(new Locale("es", "AR"));

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1) Verificar si el usuario está logueado en Firebase Auth.
        if (!ensureUserLoggedIn()) {
            return;
        }

        setContentView(R.layout.activity_cart);

        // ---------- Navbar / Menú deslizante mediante helper ----------
        ImageView btnMenu = findViewById(R.id.btnMenu);
        View overlay = findViewById(R.id.overlay);
        View topSheet = findViewById(R.id.topSheet);

        View btnMisDatos = findViewById(R.id.btnMisDatos);
        View btnMiCarrito = findViewById(R.id.btnMiCarrito);
        View btnCerrarSesion = findViewById(R.id.btnCerrarSesion);

        // En carrito tampoco hay botón "Inicio" propio → pasamos null
        View btnInicio = null;

        menuHelper = new MenuDesplegableHelper(
                this,
                btnMenu,
                overlay,
                topSheet,
                btnInicio,
                btnMisDatos,
                btnMiCarrito,
                btnCerrarSesion
        );
        menuHelper.initMenu();

        // ---- Referencias de UI del carrito ----
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
     * Dibuja toda la lista del carrito.
     * Carga las imágenes desde URL usando Glide.
     */
    private void renderCart() {
        if (llCartListContainer == null) return;

        llCartListContainer.removeAllViews();

        List<CartItem> items = CartStore.get().getItems();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (CartItem ci : items) {
            View row = inflater.inflate(R.layout.item_cart_detail, llCartListContainer, false);

            ImageView iv = row.findViewById(R.id.ivThumb);
            TextView tvName = row.findViewById(R.id.tvName);
            TextView tvDesc = row.findViewById(R.id.tvDesc);
            TextView tvUnitPrice = row.findViewById(R.id.tvUnitPrice);
            TextView tvItemTotal = row.findViewById(R.id.tvItemTotal);
            TextView tvQty = row.findViewById(R.id.tvQty);
            MaterialButton btnMinus = row.findViewById(R.id.btnMinus);
            MaterialButton btnPlus = row.findViewById(R.id.btnPlus);
            MaterialButton btnRemoveItem = row.findViewById(R.id.btnRemoveItem);

            Product p = ci.product;
            if (p == null) continue;

            // ==== Carga de imagen desde imageUrl con fallback a imageRes ====
            if (iv != null) {
                if (p.imageUrl != null && !p.imageUrl.trim().isEmpty()) {
                    // Usamos Glide para cargar la URL remota (Firebase Storage)
                    Glide.with(this)
                            .load(p.imageUrl)
                            .into(iv);
                } else if (p.imageRes != 0) {
                    // Fallback al recurso drawable local existente
                    iv.setImageResource(p.imageRes);
                } else {
                    // Último fallback: un icono genérico del proyecto
                    iv.setImageResource(R.drawable.ic_launcher_foreground);
                }
            }
            // ===============================================================

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

            if (btnMinus != null) {
                btnMinus.setOnClickListener(v -> {
                    CartStore.get().dec(p);
                    rebindRowAfterChange(row, p);
                    updateGrandTotal();
                });
            }

            if (btnPlus != null) {
                btnPlus.setOnClickListener(v -> {
                    CartStore.get().inc(p);
                    rebindRowAfterChange(row, p);
                    updateGrandTotal();
                });
            }

            if (btnRemoveItem != null) {
                btnRemoveItem.setOnClickListener(v -> {
                    new AlertDialog.Builder(this)
                            .setTitle("Eliminar producto")
                            .setMessage("¿Deseás quitar \"" + p.name + "\" del carrito?")
                            .setPositiveButton("Eliminar", (dialog, which) -> {
                                CartStore.get().remove(p);
                                renderCart();
                            })
                            .setNegativeButton("Cancelar", null)
                            .show();
                });
            }

            llCartListContainer.addView(row);
        }

        updateGrandTotal();
    }

    /**
     * Refresca la cantidad y el total de una fila específica después de un cambio.
     */
    private void rebindRowAfterChange(View row, Product p) {
        if (row == null || p == null) return;

        CartItem current = findCartItemByProductName(p.name);
        if (current == null) {
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
     * Acción “Realizar todos los pedidos”.
     */
    private void onPlaceAllOrders() {
        int items = CartStore.get().getTotalQty();
        int total = CartStore.get().getTotalAmount();
        if (items <= 0) return;

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.confirm_all_orders_title))
                .setMessage(
                        getString(
                                R.string.confirm_all_orders_msg,
                                items,
                                ars.format(total)
                        )
                )
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

    /**
     * Busca un CartItem actual por el nombre del producto.
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
