package com.example.lamontana.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.lamontana.R;
import com.example.lamontana.data.user.UserStore;

public class ProfileActivity extends AppCompatActivity {

    private EditText etNombre, etApellido, etEmail, etTelefono, etDireccion;
    private View overlay, topSheet;
    private boolean isMenuOpen = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initViews();
        setupMenu();
        setupListeners();
        loadUserData();

    }

    private void initViews() {
        etNombre = findViewById(R.id.etNombre);
        etApellido = findViewById(R.id.etApellido);
        etEmail = findViewById(R.id.etEmail);
        etTelefono = findViewById(R.id.etTelefono);
        etDireccion = findViewById(R.id.etDireccion);

        overlay = findViewById(R.id.overlay);
        topSheet = findViewById(R.id.topSheet);
    }

    private void bindUserData() {
        UserStore u = UserStore.get();

        etNombre.setText(u.nombre);
        etApellido.setText(u.apellido);
        etEmail.setText(u.email);
        etTelefono.setText(u.telefono);
        etDireccion.setText(u.direccion);
    }

    private void setupListeners() {

        findViewById(R.id.btnSave).setOnClickListener(v -> saveProfile());
        findViewById(R.id.btnChangePassword).setOnClickListener(v -> changePasswordDialog());
    }

    private void setupMenu() {
        ImageView btnMenu = findViewById(R.id.btnMenu); // viene del include_navbar.xml

        btnMenu.setOnClickListener(v -> toggleMenu());
        overlay.setOnClickListener(v -> closeMenu());

        findViewById(R.id.btnInicio).setOnClickListener(v -> {
            closeMenu();
            startActivity(new Intent(this, CatalogActivity.class));
            finish();
        });

        // listeners del menú del top sheet
        findViewById(R.id.btnMisDatos).setOnClickListener(v -> closeMenu());

        findViewById(R.id.btnMiCarrito).setOnClickListener(v -> {
            closeMenu();
            startActivity(new Intent(this, CartActivity.class));
        });

        findViewById(R.id.btnCerrarSesion).setOnClickListener(v -> {
            closeMenu();
            UserStore.get().clear();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void saveProfile() {
        String apellido = etApellido.getText().toString().trim();
        String telefono = etTelefono.getText().toString().trim();
        String direccion = etDireccion.getText().toString().trim();

        UserStore.get().setOptionalData(apellido, telefono, direccion);

        Toast.makeText(this, "Datos guardados correctamente", Toast.LENGTH_SHORT).show();
    }

    private void changePasswordDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Cambiar contraseña")
                .setMessage("Funcionalidad disponible cuando la app esté conectada a Firebase Auth.")
                .setPositiveButton("OK", null)
                .show();
    }

    // ========== MENÚ TOP-SHEET ==========

    private void toggleMenu() {
        if (isMenuOpen) closeMenu();
        else openMenu();
    }

    private void openMenu() {
        topSheet.setVisibility(View.VISIBLE);
        topSheet.startAnimation(
                android.view.animation.AnimationUtils.loadAnimation(this, R.anim.top_sheet_down)
        );
        overlay.setVisibility(View.VISIBLE);
        isMenuOpen = true;
    }

    private void closeMenu() {
        topSheet.startAnimation(
                android.view.animation.AnimationUtils.loadAnimation(this, R.anim.top_sheet_up)
        );
        overlay.setVisibility(View.GONE);
        topSheet.setVisibility(View.GONE);
        isMenuOpen = false;
    }

    private void loadUserData() {
        // Obtener datos del store
        UserStore u = UserStore.get();

        // Cargar los campos
        if (etNombre != null) etNombre.setText(u.nombre);
        if (etApellido != null) etApellido.setText(u.apellido);
        if (etEmail != null) etEmail.setText(u.email);
        if (etTelefono != null) etTelefono.setText(u.telefono);
        if (etDireccion != null) etDireccion.setText(u.direccion);
    }
}
