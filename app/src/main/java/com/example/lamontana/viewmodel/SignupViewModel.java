package com.example.lamontana.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.lamontana.data.auth.AuthRepository;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;

// -----------------------------------------------------------------------------
// Archivo: SignupViewModel.java
// Paquete: com.example.lamontana.viewmodel
//
// Responsabilidad:
//   - Actuar como capa intermedia (ViewModel) entre la UI de SignupActivity y
//     la capa de datos de autenticación (AuthRepository).
//   - Exponer estados observables (LiveData) para:
//       * loading: indica si se está procesando el registro.
//       * signupSuccess: indica si el registro fue exitoso.
//       * errorMessage: mensaje de error a mostrar en la UI.
//
// Alcance:
//   - Usado únicamente por SignupActivity dentro del flujo de registro.
//   - No conoce detalles de la UI (Views, Toasts, Intents).
//   - No realiza lecturas/escrituras en Firestore; solo delega en AuthRepository
//     que usa FirebaseAuth para crear el usuario.
//
// Métodos presentes:
//   - getLoading(): LiveData<Boolean> para observar el estado de carga.
//   - getSignupSuccess(): LiveData<Boolean> para observar si el registro fue exitoso.
//   - getErrorMessage(): LiveData<String> para observar mensajes de error.
//   - signup(String email, String password):
//       * Llama a AuthRepository.signup(...) y actualiza los LiveData según el resultado.
// -----------------------------------------------------------------------------
public class SignupViewModel extends ViewModel {

    // Repositorio que envuelve FirebaseAuth
    private final AuthRepository authRepository;

    // LiveData internos (mutables solo dentro del ViewModel)
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> signupSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public SignupViewModel() {
        authRepository = AuthRepository.getInstance();
    }

    // Getters públicos para exponer LiveData a la Activity
    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<Boolean> getSignupSuccess() {
        return signupSuccess;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * Inicia el proceso de registro de usuario con email y contraseña.
     *
     * Notas:
     *   - Se asume que la Activity ya hizo validaciones de campos (nombre, email,
     *     contraseña, confirmación).
     *   - Este método se enfoca solo en llamar a Firebase Auth para crear el usuario.
     *
     * @param email    Email del usuario a registrar.
     * @param password Contraseña del usuario.
     */
    public void signup(String email, String password) {
        // Marcamos que el registro comenzó
        loading.setValue(true);

        authRepository.signup(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(Task<AuthResult> task) {
                        // Termina el proceso: apagamos el loading
                        loading.setValue(false);

                        if (task.isSuccessful()) {
                            // Registro OK (Firebase Auth crea y loguea al usuario)
                            signupSuccess.setValue(true);
                            errorMessage.setValue(null);
                        } else {
                            // Registro fallido: seteamos error
                            signupSuccess.setValue(false);

                            String message = "Error al crear la cuenta";
                            if (task.getException() != null &&
                                    task.getException().getMessage() != null) {
                                message = task.getException().getMessage();
                            }

                            errorMessage.setValue(message);
                        }
                    }
                });
    }
}
