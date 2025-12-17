package com.example.lamontana.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.lamontana.R;
import com.example.lamontana.data.CartStore;
import com.example.lamontana.model.CartItem;
import com.example.lamontana.ui.navbar.MenuDesplegableHelper;
import com.example.lamontana.viewmodel.CheckoutViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/*
 * ============================================================
 * Archivo: CheckoutActivity.java
 * Paquete: com.example.lamontana.ui
 * ------------------------------------------------------------
 * Responsabilidad:
 *   - Pantalla de Checkout (paso siguiente al carrito).
 *   - Muestra:
 *       · Formulario de envío (dirección, CP, teléfono, notas).
 *       · Checkbox "usar mi dirección guardada".
 *       · Lista de productos que ya están en el carrito.
 *       · Total final del pedido.
 *       · Botón para confirmar compra.
 *   - Usa CheckoutViewModel para cargar dirección/teléfono
 *     desde Firestore (colección "usuarios").
 *   - Usa CartStore como fuente de verdad del carrito.
 *   - Usa MenuDesplegableHelper para el navbar/top sheet.
 * ============================================================
 */
public class CheckoutActivity extends AppCompatActivity {

    // ---------- ViewModel ----------
    private CheckoutViewModel checkoutViewModel;

    // ---------- UI: lista de productos ----------
    private LinearLayout llDetailProducts;
    private TextView tvFinalTotal;


    // ---------- Servicios ----------
    int total_servicio;

    // ---------- UI: formulario de envío ----------
    private CheckBox cbUseSavedAddress;
    private EditText etAddress;
    private EditText etPostalCode;
    private EditText etPhone;
    private EditText etNotes;

    private MaterialButton btnCalculateShipping;
    private MaterialButton btnGoToPayment;
    private MaterialButton btnBackToCart;

    // ---------- Navbar helper ----------
    private MenuDesplegableHelper menuHelper;

