package com.example.lamontana.ui;


import android.content.Intent;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.lamontana.R;
import com.example.lamontana.ui.navbar.MenuDesplegableHelper;

import java.util.Arrays;
public class ServiciosActivity extends AppCompatActivity {

    // Helper para el menú desplegable del navbar
    private MenuDesplegableHelper menuHelper;

    // Precios base
    private static final double PRECIO_CARILLA = 10;
    private static final double PRECIO_BN = 40;
    private static final double PRECIO_COLOR = 120;
    private static final double RECARGO_DOBLE_FAZ = 15;
    private static final double PRECIO_ANILLADO = 900;
    private static final double PRECIO_ENCUADERNADO = 1500;
    private int totalActual = 0;


//
    private TextView tvCartGrandTotal;
    TextView txtTotal;
    Button btnPagar;


    Button btnArchivo;

    TextView txtArchivo;
    EditText edtCarillas, edtNotas;
    Spinner spPaperSize, spMetodoPago;
    RadioGroup rgModo;
    CheckBox chkDobleFaz, chkEncuadernado, chkAnillado;

    private Uri archivoSeleccionadoUri;

    //        metodo para contar carillas
    private int obtenerCarillasDesdePdf(Uri uri) {
        try {
            ParcelFileDescriptor pfd =
                    getContentResolver().openFileDescriptor(uri, "r");

            if (pfd == null) return 0;

            PdfRenderer renderer = new PdfRenderer(pfd);
            int paginas = renderer.getPageCount();

            renderer.close();
            pfd.close();

            return paginas;

        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private void recalcularTotal() {

        int carillas = 0;
        if (!edtCarillas.getText().toString().isEmpty()) {
            carillas = Integer.parseInt(edtCarillas.getText().toString());
        }

//        variable local para sumar todos los servicios
        int total = 0;

        // Precio base por carilla
        total += carillas * PRECIO_CARILLA;

        // Blanco y negro o color
        int modoSeleccionado = rgModo.getCheckedRadioButtonId();
        if (modoSeleccionado == R.id.rbBN) {
            total += carillas * PRECIO_BN;
        } else if (modoSeleccionado == R.id.rbColor) {
            total += carillas * PRECIO_COLOR;
        }

        // Doble faz (recargo por carilla)
        if (chkDobleFaz.isChecked()) {
            total += carillas * RECARGO_DOBLE_FAZ;
        }

        // Servicios adicionales
        if (chkAnillado.isChecked()) {
            total += PRECIO_ANILLADO;
        }

        if (chkEncuadernado.isChecked()) {
            total += PRECIO_ENCUADERNADO;
        }

    //  totalActual almacena el total, es la variable que pasamos por intent a checkoutActivity
        totalActual = total;

        txtTotal.setText("TOTAL: $" + total);
    }


//             new Servicio("fotocopiado_bn",
//                    "Fotocopiado en blanco y negro por carilla, simple faz.",
//                    true, 40),
//
//            new Servicio("fotocopiado_color",
//                    "Fotocopiado/color por carilla, simple faz.",
//                    true, 120),
//
//            new Servicio("doble_faz",
//                    "Recargo o ajuste por impresión doble faz.",
//                    true, 0.8),
//
//            new Servicio("anillado",
//                    "Anillado plástico o metálico estándar.",
//                    true, 900),
//
//            new Servicio("encuadernado",
//                    "Encuadernado simple (tapa blanda).",
//                    true, 1500)

    ActivityResultLauncher<String> seleccionarArchivo;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_servicios);

//        seccion total y pagar
        txtTotal = findViewById(R.id.txtTotal);
        btnPagar = findViewById(R.id.btnPagar);

        // Referencias
        btnArchivo = findViewById(R.id.btnArchivo);
        txtArchivo = findViewById(R.id.txtArchivo);
        edtCarillas = findViewById(R.id.edtCarillas);
        edtNotas = findViewById(R.id.edtNotas);
        spPaperSize = findViewById(R.id.spPaperSize);
        spMetodoPago = findViewById(R.id.spMetodoPago);
        rgModo = findViewById(R.id.rgModo);
        chkDobleFaz = findViewById(R.id.chkDobleFaz);
        chkEncuadernado = findViewById(R.id.chkEncuadernado);
        chkAnillado = findViewById(R.id.chkAnillado);

        // ---------- Navbar / Menú deslizante con helper ----------
        ImageView btnMenu = findViewById(R.id.btnMenu);
        View overlay = findViewById(R.id.overlay);
        View topSheet = findViewById(R.id.topSheet);

        View btnMisDatos = findViewById(R.id.btnMisDatos);
        View btnMiCarrito = findViewById(R.id.btnMiCarrito);
        View btnCerrarSesion = findViewById(R.id.btnCerrarSesion);
        View btnInicio = findViewById(R.id.btnInicio);
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

//        Btn para realizar el pago. redirigir a CheckoutActivity
        if (btnPagar != null){
            btnPagar.setOnClickListener(v -> {
                Intent intent = new Intent(ServiciosActivity.this, CheckoutActivity.class);
                intent.putExtra("SERVICIO_TOTAL", totalActual);
                startActivity(intent);
            }
            );
        };


        // Selector de archivo
        seleccionarArchivo = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {

                        archivoSeleccionadoUri = uri;

                        txtArchivo.setText(uri.getLastPathSegment());

                        // Utilizo el metodo para detectar carillas automáticamente
                        int carillas = obtenerCarillasDesdePdf(uri);

                        if (carillas > 0) {
                            edtCarillas.setText(String.valueOf(carillas));
                            recalcularTotal();
                        } else {
                            edtCarillas.setText("0");
                        }
                    }
                }
        );

        btnArchivo.setOnClickListener(v ->
                seleccionarArchivo.launch("application/pdf")
        );

        btnArchivo.setOnClickListener(v ->
                seleccionarArchivo.launch("*/*")
        );



//      escucha de cambios
        edtCarillas.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                recalcularTotal();
            }
        });

        rgModo.setOnCheckedChangeListener((group, checkedId) -> recalcularTotal());
        chkDobleFaz.setOnCheckedChangeListener((b, v) -> recalcularTotal());
        chkAnillado.setOnCheckedChangeListener((b, v) -> recalcularTotal());
        chkEncuadernado.setOnCheckedChangeListener((b, v) -> recalcularTotal());

        // Spinner tamaño hoja
        ArrayAdapter<String> paperAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                Arrays.asList("A4", "US Letter", "A3", "Oficio")
        );
        spPaperSize.setAdapter(paperAdapter);

        // Spinner método pago
        ArrayAdapter<String> pagoAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                Arrays.asList("Efectivo", "Transferencia")
        );
        spMetodoPago.setAdapter(pagoAdapter);



    }
}
