package com.example.lamontana.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.lamontana.data.auth.AuthRepository;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;

// -----------------------------------------------------------------------------
// Archivo: LoginViewModel.java
// Paquete: com.example.lamontana.ui
//
// Responsabilidad:
//   - Actuar como capa intermedia (ViewModel) entre la UI de LoginActivity y
//     la capa de datos de autenticación (AuthRepository).
//   - Exponer estados observables (LiveData) para:
//       * loading: indica si se está procesando el login.
//       * loginSuccess: indica si el login fue exitoso.
//       * errorMessage: mensaje de error a mostrar en la UI.
//
// Alcance:
//   - Usado únicamente por LoginActivity dentro del flujo de autenticación.
//   - No conoce detalles de Android UI (Views, Toasts, Intents).
//   - No hace lecturas de Firestore; solo delega en AuthRepository (FirebaseAuth).
//
// Métodos presentes:
//   - getLoading(): LiveData<Boolean> para observar el estado de carga.
//   - getLoginSuccess(): LiveData<Boolean> para observar si el login fue exitoso.
//   - getErrorMessage(): LiveData<String> para observar mensajes de error.
//   - login(String email, String password):
//       * Llama a AuthRepository.login(...) y actualiza los LiveData según el resultado.
// -----------------------------------------------------------------------------
public class LoginViewModel extends ViewModel {

    // Repositorio de autenticación (envuelve FirebaseAuth)
    private final AuthRepository authRepository;

    // LiveData internos (mutables solo dentro del ViewModel)
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> loginSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public LoginViewModel() {
        // Obtiene la instancia singleton del repositorio
        authRepository = AuthRepository.getInstance();
    }

    // Getters públicos para exponer LiveData inmutables a la Activity
    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<Boolean> getLoginSuccess() {
        return loginSuccess;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * Inicia el proceso de login con email y contraseña.
     *
     * Notas:
     *   - Se asume que la Activity ya hizo validaciones básicas de campos vacíos.
     *   - Actualiza loading, loginSuccess y errorMessage según el resultado.
     *
     * @param email    Email del usuario.
     * @param password Contraseña del usuario.
     */
    public void login(String email, String password) {
        // Marca que comenzó el proceso de login
        loading.setValue(true);

        authRepository.login(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(Task<AuthResult> task) {
                        // Login finalizado: apagamos el loading
                        loading.setValue(false);

                        if (task.isSuccessful()) {
                            // Login OK
                            loginSuccess.setValue(true);
                            errorMessage.setValue(null);
                        } else {
                            // Login fallido: registramos mensaje de error legible
                            loginSuccess.setValue(false);

                            String message = "Error al iniciar sesión";
                            if (task.getException() != null &&
                                    task.getException().getMessage() != null) {
                                // Opcional: podríamos mapear mensajes de Firebase
                                message = task.getException().getMessage();
                            }

                            errorMessage.setValue(message);
                        }
                    }
                });
    }
}
