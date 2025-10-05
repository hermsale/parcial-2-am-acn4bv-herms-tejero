package com.example.parcial_1;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * MainActivity - Catálogo + Carrito (resumen arriba)
 *
 * Qué hace:
 * - Carga un catálogo mock (sin backend) y lo muestra a ancho completo.
 * - Permite filtrar por categoría y agregar productos al carrito.
 * - Muestra el carrito en la parte superior con cantidad total de ítems y total en $ ARS.
 * - Botón "Ver carrito" para ir a CartActivity (pantalla de detalle).
 *
 * Ciclo de vida / hilo:
 * - Toda la inicialización se realiza en onCreate() (hilo UI).
 */
public class MainActivity extends AppCompatActivity {

    // Vistas (catálogo y resumen de carrito)
    private LinearLayout llCatalogContainer;
    private TextView tvTotal;
    private TextView tvCartCount;

    // Filtros / acciones
    private MaterialButton btnAll, btnPrint, btnBinding;
    private MaterialButton btnClearCart, btnViewCart;

    // Datos en memoria (MVP sin backend)
    private final List<Product> allProducts = new ArrayList<>();
    private final List<CartItem> cart = new ArrayList<>();

    private LayoutInflater inflater;
    private final NumberFormat ars = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Toolbar con logo/títulos (reutilizable)
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

        // Referencias a vistas
        inflater = LayoutInflater.from(this);
        llCatalogContainer = findViewById(R.id.llCatalogContainer);
        tvTotal = findViewById(R.id.tvTotal);
        tvCartCount = findViewById(R.id.tvCartCount);

        btnAll = findViewById(R.id.btnFilterAll);
        btnPrint = findViewById(R.id.btnFilterPrint);
        btnBinding = findViewById(R.id.btnFilterBinding);
        btnClearCart = findViewById(R.id.btnClearCart);
        btnViewCart = findViewById(R.id.btnViewCart);

        // 1) Datos mock realistas
        seedMockData();

        // 2) Render catálogo inicial (Todos)
        renderCatalog(allProducts);

        // 3) Listeners de filtros
        btnAll.setOnClickListener(v -> renderCatalog(allProducts));

        btnPrint.setOnClickListener(v -> {
            List<Product> filtered = new ArrayList<>();
            for (Product p : allProducts) if (p.category == Category.PRINT) filtered.add(p);
            renderCatalog(filtered);
        });

        btnBinding.setOnClickListener(v -> {
            List<Product> filtered = new ArrayList<>();
            for (Product p : allProducts) if (p.category == Category.BINDING) filtered.add(p);
            renderCatalog(filtered);
        });

        // 4) Acciones de carrito
        btnClearCart.setOnClickListener(v -> {
            cart.clear();
            updateCartUi();
        });

        btnViewCart.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, CartActivity.class))
        );

        // Estado inicial
        updateCartUi();
    }

    /** Carga productos de ejemplo (sin backend). */
    private void seedMockData() {
        allProducts.add(new Product(
                "Impresión B/N",
                "Cara simple · A4",
                100,
                Category.PRINT,
                R.drawable.sample_print_bw
        ));
        allProducts.add(new Product(
                "Impresión color",
                "Doble cara · A4",
                200,
                Category.PRINT,
                R.drawable.sample_print_color
        ));
        allProducts.add(new Product(
                "Anillado A4",
                "Tapa plástica + contratapa",
                800,
                Category.BINDING,
                R.drawable.sample_binding
        ));
    }

    /**
     * Renderiza el catálogo a partir de una lista.
     * Limpia el contenedor y crea un item por producto inflando R.layout.item_catalog.
     */
    private void renderCatalog(List<Product> list) {
        llCatalogContainer.removeAllViews();
        for (Product p : list) {
            View item = inflater.inflate(R.layout.item_catalog, llCatalogContainer, false);

            ImageView iv = item.findViewById(R.id.ivThumb);
            TextView tvName = item.findViewById(R.id.tvName);
            TextView tvDesc = item.findViewById(R.id.tvDesc);
            TextView tvPrice = item.findViewById(R.id.tvPrice);
            MaterialButton btnAdd = item.findViewById(R.id.btnAdd);

            iv.setImageResource(p.imageRes);
            tvName.setText(p.name);
            tvDesc.setText(p.desc);
            tvPrice.setText(ars.format(p.price)); // precio $ ARS formateado

            // Agregar al carrito
            btnAdd.setOnClickListener(v -> addToCart(p));

            llCatalogContainer.addView(item);
        }
    }

    /** Agrega un producto al carrito; si ya está, incrementa cantidad. */
    private void addToCart(Product p) {
        CartItem existing = null;
        for (CartItem ci : cart) {
            if (ci.product.name.equals(p.name)) { // clave simple por nombre (suficiente para MVP)
                existing = ci;
                break;
            }
        }
        if (existing == null) {
            cart.add(new CartItem(p, 1));
        } else {
            existing.qty++;
        }
        updateCartUi();
    }

    /** Recalcula cantidad y total del carrito y los pinta en el resumen superior. */
    private void updateCartUi() {
        int items = 0;
        int total = 0;
        for (CartItem ci : cart) {
            items += ci.qty;
            total += ci.qty * ci.product.price;
        }
        tvCartCount.setText(getString(R.string.cart_items_format, items)); // "(N ítems)"
        tvTotal.setText(ars.format(total)); // "$ X.XXX,XX"
    }

    // ----------------- Modelos en memoria (MVP) -----------------

    enum Category { PRINT, BINDING }

    static class Product {
        final String name;
        final String desc;
        final int price;
        final Category category;
        final int imageRes;
        Product(String name, String desc, int price, Category category, int imageRes) {
            this.name = name;
            this.desc = desc;
            this.price = price;
            this.category = category;
            this.imageRes = imageRes;
        }
    }

    static class CartItem {
        final Product product;
        int qty;
        CartItem(Product product, int qty) {
            this.product = product;
            this.qty = qty;
        }
    }
}
