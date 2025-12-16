package com.example.lamontana.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.lamontana.R;
import com.example.lamontana.data.CartStore;
import com.example.lamontana.model.Category;
import com.example.lamontana.model.Product;
import com.example.lamontana.ui.navbar.MenuDesplegableHelper;
import com.example.lamontana.viewmodel.CatalogViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/*
 * ============================================================
 * Archivo: CatalogActivity.java
 * Paquete: com.example.lamontana.ui
 *
 * ¿De qué se encarga este archivo?
 *  - Implementa la pantalla principal de Catálogo de la app
 *    "La Montaña".
 *  - Muestra productos/servicios traídos desde Firestore,
 *    permite filtrarlos y agregarlos al carrito.
 *  - Presenta, en el panel superior, un resumen rápido del
 *    carrito (cantidad y total).
 *  - Verifica que el usuario esté logueado con Firebase Auth
 *    antes de mostrar el catálogo.
 *  - Controla el menú deslizante del navbar (top sheet) a
 *    través del helper reutilizable:
 *      · MenuDesplegableHelper (en ui.navbar)
 *    con opciones:
 *      · Mis datos
 *      · Mi carrito
 *      · Cerrar sesión
 *
 * Relación con otras clases:
 *  - CatalogViewModel:
 *      * Carga los productos desde Firestore (colección
 *        "productos") y expone List<Product> por LiveData.
 *  - Product:
 *      * Incluye tanto imageRes (drawable local) como imageUrl
 *        (URL remota de Firebase Storage).
 *  - CartStore / CartActivity:
 *      * Reciben los Product seleccionados y gestionan el
 *        estado del carrito.
 *  - MenuDesplegableHelper:
 *      * Encapsula la lógica del menú top-sheet para reducir
 *        código duplicado en las Activities.
 *
 * Métodos presentes:
 *  - onCreate(Bundle):
 *      * Verifica login (ensureUserLoggedIn()).
 *      * Infla el layout, inicializa vistas y listeners.
 *      * Configura el menú deslizante del navbar mediante
 *        MenuDesplegableHelper.
 *      * Conecta el CatalogViewModel y observa los productos.
 *  - ensureUserLoggedIn():
 *      * Consulta FirebaseAuth para ver si hay usuario
 *        autenticado.
 *  - renderCatalog(List<Product>):
 *      * Dibuja la lista de productos en item_catalog.xml,
 *        usando Glide con imageUrl si está disponible, o
 *        imageRes como fallback.
 *  - filterAndRender(Category):
 *      * Aplica un filtro por categoría sobre la lista
 *        completa cargada desde el ViewModel.
 *  - updateCartUi():
 *      * Actualiza el panel superior del carrito (cantidad y
 *        total).
 *  - onResume():
 *      * Refresca el estado del carrito al volver a esta
 *        pantalla.
 * ============================================================
 */

public class CatalogActivity extends AppCompatActivity {

    // ---------- Referencias de UI ----------
    private LinearLayout llCatalogContainer;
    private TextView tvTotal;
    private TextView tvCartCount;

    private MaterialButton btnAll, btnPrint, btnBinding;
    private MaterialButton btnClearCart, btnViewCart;

    // Helper para el menú desplegable del navbar
    private MenuDesplegableHelper menuHelper;

    // ---------- Soporte ----------
    /** Lista completa de productos cargados desde Firestore vía ViewModel. */
    private final List<Product> fullProductList = new ArrayList<>();

    private LayoutInflater inflater;
    private final NumberFormat ars =
            NumberFormat.getCurrencyInstance(new Locale("es", "AR"));

    /** ViewModel responsable de cargar los productos desde Firestore. */
    private CatalogViewModel catalogViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1) Verificar si el usuario está logueado en Firebase Auth.
        //    Si no lo está, redirigimos a Login y no continuamos.
        if (!ensureUserLoggedIn()) {
            return;
        }

        setContentView(R.layout.activity_catalog);

        // ---------- Navbar / Menú deslizante con helper ----------
        ImageView btnMenu = findViewById(R.id.btnMenu);
        View overlay = findViewById(R.id.overlay);
        View topSheet = findViewById(R.id.topSheet);

        View btnMisDatos = findViewById(R.id.btnMisDatos);
        View btnMiCarrito = findViewById(R.id.btnMiCarrito);
        View btnCerrarSesion = findViewById(R.id.btnCerrarSesion);

        // En catálogo NO hay botón "Inicio" porque ya es la pantalla principal → pasamos null
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

        // ---------- Bind de vistas del catálogo ----------
        inflater = LayoutInflater.from(this);
        llCatalogContainer = findViewById(R.id.llCatalogContainer);
        tvTotal = findViewById(R.id.tvTotal);
        tvCartCount = findViewById(R.id.tvCartCount);

        btnAll = findViewById(R.id.btnFilterAll);
        btnPrint = findViewById(R.id.btnFilterPrint);
//        boton de servicios
        btnBinding = findViewById(R.id.btnFilterBinding);

        btnClearCart = findViewById(R.id.btnClearCart);
        btnViewCart = findViewById(R.id.btnViewCart);

        // ---------- Inicializar ViewModel y observar datos ----------
        catalogViewModel = new ViewModelProvider(this).get(CatalogViewModel.class);

        // Observamos la lista de productos: cuando cambie, actualizamos la lista local y renderizamos.
        catalogViewModel.getProducts().observe(this, products -> {
            fullProductList.clear();
            if (products != null) {
                fullProductList.addAll(products);
            }
            // Renderizamos la lista completa por defecto
            renderCatalog(fullProductList);
        });

        // Observamos errores para mostrar un mensaje simple al usuario.
        catalogViewModel.getErrorMessage().observe(this, msg -> {
            if (msg != null && !msg.trim().isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            }
        });

        // Disparar la carga de productos sólo si es necesario
        catalogViewModel.loadProductsIfNeeded();

        // ---------- Listeners de filtros y acciones de carrito ----------
        if (btnAll != null) {
            btnAll.setOnClickListener(v -> renderCatalog(fullProductList));
        }
        if (btnPrint != null) {
            btnPrint.setOnClickListener(v -> filterAndRender(Category.PRINT));
        }

//        boton ir a servicios
        if (btnBinding != null) {
            btnBinding.setOnClickListener(v ->
                startActivity(new Intent(CatalogActivity.this, ServiciosActivity.class))
            );
        }

        if (btnClearCart != null) {
            btnClearCart.setOnClickListener(v -> {
                CartStore.get().clear();
                updateCartUi();
            });
        }


//       boton ir al carrito
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
     * Renderiza la lista de productos (catálogo) en el contenedor vertical.
     * Usa Glide para cargar imageUrl de Firebase Storage si está disponible;
     * en caso contrario, recurre al drawable local imageRes.
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

            if (tvName != null) tvName.setText(p.name);
            if (tvDesc != null) tvDesc.setText(p.desc);
            if (tvPrice != null) tvPrice.setText(ars.format(p.price));

            if (iv != null) {
                if (p.imageUrl != null) {
                    // Imagen remota desde Firebase Storage
                    Glide.with(iv.getContext())
                            .load(p.imageUrl)
                            .placeholder(p.imageRes != 0 ? p.imageRes : R.drawable.sample_print_bw)
                            .error(p.imageRes != 0 ? p.imageRes : R.drawable.sample_print_bw)
                            .into(iv);
                } else {
                    // Fallback: drawable local
                    iv.setImageResource(p.imageRes);
                }
            }

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
        for (Product p : fullProductList) {
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


    @Override
    protected void onResume() {
        super.onResume();
        updateCartUi();
    }
}

