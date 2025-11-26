package com.example.lamontana.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.lamontana.R;
import com.example.lamontana.viewmodel.LoginViewModel;

/* -----------------------------------------------------------------------------
  Archivo: LoginActivity.java
  Responsabilidad:
    - Mostrar el formulario de inicio de sesión.
    - Validar datos básicos ingresados por el usuario (email y contraseña).
    - Delegar el proceso de login real a LoginViewModel (Firebase Auth).
    - Navegar a la pantalla principal de catálogo si el login es válido.

  Alcance:
    - Es la pantalla de entrada de la aplicación (launcher).
    - Forma parte del flujo de autenticación usando Firebase Auth.
    - Observa LiveData del LoginViewModel para:
        * loading: habilitar/deshabilitar botones.
        * loginSuccess: navegar al catálogo.
        * errorMessage: mostrar mensajes de error.

  Lista de métodos públicos:
    - onCreate(Bundle savedInstanceState)
        * Ciclo de vida de la Activity. Inicializa la UI, el ViewModel y los listeners.

  Lista de métodos privados:
    - setupViews()
        * Enlaza los elementos visuales del layout con las variables de la Activity.
    - setupListeners()
        * Configura las acciones de los botones y eventos de la pantalla.
    - setupViewModel()
        * Obtiene una instancia de LoginViewModel.
    - observeViewModel()
        * Se suscribe a los LiveData de LoginViewModel (loading, loginSuccess, errorMessage).
    - attemptLogin()
        * Valida los campos de email y contraseña.
        * Si son válidos, llama a loginViewModel.login(email, password).
    - navigateToMain(String email)
        * Navega a la pantalla principal (MainActivity) tras login exitoso.

----------------------------------------------------------------------------- */
public class LoginActivity extends AppCompatActivity {

    // -------------------------------------------------------------------------
    // Atributos de la UI
    // -------------------------------------------------------------------------
    private EditText etEmail;
    private EditText etPassword;
    private Button btnLogin;
    // Botón para ir a la vista de crear cuenta
    private Button btnGoToSignup;

    // -------------------------------------------------------------------------
    // ViewModel
    // -------------------------------------------------------------------------
    private LoginViewModel loginViewModel;

    // -------------------------------------------------------------------------
    // Ciclo de vida
    // -------------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        setupViews();
        setupListeners();
        setupViewModel();
        observeViewModel();
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
        btnGoToSignup = findViewById(R.id.btnGoToSignup); // botón para ir a la pantalla de registro
    }

    /**
     * Configura los listeners de los botones y otros componentes interactivos.
     */
    private void setupListeners() {
        if (btnLogin != null) {
            btnLogin.setOnClickListener(view -> attemptLogin());
        }

        // Listener para ir al Signup
        if (btnGoToSignup != null) {
            btnGoToSignup.setOnClickListener(view -> {
                Intent intent = new Intent(this, SignupActivity.class);
                startActivity(intent);
            });
        }
    }

    /**
     * Inicializa el ViewModel de Login utilizando ViewModelProvider.
     */
    private void setupViewModel() {
        loginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);
    }

    /**
     * Se suscribe a los LiveData del LoginViewModel para reaccionar a cambios
     * de estado de la autenticación.
     */
    private void observeViewModel() {
        if (loginViewModel == null) return;

        // Observa el estado de carga
        loginViewModel.getLoading().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isLoading) {
                if (isLoading == null) return;

                // Deshabilitar / habilitar botón de login según el estado
                if (btnLogin != null) {
                    btnLogin.setEnabled(!isLoading);
                    btnLogin.setText(isLoading ? "Ingresando..." : "Ingresar");
                }

                if (btnGoToSignup != null) {
                    btnGoToSignup.setEnabled(!isLoading);
                }
            }
        });

        // Observa si el login fue exitoso
        loginViewModel.getLoginSuccess().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean success) {
                if (success != null && success) {
                    // Obtenemos el email actual del campo para mostrarlo o enviarlo
                    String email = etEmail != null
                            ? etEmail.getText().toString().trim()
                            : "";

                    Toast.makeText(LoginActivity.this,
                            "Login exitoso", Toast.LENGTH_SHORT).show();

                    navigateToMain(email);
                }
            }
        });

        // Observa mensajes de error
        loginViewModel.getErrorMessage().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String message) {
                if (message != null && !message.isEmpty()) {
                    Toast.makeText(LoginActivity.this,
                            message, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    // -------------------------------------------------------------------------
    // Lógica de negocio (validación local + llamada al ViewModel)
    // -------------------------------------------------------------------------

    /**
     * Valida los campos del formulario de login y, si son válidos,
     * llama al LoginViewModel para iniciar el proceso de autenticación
     * contra Firebase Auth.
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

        if (loginViewModel == null) {
            Toast.makeText(this,
                    "Error interno: ViewModel no inicializado",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Llamada al ViewModel: el resultado se manejará en los observers
        loginViewModel.login(email, password);
    }

    /**
     * Navega a la pantalla principal (catálogo) una vez que el login
     * fue exitoso.
     *
     * @param email Email del usuario logueado (opcional, se pasa como extra).
     */
    private void navigateToMain(String email) {
        Intent intent = new Intent(this, CatalogActivity.class);
        intent.putExtra("email", email);
        startActivity(intent);

        // Cerrar la pantalla de login para que no se pueda volver atrás fácilmente
        finish();
    }
}
