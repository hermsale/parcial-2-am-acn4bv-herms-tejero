package com.example.lamontana;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.lamontana.data.CartStore;
import com.example.lamontana.model.Category;
import com.example.lamontana.model.Product;
import com.example.lamontana.ui.LoginActivity;
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
 *  - A partir de ahora, verifica que el usuario esté logueado con Firebase Auth
 *    antes de mostrar el catálogo.
 *
 * Clases declaradas:
 *  - MainActivity: Activity concreta (AppCompatActivity) que actúa como pantalla
 *    de Catálogo, protegida para usuarios autenticados.
 *
 * Métodos y responsabilidades:
 *  - onCreate(Bundle):
 *      * Ciclo de vida.
 *      * Verifica que haya usuario logueado (ensureUserLoggedIn()).
 *      * Si no hay usuario → redirige a LoginActivity y termina.
 *      * Si hay usuario → infla el layout, configura la Toolbar, inicializa vistas,
 *        carga datos de ejemplo (seedMockData) y renderiza el catálogo completo.
 *  - ensureUserLoggedIn():
 *      * Consulta FirebaseAuth para ver si hay un usuario autenticado.
 *      * Si no lo hay, redirige a LoginActivity y devuelve false.
 *      * Si lo hay, devuelve true.
 *  - seedMockData():
 *      * Crea un conjunto mínimo de productos de ejemplo para la demo sin backend.
 *  - renderCatalog(List<Product>):
 *      * Dibuja la lista de productos en el contenedor del catálogo e instala
 *        los listeners del botón "Agregar".
 *  - updateCartUi():
 *      * Actualiza el panel superior del carrito (cantidad y total) leyendo CartStore.
 *  - filterAndRender(Category):
 *      * Aplica un filtro por categoría y vuelve a renderizar.
 *
 * Relación con las vistas:
 *  - Vista Catálogo (activity_catalog.xml): esta Activity es su controlador.
 *    Maneja filtros y altas al carrito. Desde aquí se navega a CartActivity.
 *
 *  - Se asegura que solo usuarios autenticados (FirebaseAuth) puedan ver el catálogo.
 * ============================================================
 */

public class MainActivity extends AppCompatActivity {

    // ---------- Referencias de UI ----------
    private LinearLayout llCatalogContainer;
    private TextView tvTotal;
    private TextView tvCartCount;

    private MaterialButton btnAll, btnPrint, btnBinding;
    private MaterialButton btnClearCart, btnViewCart;

    // ---------- Soporte ----------
    private final List<Product> allProducts = new ArrayList<>();
    private LayoutInflater inflater;
    private final NumberFormat ars = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1) Verificar si el usuario está logueado en Firebase Auth.
        //    Si no lo está, lo redirigimos al Login y no mostramos el catálogo.
        if (!ensureUserLoggedIn()) {
            // Si no hay usuario logueado, ya redirigimos y cerramos esta Activity.
            // No continuamos con la inicialización de la UI.
            return;
        }

        setContentView(R.layout.activity_catalog);

        ImageView btnMenu = findViewById(R.id.btnMenu);
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                // Por ahora este botón sigue llevando a la pantalla de Login.
                // Podríamos convertirlo en "Cerrar sesión" en una mejora futura.
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
            });
        }

        // Bind de vistas
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

        // ---------- Listeners ----------
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
                    startActivity(new Intent(MainActivity.this, CartActivity.class))
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
            // No hay usuario autenticado: enviar a la pantalla de Login.
            Intent intent = new Intent(this, LoginActivity.class);
            // Opcional: limpiar el back stack para evitar volver al catálogo sin login.
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return false;
        }
        // Hay usuario logueado, se puede continuar.
        return true;
    }

    /**
     * Crea productos de ejemplo para la demostración (no hay backend en esta etapa).
     * Se incluyen productos con y sin "copyBased" (detección de páginas de PDF).
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
     * Instala el click de "Agregar" para sumar al carrito y refrescar el panel superior.
     */
    private void renderCatalog(List<Product> list) {
        if (llCatalogContainer == null) return;

        llCatalogContainer.removeAllViews();

        for (Product p : list) {
            final View item = inflater.inflate(R.layout.item_catalog, llCatalogContainer, false);

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
                    updateCartUi(); // refresca badge y total del carrito en el panel superior
                });
            }

            llCatalogContainer.addView(item);
        }
    }

    /**
     * Aplica un filtro por categoría y vuelve a renderizar el catálogo.
     * Evita duplicación de código en listeners de filtros.
     */
    private void filterAndRender(Category category) {
        List<Product> filtered = new ArrayList<>();
        for (Product p : allProducts) {
            if (p.category == category) filtered.add(p);
        }
        renderCatalog(filtered);
    }

    /**
     * Actualiza el panel superior del carrito (cantidad y monto total) utilizando CartStore.
     * Se invoca luego de cualquier cambio que afecte el carrito (agregar/vaciar, etc.).
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
}
