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

import com.example.lamontana.MainActivity;
import com.example.lamontana.R;
import com.example.lamontana.viewmodel.SignupViewModel;
/* -----------------------------------------------------------------------------
  Archivo: SignupActivity.java
  Responsabilidad:
    - Mostrar el formulario de creación de usuario.
    - Validar los datos ingresados por el usuario para crear una nueva cuenta.
    - Delegar el registro real a SignupViewModel (Firebase Auth + Firestore).
    - Navegar nuevamente al Login o a la pantalla principal si el registro es válido.

  Alcance:
    - Forma parte del flujo de autenticación inicial de la app.
    - Usa el patrón MVVM:
        * UI (SignupActivity)
        * ViewModel (SignupViewModel)
        * Repositorios:
            - AuthRepository (FirebaseAuth)
            - UserRepository (Firestore, colección "usuarios")

  Lista de métodos públicos:
    - onCreate(Bundle savedInstanceState)
        * Inicializa la UI, enlaza vistas, listeners y el ViewModel.

  Lista de métodos privados:
    - setupViews()
        * Vincula las vistas XML con variables.
    - setupListeners()
        * Configura botones y acciones.
    - setupViewModel()
        * Obtiene una instancia de SignupViewModel.
    - observeViewModel()
        * Se suscribe a los LiveData de SignupViewModel (loading, signupSuccess, errorMessage).
    - attemptSignup()
        * Valida los campos del formulario y llama a
          signupViewModel.signup(name, email, password).
    - navigateAfterSignup(String name, String email)
        * Navega a la pantalla principal tras un registro exitoso.

  Notas:
    - La lógica de autenticación y guardado de perfil se encuentra en SignupViewModel,
      AuthRepository y UserRepository, no dentro de la Activity.
----------------------------------------------------------------------------- */

public class SignupActivity extends AppCompatActivity {

    // -------------------------------------------------------------------------
    // Atributos de la UI
    // -------------------------------------------------------------------------
    private EditText etName;
    private EditText etEmail;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private Button btnCreateAccount;
    // Botón para volver a la pantalla de Login
    private Button btnGoToLogin;

    // -------------------------------------------------------------------------
    // ViewModel
    // -------------------------------------------------------------------------
    private SignupViewModel signupViewModel;

    // -------------------------------------------------------------------------
    // Ciclo de vida
    // -------------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        setupViews();
        setupListeners();
        setupViewModel();
        observeViewModel();
    }

    // -------------------------------------------------------------------------
    // Inicialización de vistas
    // -------------------------------------------------------------------------

    private void setupViews() {
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etSignupEmail);
        etPassword = findViewById(R.id.etSignupPassword);
        etConfirmPassword = findViewById(R.id.etSignupConfirmPassword);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        btnGoToLogin = findViewById(R.id.btnGoToLogin);
    }

    private void setupListeners() {
        if (btnCreateAccount != null) {
            btnCreateAccount.setOnClickListener(v -> attemptSignup());
        }

        // Listener para ir al Login
        if (btnGoToLogin != null) {
            btnGoToLogin.setOnClickListener(view -> {
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
                finish();
            });
        }
    }

    private void setupViewModel() {
        signupViewModel = new ViewModelProvider(this).get(SignupViewModel.class);
    }

    private void observeViewModel() {
        if (signupViewModel == null) return;

        // Observa el estado de carga
        signupViewModel.getLoading().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isLoading) {
                if (isLoading == null) return;

                if (btnCreateAccount != null) {
                    btnCreateAccount.setEnabled(!isLoading);
                    btnCreateAccount.setText(isLoading ? "Creando cuenta..." : "Crear cuenta");
                }

                if (btnGoToLogin != null) {
                    btnGoToLogin.setEnabled(!isLoading);
                }
            }
        });

        // Observa si el registro fue exitoso (Auth + Firestore)
        signupViewModel.getSignupSuccess().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean success) {
                if (success != null && success) {
                    String name = etName != null
                            ? etName.getText().toString().trim()
                            : "";
                    String email = etEmail != null
                            ? etEmail.getText().toString().trim()
                            : "";

                    Toast.makeText(SignupActivity.this,
                            "Cuenta creada con éxito", Toast.LENGTH_SHORT).show();

                    navigateAfterSignup(name, email);
                }
            }
        });

        // Observa mensajes de error
        signupViewModel.getErrorMessage().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String message) {
                if (message != null && !message.isEmpty()) {
                    Toast.makeText(SignupActivity.this,
                            message, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    // -------------------------------------------------------------------------
    // Lógica de negocio (validación + llamada al ViewModel)
    // -------------------------------------------------------------------------

    /**
     * Valida los campos del formulario y, si son válidos,
     * llama al SignupViewModel para registrar el usuario en
     * Firebase Auth y guardar su perfil en Firestore.
     */
    private void attemptSignup() {
        String name = etName != null ? etName.getText().toString().trim() : "";
        String email = etEmail != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword != null ? etPassword.getText().toString().trim() : "";
        String confirmPassword = etConfirmPassword != null ? etConfirmPassword.getText().toString().trim() : "";

        // Validaciones básicas -------------------------------------------------
        if (TextUtils.isEmpty(name)) {
            if (etName != null) {
                etName.setError("Ingrese su nombre completo");
                etName.requestFocus();
            }
            return;
        }

        if (TextUtils.isEmpty(email)) {
            if (etEmail != null) {
                etEmail.setError("Ingrese un email válido");
                etEmail.requestFocus();
            }
            return;
        }

        if (TextUtils.isEmpty(password)) {
            if (etPassword != null) {
                etPassword.setError("Ingrese una contraseña");
                etPassword.requestFocus();
            }
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            if (etConfirmPassword != null) {
                etConfirmPassword.setError("Confirme su contraseña");
                etConfirmPassword.requestFocus();
            }
            return;
        }

        if (!password.equals(confirmPassword)) {
            if (etConfirmPassword != null) {
                etConfirmPassword.setError("Las contraseñas no coinciden");
                etConfirmPassword.requestFocus();
            }
            return;
        }

        if (signupViewModel == null) {
            Toast.makeText(this,
                    "Error interno: ViewModel no inicializado",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Llamada al ViewModel: ahora pasa name, email y password
        signupViewModel.signup(name, email, password);
    }

    /**
     * Navega a la pantalla principal (MainActivity) tras un registro exitoso.
     */
    private void navigateAfterSignup(String name, String email) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("name", name);
        intent.putExtra("email", email);
        startActivity(intent);
        finish(); // Cierra actividad de registro
    }
}
