package com.example.parcial_1;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.parcial_1.data.CartStore;
import com.example.parcial_1.model.Category;
import com.example.parcial_1.model.Product;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/*
 * ============================================================
 * Archivo: MainActivity.java
 * Paquete: com.example.parcial_1
 *
 * ¿De qué se encarga este archivo?
 *  - Implementa la pantalla principal de Catálogo de la app "La Montaña".
 *  - Muestra productos/servicios, permite filtrarlos y agregarlos al carrito.
 *  - Presenta, en el panel superior, un resumen rápido del carrito (cantidad y total).
 *
 * Clases declaradas:
 *  - MainActivity: Activity concreta (AppCompatActivity) que actúa como pantalla de Catálogo.
 *
 * Métodos y responsabilidades:
 *  - onCreate(Bundle): ciclo de vida. Infla el layout, configura la Toolbar, inicializa vistas,
 *    carga datos de ejemplo (seedMockData) y renderiza el catálogo completo.
 *  - seedMockData(): crea un conjunto mínimo de productos de ejemplo para la demo sin backend.
 *  - renderCatalog(List<Product>): dibuja la lista de productos en el contenedor del catálogo e
 *    instala los listeners del botón "Agregar" para enviar items al carrito.
 *  - updateCartUi(): actualiza el panel superior del carrito (cantidad y total) leyendo CartStore.
 *  - filterAndRender(Category): aplica un filtro por categoría y vuelve a renderizar.
 *
 * Relación con las vistas:
 *  - Vista Catálogo (activity_catalog.xml): esta Activity es su controlador. Maneja filtros y altas
 *    al carrito. Desde aquí se navega a la Vista Carrito (CartActivity) con "Ver carrito".
 *  - Vista Carrito: NO se controla desde esta clase, pero se actualiza el resumen superior cuando
 *    el usuario agrega/vacía productos en esta pantalla.
 *
 * Notas de implementación:
 *  - No hay backend: el carrito vive en memoria vía CartStore (singleton de demo).
 *  - Se evita duplicación de código creando el helper filterAndRender(Category).
 *  - Todas las operaciones de UI ocurren en el hilo principal (propio de Activities).
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
        setContentView(R.layout.activity_catalog);

        // Toolbar de la pantalla (logo y títulos)
        Toolbar toolbar = findViewById(R.id.appToolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayShowHomeEnabled(true);
                getSupportActionBar().setLogo(R.drawable.logo_lamontana);
                getSupportActionBar().setDisplayUseLogoEnabled(true);
                getSupportActionBar().setTitle("La Montaña");
                getSupportActionBar().setSubtitle("Impresiones");
            }
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
        btnAll.setOnClickListener(v -> renderCatalog(allProducts));
        btnPrint.setOnClickListener(v -> filterAndRender(Category.PRINT));
        btnBinding.setOnClickListener(v -> filterAndRender(Category.BINDING));

        btnClearCart.setOnClickListener(v -> {
            CartStore.get().clear();
            updateCartUi();
        });

        btnViewCart.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, CartActivity.class))
        );

        // Estado inicial del resumen de carrito
        updateCartUi();
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
        llCatalogContainer.removeAllViews();

        for (Product p : list) {
            final View item = inflater.inflate(R.layout.item_catalog, llCatalogContainer, false);

            ImageView iv = item.findViewById(R.id.ivThumb);
            TextView tvName = item.findViewById(R.id.tvName);
            TextView tvDesc = item.findViewById(R.id.tvDesc);
            TextView tvPrice = item.findViewById(R.id.tvPrice);
            MaterialButton btnAdd = item.findViewById(R.id.btnAdd);

            iv.setImageResource(p.imageRes);
            tvName.setText(p.name);
            tvDesc.setText(p.desc);
            tvPrice.setText(ars.format(p.price));

            btnAdd.setOnClickListener(v -> {
                CartStore.get().add(p);
                updateCartUi(); // refresca badge y total del carrito en el panel superior
            });

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
        tvCartCount.setText(getString(R.string.cart_items_format, items));
        tvTotal.setText(ars.format(total));
    }
}