    // ---------- Soporte ----------
    private LayoutInflater inflater;
    private final NumberFormat ars =
            NumberFormat.getCurrencyInstance(new Locale("es", "AR"));

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1) Verificar login. Si no hay usuario, redirige a LoginActivity.
        if (!ensureUserLoggedIn()) {
            return;
        }

        setContentView(R.layout.activity_cart_detail);

        // 2) Navbar / menú deslizante con helper
        setupNavbar();

        // 3) Bind de vistas de checkout
        initViews();

        // 4) Inicializar ViewModel y observers
        checkoutViewModel = new ViewModelProvider(this).get(CheckoutViewModel.class);
        setupObservers();

        // 5) Cargar dirección guardada (si existe) al entrar al checkout
        checkoutViewModel.loadUserAddressFromFirestore();

        // 6) Render inicial del carrito
        inflater = LayoutInflater.from(this);
        renderCheckoutItems(CartStore.get().getItems());

        //        verifico que llegue bien el monto
        total_servicio = getIntent().getIntExtra("SERVICIO_TOTAL", 0);
        Log.d("CHECKOUT", "Total recibido: " + total_servicio);
        updateTotalLabel();



    }

    // ----------------------------------------------------------
    // Verifica si hay usuario logueado en FirebaseAuth
    // ----------------------------------------------------------
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

    // ----------------------------------------------------------
    // Configura el navbar usando MenuDesplegableHelper
    // ----------------------------------------------------------
    private void setupNavbar() {
        ImageView btnMenu = findViewById(R.id.btnMenu);
        View overlay = findViewById(R.id.overlay);
        View topSheet = findViewById(R.id.topSheet);

        View btnInicio = findViewById(R.id.btnInicio);
        View btnMisDatos = findViewById(R.id.btnMisDatos);
        View btnMiCarrito = findViewById(R.id.btnMiCarrito);
        View btnImpresionesCopias = findViewById(R.id.btnImpresionesCopias);
        View btnCerrarSesion = findViewById(R.id.btnCerrarSesion);



        menuHelper = new MenuDesplegableHelper(
                this,
                btnMenu,
                overlay,
                topSheet,
                btnInicio,
                btnMisDatos,
                btnMiCarrito,
                btnImpresionesCopias,
                btnCerrarSesion
        );
        menuHelper.initMenu();
    }

    // ----------------------------------------------------------
    // Bind de vistas y listeners de la pantalla de checkout
    // ----------------------------------------------------------
    private void initViews() {
        // Botón para volver al carrito
        btnBackToCart = findViewById(R.id.btnBackToCart);
        if (btnBackToCart != null) {
            btnBackToCart.setOnClickListener(v -> {
                Intent intent = new Intent(CheckoutActivity.this, CartActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            });
        }

        // Contenedor de productos y total
        llDetailProducts = findViewById(R.id.llDetailProducts);
        tvFinalTotal = findViewById(R.id.tvFinalTotal);

        // Formulario de envío
        cbUseSavedAddress = findViewById(R.id.cbUseSavedAddress);
        etAddress = findViewById(R.id.etAddress);
        etPostalCode = findViewById(R.id.etPostalCode);
        etPhone = findViewById(R.id.etPhone);
        etNotes = findViewById(R.id.etNotes);

        btnCalculateShipping = findViewById(R.id.btnCalculateShipping);
        btnGoToPayment = findViewById(R.id.btnGoToPayment);

        // Listener del checkbox: SOLO dispara la carga desde Firestore
        if (cbUseSavedAddress != null) {
            cbUseSavedAddress.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    // Vuelve a leer Firestore por si la dirección fue actualizada en otra pantalla
                    checkoutViewModel.loadUserAddressFromFirestore();
                }
                // Si se desmarca, dejamos que el usuario edite manualmente
            });
        }

        // Botón "Calcular envío"
        if (btnCalculateShipping != null) {
            btnCalculateShipping.setOnClickListener(v -> onCalculateShippingClicked());
        }

        // Botón "Realizar el pago"
        if (btnGoToPayment != null) {
            btnGoToPayment.setOnClickListener(v -> onPlaceAllOrders());
        }
    }

    // ----------------------------------------------------------
    // Observa LiveData del CheckoutViewModel
    // ----------------------------------------------------------
    private void setupObservers() {
        // Dirección desde Firestore
        checkoutViewModel.getAddress().observe(this, value -> {
            // Si el checkbox está marcado y hay dirección -> autocompletar
            if (cbUseSavedAddress != null
                    && cbUseSavedAddress.isChecked()
                    && !TextUtils.isEmpty(value)
                    && etAddress != null) {

                etAddress.setText(value);
            }
        });

        // Teléfono desde Firestore (opcional: si querés auto-rellenar)
        checkoutViewModel.getPhone().observe(this, value -> {
            if (cbUseSavedAddress != null
                    && cbUseSavedAddress.isChecked()
                    && !TextUtils.isEmpty(value)
                    && etPhone != null) {

                etPhone.setText(value);
            }
        });
    }

    // ----------------------------------------------------------
    // Renderiza las líneas de detalle del carrito en llDetailProducts
    // usando item_product_checkout.xml
    // ----------------------------------------------------------
    private void renderCheckoutItems(List<CartItem> items) {
        if (llDetailProducts == null) return;

        llDetailProducts.removeAllViews();
        if (items == null || items.isEmpty()) {
            // Podrías mostrar un mensaje de "Carrito vacío" aquí si lo necesitás.
            return;
        }

        for (CartItem item : items) {
            View row = inflater.inflate(
                    R.layout.item_product_checkout,
                    llDetailProducts,
                    false
            );

            TextView tvName = row.findViewById(R.id.tvCheckoutName);
            TextView tvQty = row.findViewById(R.id.tvCheckoutQty);
            TextView tvSubtotal = row.findViewById(R.id.tvCheckoutTotal);

            if (tvName != null) {
                tvName.setText(item.product.name);
            }
            if (tvQty != null) {
                tvQty.setText("Cantidad: " + item.qty);
            }
            if (tvSubtotal != null) {
                int subtotal = item.qty * item.product.price;
                tvSubtotal.setText("Subtotal: " + ars.format(subtotal));
            }

            llDetailProducts.addView(row);
        }
    }

    // ----------------------------------------------------------
    // Actualiza el label de total general del checkout
    // ----------------------------------------------------------
    private int updateTotalLabel() {
        int total = CartStore.get().getTotalAmount();

        total_servicio = getIntent().getIntExtra("SERVICIO_TOTAL", 0);
        total += total_servicio;

        if (tvFinalTotal != null) {
            tvFinalTotal.setText("Total: " + ars.format(total));
        }

        return total;
    }

    // ----------------------------------------------------------
    // Acción “Realizar todos los pedidos”
    // ----------------------------------------------------------
    private void onPlaceAllOrders() {
//        actualizo el total
        int realizarPagoTotal = updateTotalLabel();
//        int items = CartStore.get().getTotalQty();
//        int total = CartStore.get().getTotalAmount();
        if (realizarPagoTotal <= 0) return;

        new AlertDialog.Builder(this)
                .setTitle("Confirmar compra")
                .setMessage("Vas a realizar un pedido por un total de " + ars.format(realizarPagoTotal))
                .setPositiveButton("Confirmar", (dialog, which) -> {
                    CartStore.get().clear();
                    Intent i = new Intent(CheckoutActivity.this, SuccessActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ----------------------------------------------------------
    // Lógica de cálculo de envío según CP (4 dígitos)
    // ----------------------------------------------------------
    private void onCalculateShippingClicked() {
        String cp = etPostalCode != null
                ? etPostalCode.getText().toString().trim()
                : "";

        if (cp.isEmpty()) {
            showShippingDialog("Ingresá un código postal para calcular el envío.");
            return;
        }

        if (!cp.matches("\\d{4}")) {
            showShippingDialog("El código postal debe tener 4 dígitos numéricos.");
            return;
        }

        if (isAmbaPostalCode(cp)) {
            showShippingDialog("envio gratis!");
        } else {
            showShippingDialog("Por ahora no se hacen envios a esa direccion.");
        }
    }

    private void showShippingDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Envío")
                .setMessage(message)
                .setPositiveButton("Aceptar", null)
                .show();
    }

    /**
     * Determina si un código postal corresponde a CABA / Gran Buenos Aires.
     *
     * Implementación simplificada:
     *   - Espera SIEMPRE 4 dígitos (ej: 1425).
     *   - Considera AMBA si el número está entre 1000 y 1999.
     */
    private boolean isAmbaPostalCode(String cp) {
        try {
            int value = Integer.parseInt(cp);
            return value >= 1000 && value <= 1999;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
