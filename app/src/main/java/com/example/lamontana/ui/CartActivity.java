package com.example.lamontana.ui;

import android.app.AlertDialog;
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
 *     y realizar todos los pedidos (simulado).
 *   - Verifica que el usuario esté logueado con Firebase Auth antes
 *     de mostrar el carrito.
 *   - Usa MenuDesplegableHelper para manejar el menú deslizante
 *     (top sheet) del navbar con opciones:
 *       · Inicio (Catálogo)
 *       · Mis datos
 *       · Mi carrito
 *       · Cerrar sesión
 *   - Carga las imágenes de los productos desde la URL remota
 *     (Product.imageUrl, proveniente de Firestore/Storage) usando Glide,
 *     con fallback al recurso drawable local (imageRes).
 *   - Delega la lógica de estado del carrito en CartViewModel, que
 *     a su vez envuelve CartStore y expone LiveData:
 *       · Lista de CartItem
 *       · Total en pesos
 *       · Cantidad total de ítems
 *
 * Clases usadas:
 *   - CartViewModel: capa de lógica de negocio del carrito.
 *   - CartItem/Product (model): datos de dominio.
 *   - FirebaseAuth/FirebaseUser: autenticación de usuario.
 *   - Glide: carga de imágenes desde URL.
 *   - MenuDesplegableHelper: lógica común del menú top-sheet.
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
            // Volver SIEMPRE a la pantalla de Catálogo, no solo "atrás"
            btnBack.setOnClickListener(v -> {
                Intent intent = new Intent(CartActivity.this, CatalogActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            });
        }

        MaterialButton btnPlaceAll = findViewById(R.id.btnPlaceAllOrders);
        if (btnPlaceAll != null) {
            btnPlaceAll.setOnClickListener(v -> onPlaceAllOrders());
        }

        // ---------- Inicializar ViewModel y observar LiveData ----------
        cartViewModel = new ViewModelProvider(this).get(CartViewModel.class);

        // Observamos SOLO la lista de ítems.
        // Cuando cambie:
        //   1) Re-renderizamos el carrito completo.
        //   2) Actualizamos el total y el estado del botón usando los totales del ViewModel.
        cartViewModel.getItems().observe(this, this::renderCart);
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
     * Además, actualiza el total global usando los valores del ViewModel.
     */
    private void renderCart(List<CartItem> items) {
        if (llCartListContainer == null) return;

        llCartListContainer.removeAllViews();

        if (items != null && !items.isEmpty()) {
            LayoutInflater inflater = LayoutInflater.from(this);
            for (CartItem ci : items) {
                View row = createCartRow(inflater, ci);
                if (row != null) {
                    llCartListContainer.addView(row);
                }
            }
        }

        // Leer totales actuales desde el ViewModel y actualizar la UI
        Integer totalAmount = cartViewModel.getTotalAmount().getValue();
        Integer totalQty = cartViewModel.getTotalQty().getValue();

        updateGrandTotal(
                totalAmount != null ? totalAmount : 0,
                totalQty != null ? totalQty : 0
        );
    }

    /**
     * Crea y configura una fila visual del carrito para un CartItem dado.
     */
    private View createCartRow(LayoutInflater inflater, CartItem ci) {
        if (ci == null || ci.product == null) return null;

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

        // ==== BLOQUE Glide: Carga de imagen desde imageUrl con fallback a imageRes ====
        if (iv != null) {
            int fallbackRes = (p.imageRes != 0)
                    ? p.imageRes
                    : R.drawable.sample_print_bw;

            if (!TextUtils.isEmpty(p.imageUrl)) {
                // Imagen remota desde Firebase Storage
                Glide.with(iv.getContext())
                        .load(p.imageUrl)
                        .placeholder(fallbackRes)
                        .error(fallbackRes)
                        .into(iv);
            } else if (p.imageRes != 0) {
                // Fallback: drawable local ya definido en el Product
                iv.setImageResource(p.imageRes);
            } else {
                // Último recurso: icono genérico
                iv.setImageResource(R.drawable.ic_launcher_foreground);
            }
        }
        // ============================================================================

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
                new AlertDialog.Builder(CartActivity.this)
                        .setTitle("Eliminar producto")
                        .setMessage("¿Deseás quitar \"" + p.name + "\" del carrito?")
                        .setPositiveButton("Eliminar", (dialog, which) -> {
                            cartViewModel.remove(p);
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            });
        }

        return row;
    }

    /**
     * Recalcula y muestra el total global (usando los valores que vienen
     * del ViewModel).
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

    /**
     * Acción “Realizar todos los pedidos”.
     */
    private void onPlaceAllOrders() {
        Integer items = cartViewModel.getTotalQty().getValue();
        Integer total = cartViewModel.getTotalAmount().getValue();

        int safeItems = items != null ? items : 0;
        int safeTotal = total != null ? total : 0;

        if (safeItems <= 0) return;

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.confirm_all_orders_title))
                .setMessage(
                        getString(
                                R.string.confirm_all_orders_msg,
                                safeItems,
                                ars.format(safeTotal)
                        )
                )
                .setPositiveButton(getString(R.string.ok), (d, w) -> {
                    new AlertDialog.Builder(this)
                            .setMessage(getString(R.string.all_orders_done))
                            .setPositiveButton(getString(R.string.ok), (d2, w2) -> {
                                // Vaciar carrito a través del ViewModel
                                cartViewModel.clear();
                            })
                            .show();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }
}
