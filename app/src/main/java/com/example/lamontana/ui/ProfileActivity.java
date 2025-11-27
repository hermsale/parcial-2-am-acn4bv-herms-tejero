package com.example.lamontana.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.lamontana.R;
import com.example.lamontana.data.user.UserStore;
import com.example.lamontana.ui.navbar.MenuDesplegableHelper;

/*
 * ============================================================
 * Archivo: ProfileActivity.java
 * Paquete: com.example.lamontana.ui
 * ------------------------------------------------------------
 * ¿De qué se encarga?
 *   - Muestra y permite editar los datos básicos del usuario
 *     (nombre, apellido, email, teléfono, dirección).
 *   - Carga y guarda esos datos en UserStore (capa de datos
 *     local de usuario).
 *   - Muestra el navbar con menú desplegable (top sheet) para:
 *       · Ir al inicio (Catálogo)
 *       · Ir a Mis datos (esta misma pantalla)
 *       · Ir al carrito
 *       · Cerrar sesión
 *     usando el helper reutilizable MenuDesplegableHelper.
 *
 * Clases usadas:
 *   - UserStore: almacena datos básicos del usuario.
 *   - MenuDesplegableHelper: maneja el menú top-sheet del navbar.
 *   - AlertDialog: para el mensaje de "Cambiar contraseña".
 *
 * Métodos presentes:
 *   - onCreate(Bundle):
 *       * Configura la UI, inicializa vistas, menú (via helper)
 *         y listeners. Carga datos del usuario.
 *   - initViews():
 *       * Enlaza las vistas de los campos de perfil y vistas
 *         del navbar (overlay, topSheet ya no se manejan aquí).
 *   - setupListeners():
 *       * Configura botones de "Guardar" y "Cambiar contraseña".
 *   - saveProfile():
 *       * Actualiza datos opcionales en UserStore (apellido,
 *         teléfono, dirección).
 *   - changePasswordDialog():
 *       * Muestra un diálogo informativo (placeholder).
 *   - loadUserData():
 *       * Carga los datos desde UserStore en los EditText.
 *   - bindUserData():
 *       * Versión interna para vincular datos a las vistas.
 * ============================================================
 */

public class ProfileActivity extends AppCompatActivity {

    private EditText etNombre, etApellido, etEmail, etTelefono, etDireccion;

    // Helper para el menú desplegable del navbar
    private MenuDesplegableHelper menuHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initViews();

        // ---------- Configurar menú desplegable con helper ----------
        ImageView btnMenu = findViewById(R.id.btnMenu);       // viene del include_navbar.xml
        View overlay = findViewById(R.id.overlay);
        View topSheet = findViewById(R.id.topSheet);

        View btnInicio = findViewById(R.id.btnInicio);
        View btnMisDatos = findViewById(R.id.btnMisDatos);
        View btnMiCarrito = findViewById(R.id.btnMiCarrito);
        View btnCerrarSesion = findViewById(R.id.btnCerrarSesion);

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

        setupListeners();
        loadUserData();
    }

    private void initViews() {
        etNombre = findViewById(R.id.etNombre);
        etApellido = findViewById(R.id.etApellido);
        etEmail = findViewById(R.id.etEmail);
        etTelefono = findViewById(R.id.etTelefono);
        etDireccion = findViewById(R.id.etDireccion);
    }

    private void bindUserData() {
        UserStore u = UserStore.get();

        if (etNombre != null)   etNombre.setText(u.nombre);
        if (etApellido != null) etApellido.setText(u.apellido);
        if (etEmail != null)    etEmail.setText(u.email);
        if (etTelefono != null) etTelefono.setText(u.telefono);
        if (etDireccion != null)etDireccion.setText(u.direccion);
    }

    private void setupListeners() {
        View btnSave = findViewById(R.id.btnSave);
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> saveProfile());
        }

        View btnChangePassword = findViewById(R.id.btnChangePassword);
        if (btnChangePassword != null) {
            btnChangePassword.setOnClickListener(v -> changePasswordDialog());
        }
    }

    private void saveProfile() {
        String apellido = etApellido != null ? etApellido.getText().toString().trim() : "";
        String telefono = etTelefono != null ? etTelefono.getText().toString().trim() : "";
        String direccion = etDireccion != null ? etDireccion.getText().toString().trim() : "";

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

    private void loadUserData() {
        bindUserData();
    }
}
