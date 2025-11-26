package com.example.lamontana.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.lamontana.R;
import com.example.lamontana.data.CartStore;
import com.example.lamontana.model.Category;
import com.example.lamontana.model.Product;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/*
 * ============================================================
 * Archivo: MainActivity.java
 * Paquete: com.example.lamontana
 *
 * ¿De qué se encarga este archivo?
 *  - Implementa la pantalla principal de Catálogo de la app "La Montaña".
 *  - Muestra productos/servicios, permite filtrarlos y agregarlos al carrito.
 *  - Presenta, en el panel superior, un resumen rápido del carrito (cantidad y total).
 *  - Verifica que el usuario esté logueado con Firebase Auth
 *    antes de mostrar el catálogo.
 *  - Controla el menú deslizante del navbar (top sheet) con opciones:
 *      · Mis datos
 *      · Mi carrito
 *      · Cerrar sesión
 *
 * Clases declaradas:
 *  - MainActivity: Activity concreta (AppCompatActivity) que actúa como
 *    pantalla de Catálogo protegida para usuarios autenticados.
 *
 * Métodos presentes:
 *  - onCreate(Bundle):
 *      * Verifica login (ensureUserLoggedIn()).
 *      * Infla el layout, inicializa vistas y listeners.
 *      * Configura el menú deslizante del navbar.
 *      * Carga datos de ejemplo (seedMockData) y renderiza catálogo.
 *  - ensureUserLoggedIn():
 *      * Consulta FirebaseAuth para ver si hay usuario autenticado.
 *  - seedMockData():
 *      * Crea productos de ejemplo para la demo.
 *  - renderCatalog(List<Product>):
 *      * Dibuja la lista de productos y vincula el botón "Agregar".
 *  - filterAndRender(Category):
 *      * Aplica un filtro por categoría y vuelve a renderizar.
 *  - updateCartUi():
 *      * Actualiza el panel superior del carrito (cantidad y total).
 *  - toggleMenu(), openMenu(), closeMenu():
 *      * Controlan la apertura/cierre del menú deslizante del navbar.
 *  - onResume():
 *      * Refresca el estado del carrito al volver a esta pantalla.
 * ============================================================
 */

public class CatalogActivity extends AppCompatActivity {

    // ---------- Referencias de UI ----------
    private LinearLayout llCatalogContainer;
    private TextView tvTotal;
    private TextView tvCartCount;

    private MaterialButton btnAll, btnPrint, btnBinding;
    private MaterialButton btnClearCart, btnViewCart;

    // ----- Vistas para el menú deslizante (navbar) -----
    private View overlay;
    private View topSheet;
    private boolean isMenuOpen = false;

