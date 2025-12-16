package com.example.lamontana.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.lamontana.R;
import com.example.lamontana.model.CartItem;
import com.example.lamontana.model.Product;
import com.example.lamontana.ui.navbar.MenuDesplegableHelper;
import com.example.lamontana.viewmodel.CartViewModel;
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
 *     y navegar al flujo de Checkout.
 *   - Verifica que el usuario esté logueado con Firebase Auth antes
 *     de mostrar el carrito.
 *   - Usa MenuDesplegableHelper para manejar el menú deslizante
 *     (top sheet) del navbar con opciones:
 *       · Inicio (Catálogo)
 *       · Mis datos
 *       · Mi carrito
 *       · Cerrar sesión
 *   - Usa CartViewModel como capa de estado del carrito, que envuelve
 *     CartStore y expone LiveData:
 *       · Lista de CartItem
 *       · Total en pesos
 *       · Cantidad total de ítems
 *   - Carga las imágenes de los productos desde la URL remota
 *     (Product.imageUrl, proveniente de Firestore/Storage) usando Glide,
 *     con fallback al recurso drawable local (imageRes).
 *   - El botón “Realizar todos los pedidos” navega a CheckoutActivity.
 * ============================================================
 */
public class CartActivity extends AppCompatActivity {

    // Contenedor vertical donde se agregan dinámicamente las filas del carrito
    private LinearLayout llCartListContainer;

    // Texto de total general ($)
    private TextView tvCartGrandTotal;

    // Helper para el menú deslizante del navbar
    private MenuDesplegableHelper menuHelper;

    // ViewModel del carrito
    private CartViewModel cartViewModel;

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

        View btnInicio = findViewById(R.id.btnInicio);
        View btnMisDatos = findViewById(R.id.btnMisDatos);
        View btnMiCarrito = findViewById(R.id.btnMiCarrito);
        View btnCerrarSesion = findViewById(R.id.btnCerrarSesion);

        View btnImpresionesCopias = findViewById(R.id.btnImpresionesCopias);

        menuHelper = new MenuDesplegableHelper(
                this,
                btnMenu,
                overlay,
                topSheet,
                btnInicio,
                btnMisDatos,
                btnImpresionesCopias,
                btnMiCarrito,
                btnCerrarSesion
        );
        menuHelper.initMenu();

        // ---- Referencias de UI del carrito ----
        llCartListContainer = findViewById(R.id.llCartListContainer);
        tvCartGrandTotal = findViewById(R.id.tvCartGrandTotal);

        // Botón “Volver al catálogo” → SIEMPRE ir al catálogo
        MaterialButton btnBack = findViewById(R.id.btnBackToCatalog);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                Intent intent = new Intent(CartActivity.this, CatalogActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            });
        }

        // Botón “Realizar todos los pedidos” → ir a CheckoutActivity
        MaterialButton btnPlaceAll = findViewById(R.id.btnPlaceAllOrders);
        if (btnPlaceAll != null) {
            btnPlaceAll.setOnClickListener(v -> {
                Integer qty = cartViewModel.getTotalQty().getValue();
                if (qty == null || qty <= 0) {
                    // Si por cualquier motivo se toca con carrito vacío, no hacemos nada.
                    return;
                }
                Intent intent = new Intent(CartActivity.this, CheckoutActivity.class);
                startActivity(intent);
            });
        }

        // ---------- Inicializar ViewModel y observar LiveData ----------
        cartViewModel = new ViewModelProvider(this).get(CartViewModel.class);

        // Observamos SOLO la lista; cada cambio:
        //  - re-renderiza el carrito
        //  - actualiza totales usando los LiveData de total/cantidad
        cartViewModel.getItems().observe(this, items -> {
            renderCart(items);
            Integer totalAmount = cartViewModel.getTotalAmount().getValue();
            Integer totalQty = cartViewModel.getTotalQty().getValue();
            updateGrandTotal(
                    totalAmount != null ? totalAmount : 0,
                    totalQty != null ? totalQty : 0
            );
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (cartViewModel != null) {
            cartViewModel.refresh();
        }
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
     * Dibuja toda la lista del carrito a partir de la lista observada.
     * Carga las imágenes desde URL usando Glide.
     */
    private void renderCart(List<CartItem> items) {
        if (llCartListContainer == null) return;

        llCartListContainer.removeAllViews();
        if (items == null || items.isEmpty()) {
            // Podrías mostrar un mensaje de "Carrito vacío" aquí si querés.
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);

        for (CartItem ci : items) {
            View row = inflater.inflate(R.layout.item_cart_detail, llCartListContainer, false);
            bindRow(row, ci);
            llCartListContainer.addView(row);
        }
    }

    /**
     * Renderiza una fila individual del carrito.
     */
    private void bindRow(View row, CartItem ci) {
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
        if (p == null) return;

        // ==== CARGA DE IMAGEN: URL remota (imageUrl) + fallback a drawable local ====
        if (iv != null) {
            int fallbackRes = (p.imageRes != 0)
                    ? p.imageRes
                    : R.drawable.sample_print_bw;

            if (!TextUtils.isEmpty(p.imageUrl)) {
                Glide.with(iv.getContext())
                        .load(p.imageUrl)
                        .placeholder(fallbackRes)
                        .error(fallbackRes)
                        .into(iv);
            } else if (p.imageRes != 0) {
                iv.setImageResource(p.imageRes);
            } else {
                iv.setImageResource(R.drawable.ic_launcher_foreground);
            }
        }
        // ===================================================================

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

        // ----- Listeners para +, -, eliminar delegando en el ViewModel -----

        if (btnMinus != null) {
            btnMinus.setOnClickListener(v -> cartViewModel.dec(p));
        }

        if (btnPlus != null) {
            btnPlus.setOnClickListener(v -> cartViewModel.inc(p));
        }

        if (btnRemoveItem != null) {
            btnRemoveItem.setOnClickListener(v -> {
                new android.app.AlertDialog.Builder(this)
                        .setTitle("Eliminar producto")
                        .setMessage("¿Deseás quitar \"" + p.name + "\" del carrito?")
                        .setPositiveButton("Eliminar", (dialog, which) -> {
                            cartViewModel.remove(p);
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            });
        }
    }

    /**
     * Actualiza el total global y el estado del botón "Realizar todos".
     */
    private void updateGrandTotal(int totalAmount, int totalQty) {
        if (tvCartGrandTotal != null) {
            tvCartGrandTotal.setText(
                    getString(R.string.cart_total_label_format, ars.format(totalAmount))
            );
        }

        MaterialButton btnPlaceAll = findViewById(R.id.btnPlaceAllOrders);
        if (btnPlaceAll != null) {
            boolean enabled = totalQty > 0;
            btnPlaceAll.setEnabled(enabled);
            btnPlaceAll.setAlpha(enabled ? 1f : 0.5f);
        }
    }
}
