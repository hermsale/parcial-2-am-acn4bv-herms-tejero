package com.example.parcial_1;

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

import com.example.parcial_1.data.CartStore;
import com.example.parcial_1.model.CartItem;
import com.example.parcial_1.model.Product;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class CartActivity extends AppCompatActivity {

    private LinearLayout llCartListContainer;
    private TextView tvCartGrandTotal;
    private final NumberFormat ars = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        Toolbar toolbar = findViewById(R.id.appToolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setTitle(""); // dejamos espacio para userchip a futuro
            // Click del ícono hamburguesa (derecha) -> desplegar opciones
            toolbar.setOnMenuItemClickListener(this::onToolbarMenuClick);
        }

        llCartListContainer = findViewById(R.id.llCartListContainer);
        tvCartGrandTotal = findViewById(R.id.tvCartGrandTotal);

        MaterialButton btnBack = findViewById(R.id.btnBackToCatalog);
        btnBack.setOnClickListener(v -> finish()); // volver a catálogo

        MaterialButton btnPlaceAll = findViewById(R.id.btnPlaceAllOrders);
        if (btnPlaceAll != null) {
            btnPlaceAll.setOnClickListener(v -> onPlaceAllOrders());
        }

        renderCart();
    }

    private boolean onToolbarMenuClick(MenuItem item) {
        if (item.getItemId() == R.id.action_nav) {
            // Mostramos popup con navegación a Catálogo/Carrito
            View anchor = findViewById(R.id.action_nav_anchor); // ver include_app_bar.xml
            if (anchor == null) anchor = findViewById(R.id.appToolbar);
            PopupMenu pm = new PopupMenu(this, anchor);
            pm.getMenu().add(0, 1, 0, getString(R.string.menu_catalog));
            pm.getMenu().add(0, 2, 1, getString(R.string.menu_cart));
            pm.setOnMenuItemClickListener(m -> {
                if (m.getItemId() == 1) {
                    startActivity(new Intent(this, MainActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                    return true;
                } else if (m.getItemId() == 2) {
                    // Ya estamos aquí; opcionalmente refrescamos
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

    private void renderCart() {
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
            MaterialButton btnUploadPdf = row.findViewById(R.id.btnDetectFromPdf);
            MaterialButton btnPlaceOrder = row.findViewById(R.id.btnPlaceOrder);

            Product p = ci.product;

            iv.setImageResource(p.imageRes);
            tvName.setText(p.name);
            tvDesc.setText(p.desc);
            tvUnitPrice.setText(getString(R.string.unit_price_format, ars.format(p.price)));
            tvQty.setText(String.valueOf(ci.qty));
            tvItemTotal.setText(getString(R.string.item_total_format, ars.format(ci.qty * p.price)));

            btnMinus.setOnClickListener(v -> {
                CartStore.get().dec(p);
                rebindRowAfterChange(row, p);
                updateGrandTotal();
            });

            btnPlus.setOnClickListener(v -> {
                CartStore.get().inc(p);
                rebindRowAfterChange(row, p);
                updateGrandTotal();
            });

            if (p.copyBased) {
                btnUploadPdf.setVisibility(View.VISIBLE);
                btnUploadPdf.setText(getString(R.string.upload_pdf));
                btnUploadPdf.setOnClickListener(v -> {
                    // Simulación: archivo PDF con N hojas
                    String fakeFileName = "Trabajo.pdf";
                    int fakePages = 37;
                    CartStore.get().setQty(p, fakePages);
                    new AlertDialog.Builder(this)
                            .setTitle("PDF cargado")
                            .setMessage(fakeFileName + " (" + fakePages + " hojas)\nCantidad actualizada.")
                            .setPositiveButton(getString(R.string.ok), null)
                            .show();
                    rebindRowAfterChange(row, p);
                    updateGrandTotal();
                });
            } else {
                btnUploadPdf.setVisibility(View.GONE);
            }

            btnPlaceOrder.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Procesando pedido")
                        .setMessage("Redireccionado a vista Pagos… (simulado)")
                        .setPositiveButton(getString(R.string.ok), null)
                        .show();
            });

            llCartListContainer.addView(row);
        }

        updateGrandTotal();
    }

    private void rebindRowAfterChange(View row, Product p) {
        CartItem current = null;
        for (CartItem x : CartStore.get().getItems()) {
            if (x.product.name.equals(p.name)) { current = x; break; }
        }
        if (current == null) {
            renderCart();
            return;
        }
        TextView tvQty = row.findViewById(R.id.tvQty);
        TextView tvItemTotal = row.findViewById(R.id.tvItemTotal);
        tvQty.setText(String.valueOf(current.qty));
        tvItemTotal.setText(getString(R.string.item_total_format, ars.format(current.qty * p.price)));
    }

    private void updateGrandTotal() {
        int total = CartStore.get().getTotalAmount();
        tvCartGrandTotal.setText(getString(R.string.cart_total_label_format, ars.format(total)));

        MaterialButton btnPlaceAll = findViewById(R.id.btnPlaceAllOrders);
        if (btnPlaceAll != null) {
            boolean enabled = CartStore.get().getTotalQty() > 0;
            btnPlaceAll.setEnabled(enabled);
            btnPlaceAll.setAlpha(enabled ? 1f : 0.5f);
        }
    }

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
}
