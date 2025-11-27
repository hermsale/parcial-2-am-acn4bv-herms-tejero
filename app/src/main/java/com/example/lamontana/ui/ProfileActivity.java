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
import com.example.lamontana.data.user.UserRepository;
import com.example.lamontana.data.user.UserStore;
import com.example.lamontana.ui.navbar.MenuDesplegableHelper;

/*
 * ============================================================
 * Archivo: ProfileActivity.java
 * Paquete: com.example.lamontana.ui
 * ------------------------------------------------------------
 * ¿De qué se encarga?
 *   - Muestra y permite editar los datos básicos del usuario:
 *       · nombre
 *       · apellido
 *       · email (normalmente solo lectura)
 *       · teléfono
 *       · dirección
 *   - Carga y guarda esos datos en UserStore (estado en memoria).
 *   - Sincroniza los cambios con Firestore en la colección
 *     "usuarios", actualizando el documento del usuario actual
 *     a través de UserRepository.updateUserProfile(...).
 *   - Muestra el navbar con menú desplegable (top sheet) para:
 *       · Ir al inicio (Catálogo)
 *       · Ir a Mis datos
 *       · Ir al carrito
 *       · Cerrar sesión
 *     usando el helper reutilizable MenuDesplegableHelper.
 *
 * Clases usadas:
 *   - UserStore:
 *       * Almacena datos básicos del usuario en memoria.
 *   - UserRepository:
 *       * Encapsula operaciones sobre la colección "usuarios"
 *         en Firestore (actualizar perfil).
 *   - MenuDesplegableHelper:
 *       * Maneja el menú top-sheet del navbar (animaciones y
 *         navegación).
 *   - AlertDialog:
 *       * Para el mensaje de "Cambiar contraseña" (placeholder).
 *
 * Métodos presentes:
 *   - onCreate(Bundle):
 *       * Configura la UI, inicializa vistas, menú (helper),
 *         listeners y carga datos del usuario.
 *   - initViews():
 *       * Enlaza las vistas de los campos de perfil.
 *   - setupListeners():
 *       * Configura botones de "Guardar" y "Cambiar contraseña".
 *   - saveProfile():
 *       * Lee campos del formulario.
 *       * Actualiza UserStore en memoria.
 *       * Intenta sincronizar con Firestore usando UserRepository.
 *   - changePasswordDialog():
 *       * Muestra un diálogo informativo (sin lógica real aún).
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

    /**
     * Guarda los datos editados:
     *  1) Actualiza UserStore en memoria.
     *  2) Si tenemos un UID válido, actualiza también Firestore
     *     en la colección "usuarios" (documento usuarios/{uid}).
     */
    private void saveProfile() {
        // 1) Leer campos desde la UI (con null-checks defensivos)
        String nombre = etNombre != null ? etNombre.getText().toString().trim() : "";
        String apellido = etApellido != null ? etApellido.getText().toString().trim() : "";
        String telefono = etTelefono != null ? etTelefono.getText().toString().trim() : "";
        String direccion = etDireccion != null ? etDireccion.getText().toString().trim() : "";

        // 2) Actualizar UserStore en memoria
        UserStore store = UserStore.get();
        // nombre forma parte de los datos "básicos"
        store.setBasicData(store.uid, nombre.isEmpty() ? store.nombre : nombre, store.email);
        // los opcionales se actualizan siempre
        store.setOptionalData(apellido, telefono, direccion);

        // 3) Obtener UID para Firestore
        String uid = store.uid;

        if (uid == null || uid.trim().isEmpty()) {
            // No tenemos UID válido: no podemos sincronizar con Firestore
            Toast.makeText(
                    this,
                    "Datos guardados localmente, pero no se pudo sincronizar con Firestore (UID vacío).",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        // 4) Actualizar Firestore a través del repositorio
        UserRepository.getInstance()
                .updateUserProfile(
                        uid,
                        nombre.isEmpty() ? null : nombre,
                        apellido.isEmpty() ? null : apellido,
                        telefono.isEmpty() ? null : telefono,
                        direccion.isEmpty() ? null : direccion
                )
                .addOnSuccessListener(unused -> Toast.makeText(
                        ProfileActivity.this,
                        "Datos guardados correctamente",
                        Toast.LENGTH_SHORT
                ).show())
                .addOnFailureListener(e -> Toast.makeText(
                        ProfileActivity.this,
                        "Error al guardar datos en Firestore: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show());
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
