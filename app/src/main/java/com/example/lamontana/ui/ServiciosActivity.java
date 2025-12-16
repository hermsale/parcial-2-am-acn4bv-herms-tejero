package com.example.lamontana.ui;


import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.lamontana.R;

import java.util.Arrays;
public class ServiciosActivity extends AppCompatActivity {

    Button btnArchivo;
    TextView txtArchivo;
    EditText edtCarillas, edtNotas;
    Spinner spPaperSize, spMetodoPago;
    RadioGroup rgModo;
    CheckBox chkDobleFaz, chkEncuadernado, chkAnillado;

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

        // Selector de archivo
        seleccionarArchivo = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        txtArchivo.setText(uri.getLastPathSegment());
                    }
                }
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
