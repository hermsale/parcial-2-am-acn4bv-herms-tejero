package com.example.lamontana.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.lamontana.R;
import com.example.lamontana.data.user.UserStore;
import com.example.lamontana.ui.navbar.MenuDesplegableHelper;
import com.example.lamontana.ui.profile.ModificarContrasenaHelper;
import com.example.lamontana.viewmodel.ProfileViewModel;

/*
 * ============================================================
 * Archivo: ProfileActivity.java
 * Paquete: com.example.lamontana.ui
 * ------------------------------------------------------------
 * ¿De qué se encarga?
 *   - Muestra y permite editar los datos básicos del usuario:
 *       · nombre
 *       · apellido
 *       · email (solo lectura)
 *       · teléfono
 *       · dirección
 *   - Carga esos datos desde UserStore (estado en memoria) a
 *     los campos del formulario.
 *   - Delegar el guardado de cambios al ProfileViewModel, que:
 *       · Actualiza UserStore.
 *       · Sincroniza con Firestore (colección "usuarios").
 *       · Expone estados loading / saveSuccess / errorMessage.
 *   - Ofrece la acción "Cambiar contraseña", delegando toda la
 *     lógica de Auth + Firestore en:
 *       · ModificarContrasenaHelper.mostrarDialogoCambioContrasena(...)
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
 *   - ProfileViewModel:
 *       * Orquesta el guardado de perfil (UserStore + Firestore).
 *   - MenuDesplegableHelper:
 *       * Maneja el menú top-sheet del navbar (animaciones y
 *         navegación).
 *   - ModificarContrasenaHelper:
 *       * Encapsula el flujo de "Cambiar contraseña".
 *
 * Métodos presentes:
 *   - onCreate(Bundle):
 *       * Configura la UI, inicializa vistas, menú (helper),
 *         ViewModel, listeners y carga datos del usuario.
 *   - initViews():
 *       * Enlaza las vistas de los campos de perfil.
 *   - setupMenu():
 *       * Configura MenuDesplegableHelper.
 *   - setupListeners():
 *       * Configura botones de "Guardar" y "Cambiar contraseña".
 *   - observeViewModel():
 *       * Observa loading / saveSuccess / errorMessage del
 *         ProfileViewModel y actualiza la UI.
 *   - saveProfile():
 *       * Lee campos del formulario y delega el guardado en
 *         profileViewModel.saveProfile(...).
 *   - loadUserData() / bindUserData():
 *       * Carga los datos desde UserStore en los EditText.
 * ============================================================
 */

public class ProfileActivity extends AppCompatActivity {

    // --- Vistas de formulario ---
    private EditText etNombre;
    private EditText etApellido;
    private EditText etEmail;
    private EditText etTelefono;
    private EditText etDireccion;
    private View btnSave;
    private View btnChangePassword;

    // Helper para el menú desplegable del navbar
    private MenuDesplegableHelper menuHelper;

    // ViewModel para gestionar perfil de usuario
    private ProfileViewModel profileViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initViews();
        setupMenu();

        // ---------- Inicializar ViewModel ----------
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        observeViewModel();

        setupListeners();
        loadUserData();
    }

    /**
     * Enlaza las vistas de la pantalla con las variables de la Activity.
     */
    private void initViews() {
        etNombre = findViewById(R.id.etNombre);
        etApellido = findViewById(R.id.etApellido);
        etEmail = findViewById(R.id.etEmail);
        etTelefono = findViewById(R.id.etTelefono);
        etDireccion = findViewById(R.id.etDireccion);

        btnSave = findViewById(R.id.btnSave);
        btnChangePassword = findViewById(R.id.btnChangePassword);
    }

    /**
     * Configura el menú desplegable (navbar) usando el helper común.
     */
    private void setupMenu() {
        ImageView btnMenu = findViewById(R.id.btnMenu); // viene de include_navbar.xml
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
    }

    /**
     * Carga los datos desde UserStore y los muestra en los EditText.
     */
    private void bindUserData() {
        UserStore u = UserStore.get();

        if (etNombre != null)   etNombre.setText(u.nombre);
        if (etApellido != null) etApellido.setText(u.apellido);
        if (etEmail != null)    etEmail.setText(u.email);
        if (etTelefono != null) etTelefono.setText(u.telefono);
        if (etDireccion != null)etDireccion.setText(u.direccion);
    }

    /**
     * Configura listeners para los botones de "Guardar" y "Cambiar contraseña".
     */
    private void setupListeners() {
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> saveProfile());
        }

        if (btnChangePassword != null) {
            btnChangePassword.setOnClickListener(
                    v -> ModificarContrasenaHelper.mostrarDialogoCambioContrasena(ProfileActivity.this)
            );
        }
    }

    /**
     * Observa los estados del ProfileViewModel para actualizar la UI.
     */
    private void observeViewModel() {
        if (profileViewModel == null) return;

        // Loading: habilitar / deshabilitar botón "Guardar cambios"
        profileViewModel.getLoading().observe(this, isLoading -> {
            if (btnSave != null && isLoading != null) {
                btnSave.setEnabled(!isLoading);
            }
        });

        // saveSuccess: mostrar mensaje de éxito
        profileViewModel.getSaveSuccess().observe(this, success -> {
            if (success != null && success) {
                Toast.makeText(
                        ProfileActivity.this,
                        "Datos guardados correctamente",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        // errorMessage: mostrar mensaje de error
        profileViewModel.getErrorMessage().observe(this, msg -> {
            if (msg != null && !msg.trim().isEmpty()) {
                Toast.makeText(
                        ProfileActivity.this,
                        msg,
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    /**
     * Lee los campos del formulario y delega el guardado en el ViewModel.
     */
    private void saveProfile() {
        // 1) Leer campos desde la UI
        String nombre = etNombre != null ? etNombre.getText().toString().trim() : "";
        String apellido = etApellido != null ? etApellido.getText().toString().trim() : "";
        String telefono = etTelefono != null ? etTelefono.getText().toString().trim() : "";
        String direccion = etDireccion != null ? etDireccion.getText().toString().trim() : "";

        // Validación mínima: nombre no vacío (podés sumar más si querés)
        if (nombre.isEmpty()) {
            if (etNombre != null) {
                etNombre.setError("Ingrese su nombre");
                etNombre.requestFocus();
            }
            return;
        }

        if (profileViewModel == null) {
            Toast.makeText(
                    this,
                    "Error interno: ProfileViewModel no inicializado",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        // 2) Delegar la lógica de guardado al ViewModel
        profileViewModel.saveProfile(nombre, apellido, telefono, direccion);
    }

    /**
     * Carga los datos del usuario desde UserStore a la UI.
     */
    private void loadUserData() {
        bindUserData();
    }
}
