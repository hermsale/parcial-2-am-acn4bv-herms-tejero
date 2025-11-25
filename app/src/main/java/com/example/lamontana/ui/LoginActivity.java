package com.example.lamontana.ui;

/* -----------------------------------------------------------------------------
  Archivo: LoginActivity.java
  Responsabilidad:
    - Mostrar el formulario de inicio de sesión.
    - Validar datos básicos ingresados por el usuario (email y contraseña).
    - Navegar a la pantalla principal de catálogo si el login es válido.

  Alcance:
    - Es la pantalla de entrada de la aplicación (launcher).
    - Por ahora realiza un "login local" sin conexión real a servidor/Firebase.
    - Más adelante se integrará con Firebase Auth para login real.

  Lista de métodos públicos:
    - onCreate(Bundle savedInstanceState)
        * Ciclo de vida de la Activity. Inicializa la UI y configura listeners.

  Lista de métodos privados:
    - setupViews()
        * Enlaza los elementos visuales del layout con las variables de la Activity.
    - setupListeners()
        * Configura las acciones de los botones y eventos de la pantalla.
    - attemptLogin()
        * Valida los campos de email y contraseña.
        * Muestra mensajes de error si faltan datos.
        * Navega a la pantalla principal si el login es aceptado.

  Notas:
    - Esta implementación es intencionalmente simple para el parcial.
    - La lógica de autenticación real (Firebase Auth) se agregará en pasos posteriores.
----------------------------------------------------------------------------- */

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lamontana.MainActivity;
import com.example.lamontana.R;

public class LoginActivity extends AppCompatActivity {

    // -------------------------------------------------------------------------
    // Atributos de la UI
    // -------------------------------------------------------------------------
    private EditText etEmail;
    private EditText etPassword;
    private Button btnLogin;

    // -------------------------------------------------------------------------
    // Ciclo de vida
    // -------------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // IMPORTANTE: estos métodos deben ejecutarse después de setContentView
        setupViews();
        setupListeners();
    }

    // -------------------------------------------------------------------------
    // Métodos privados de inicialización
    // -------------------------------------------------------------------------

    /**
     * Enlaza los elementos del layout con las variables de la Activity.
     * Es crítico que los IDs usados aquí existan en activity_login.xml.
     */
    private void setupViews() {
        etEmail = findViewById(R.id.etEmail);       // Debe existir @+id/etEmail en el XML
        etPassword = findViewById(R.id.etPassword); // Debe existir @+id/etPassword en el XML
        btnLogin = findViewById(R.id.btnLogin);     // Debe existir @+id/btnLogin en el XML
    }

    /**
     * Configura los listeners de los botones y otros componentes interactivos.
     */
    private void setupListeners() {
        if (btnLogin != null) {
            btnLogin.setOnClickListener(view -> attemptLogin());
        }
    }

    // -------------------------------------------------------------------------
    // Lógica de negocio (simplificada)
    // -------------------------------------------------------------------------

    /**
     * Valida los campos del formulario de login y navega a la pantalla principal
     * si la validación es exitosa.
     *
     * Notas:
     * - Por ahora solo verificamos que los campos no estén vacíos.
     * - Más adelante se reemplazará por autenticación real con Firebase Auth.
     */
    private void attemptLogin() {
        String email = etEmail != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword != null ? etPassword.getText().toString().trim() : "";

        // Validaciones básicas
        if (TextUtils.isEmpty(email)) {
            if (etEmail != null) {
                etEmail.setError("Ingrese su email");
                etEmail.requestFocus();
            }
            return;
        }

        if (TextUtils.isEmpty(password)) {
            if (etPassword != null) {
                etPassword.setError("Ingrese su contraseña");
                etPassword.requestFocus();
            }
            return;
        }

        // ---------------------------------------------------------------------
        // Login local "de mentira" para el parcial:
        // Si llegamos hasta acá, consideramos el login como exitoso.
        // ---------------------------------------------------------------------

        Toast.makeText(this, "Login exitoso", Toast.LENGTH_SHORT).show();

        // Navegar a la pantalla principal (catálogo/carrito actual)
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("email", email); // Enviamos el email por si luego se usa
        startActivity(intent);

        // Opcional: cerrar la pantalla de login para que no se vuelva atrás
        finish();
    }
}
