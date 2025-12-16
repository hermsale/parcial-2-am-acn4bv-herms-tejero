package com.example.lamontana.ui;


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

    ActivityResultLauncher<String> seleccionarArchivo;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_servicios);

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