    // ---------- Soporte ----------
    private final List<Product> allProducts = new ArrayList<>();
    private LayoutInflater inflater;
    private final NumberFormat ars =
            NumberFormat.getCurrencyInstance(new Locale("es", "AR"));

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1) Verificar si el usuario está logueado en Firebase Auth.
        //    Si no lo está, redirigimos a Login y no continuamos.
        if (!ensureUserLoggedIn()) {
            return;
        }

        setContentView(R.layout.activity_catalog);

        // ---------- Navbar / Menú deslizante ----------
        ImageView btnMenu = findViewById(R.id.btnMenu);
        overlay = findViewById(R.id.overlay);
        topSheet = findViewById(R.id.topSheet);

        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> toggleMenu());
        }

        if (overlay != null) {
            overlay.setOnClickListener(v -> closeMenu());
        }

        // Botones dentro del top sheet (menú)
        View btnMisDatos = findViewById(R.id.btnMisDatos);
        View btnMiCarrito = findViewById(R.id.btnMiCarrito);
        View btnCerrarSesion = findViewById(R.id.btnCerrarSesion);

        if (btnMisDatos != null) {
            btnMisDatos.setOnClickListener(v -> {
                closeMenu();
                startActivity(new Intent(CatalogActivity.this, ProfileActivity.class));
            });
        }

        if (btnMiCarrito != null) {
            btnMiCarrito.setOnClickListener(v -> {
                closeMenu();
                startActivity(new Intent(CatalogActivity.this, CartActivity.class));
            });
        }

        if (btnCerrarSesion != null) {
            btnCerrarSesion.setOnClickListener(v -> {
                closeMenu();
                // Cerrar sesión en Firebase y volver al login
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(CatalogActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            });
        }

        // ---------- Bind de vistas del catálogo ----------
        inflater = LayoutInflater.from(this);
        llCatalogContainer = findViewById(R.id.llCatalogContainer);
        tvTotal = findViewById(R.id.tvTotal);
        tvCartCount = findViewById(R.id.tvCartCount);

        btnAll = findViewById(R.id.btnFilterAll);
        btnPrint = findViewById(R.id.btnFilterPrint);
        btnBinding = findViewById(R.id.btnFilterBinding);
        btnClearCart = findViewById(R.id.btnClearCart);
        btnViewCart = findViewById(R.id.btnViewCart);

        // Datos de ejemplo y primer render del catálogo
        seedMockData();
        renderCatalog(allProducts);

        // ---------- Listeners de filtros y acciones de carrito ----------
        if (btnAll != null) {
            btnAll.setOnClickListener(v -> renderCatalog(allProducts));
        }
        if (btnPrint != null) {
            btnPrint.setOnClickListener(v -> filterAndRender(Category.PRINT));
        }
        if (btnBinding != null) {
            btnBinding.setOnClickListener(v -> filterAndRender(Category.BINDING));
        }

        if (btnClearCart != null) {
            btnClearCart.setOnClickListener(v -> {
                CartStore.get().clear();
                updateCartUi();
            });
        }

        if (btnViewCart != null) {
            btnViewCart.setOnClickListener(v ->
                    startActivity(new Intent(CatalogActivity.this, CartActivity.class))
            );
        }

        // Estado inicial del resumen de carrito
        updateCartUi();
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
     * Crea productos de ejemplo para la demostración.
     */
    private void seedMockData() {
        allProducts.add(new Product(
                "Impresión B/N", "Cara simple · A4", 100,
                Category.PRINT, R.drawable.sample_print_bw, /*copyBased*/ true
        ));
        allProducts.add(new Product(
                "Impresión color", "Doble cara · A4", 200,
                Category.PRINT, R.drawable.sample_print_color, /*copyBased*/ true
        ));
        allProducts.add(new Product(
                "Anillado A4", "Tapa plástica + contratapa", 800,
                Category.BINDING, R.drawable.sample_binding, /*copyBased*/ false
        ));
    }

    /**
     * Renderiza la lista de productos (catálogo) en el contenedor vertical.
     */
    private void renderCatalog(List<Product> list) {
        if (llCatalogContainer == null) return;

        llCatalogContainer.removeAllViews();

        for (Product p : list) {
            final View item =
                    inflater.inflate(R.layout.item_catalog, llCatalogContainer, false);

            ImageView iv = item.findViewById(R.id.ivThumb);
            TextView tvName = item.findViewById(R.id.tvName);
            TextView tvDesc = item.findViewById(R.id.tvDesc);
            TextView tvPrice = item.findViewById(R.id.tvPrice);
            MaterialButton btnAdd = item.findViewById(R.id.btnAdd);

            if (iv != null) iv.setImageResource(p.imageRes);
            if (tvName != null) tvName.setText(p.name);
            if (tvDesc != null) tvDesc.setText(p.desc);
            if (tvPrice != null) tvPrice.setText(ars.format(p.price));

            if (btnAdd != null) {
                btnAdd.setOnClickListener(v -> {
                    CartStore.get().add(p);
                    updateCartUi();
                });
            }

            llCatalogContainer.addView(item);
        }
    }

    /**
     * Aplica un filtro por categoría y vuelve a renderizar el catálogo.
     */
    private void filterAndRender(Category category) {
        List<Product> filtered = new ArrayList<>();
        for (Product p : allProducts) {
            if (p.category == category) filtered.add(p);
        }
        renderCatalog(filtered);
    }

    /**
     * Actualiza el panel superior del carrito (cantidad y monto total).
     */
    private void updateCartUi() {
        int items = CartStore.get().getTotalQty();
        int total = CartStore.get().getTotalAmount();

        if (tvCartCount != null) {
            tvCartCount.setText(getString(R.string.cart_items_format, items));
        }
        if (tvTotal != null) {
            tvTotal.setText(ars.format(total));
        }
    }

    // ----- Control del menú deslizante (top sheet / navbar) -----

    private void toggleMenu() {
        if (isMenuOpen) {
            closeMenu();
        } else {
            openMenu();
        }
    }

    private void openMenu() {
        if (topSheet == null || overlay == null) return;

        topSheet.setVisibility(View.VISIBLE);
        topSheet.startAnimation(
                AnimationUtils.loadAnimation(this, R.anim.top_sheet_down)
        );
        overlay.setVisibility(View.VISIBLE);
        isMenuOpen = true;
    }

    private void closeMenu() {
        if (topSheet == null || overlay == null) return;

        topSheet.startAnimation(
                AnimationUtils.loadAnimation(this, R.anim.top_sheet_up)
        );
        overlay.setVisibility(View.GONE);
        topSheet.setVisibility(View.GONE);
        isMenuOpen = false;
    }

    // Esto soluciona que, al volver desde el carrito, se actualice el resumen
    @Override
    protected void onResume() {
        super.onResume();
        updateCartUi();
    }
}
