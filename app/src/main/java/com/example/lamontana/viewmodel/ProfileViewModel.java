package com.example.lamontana.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.lamontana.data.auth.AuthRepository;
import com.example.lamontana.data.user.UserRepository;
import com.example.lamontana.data.user.UserStore;
import com.google.firebase.auth.FirebaseUser;

/*
 * ============================================================
 * Archivo: ProfileViewModel.java
 * Paquete: com.example.lamontana.viewmodel
 * ------------------------------------------------------------
 * ¿De qué se encarga?
 *   - Actúa como capa intermedia (MVVM) entre ProfileActivity
 *     y las fuentes de datos de usuario (UserStore + Firestore).
 *   - Expone estados observables (LiveData) para:
 *       · loading: indica si se está guardando el perfil.
 *       · saveSuccess: indica si el guardado fue exitoso.
 *       · errorMessage: contiene mensajes de error a mostrar.
 *   - Orquesta la actualización del perfil:
 *       · Actualiza UserStore (copia en memoria).
 *       · Sincroniza con Firestore a través de UserRepository.updateUserProfile().
 *
 * Clases usadas:
 *   - UserStore: caché en memoria del usuario actual.
 *   - UserRepository: acceso a colección "usuarios" en Firestore.
 *   - AuthRepository: para obtener el usuario logueado (uid) si hace falta.
 *
 * Métodos públicos:
 *   - LiveData<Boolean> getLoading()
 *   - LiveData<Boolean> getSaveSuccess()
 *   - LiveData<String> getErrorMessage()
 *   - void saveProfile(String nombre, String apellido,
 *                      String telefono, String direccion)
 *       * Actualiza UserStore y sincroniza con Firestore.
 *
 * Notas:
 *   - No conoce detalles de la UI ni de Android Views.
 *   - La validación de campos (no vacíos, formatos, etc.)
 *     se hace en ProfileActivity antes de llamar a saveProfile().
 * ============================================================
 */
public class ProfileViewModel extends ViewModel {

    // Repositorios / stores
    private final UserRepository userRepository = UserRepository.getInstance();
    private final AuthRepository authRepository = AuthRepository.getInstance();

    // Estados observables
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> saveSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    // --- Getters de LiveData para que la Activity observe ---

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<Boolean> getSaveSuccess() {
        return saveSuccess;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * Guarda los cambios del perfil del usuario actual.
     *
     * Flujo:
     *  1. Obtiene el uid:
     *       - Preferentemente desde UserStore.uid.
     *       - Si está vacío, intenta desde AuthRepository.getCurrentUser().
     *  2. Actualiza UserStore en memoria.
     *  3. Envía los cambios a Firestore mediante UserRepository.updateUserProfile().
     *  4. Actualiza los LiveData (loading, saveSuccess, errorMessage)
     *     según el resultado.
     *
     * @param nombre   Nombre del usuario.
     * @param apellido Apellido del usuario.
     * @param telefono Teléfono de contacto.
     * @param direccion Dirección del usuario.
     */
    public void saveProfile(String nombre,
                            String apellido,
                            String telefono,
                            String direccion) {

        // Reset de estados
        loading.setValue(true);
        saveSuccess.setValue(false);
        errorMessage.setValue(null);

        // 1) Obtener uid del usuario actual
        UserStore userStore = UserStore.get();
        String uid = userStore.uid;

        if (uid == null || uid.trim().isEmpty()) {
            // Si por algún motivo el store está vacío, intentamos con Auth
            FirebaseUser currentUser = authRepository.getCurrentUser();
            if (currentUser != null) {
                uid = currentUser.getUid();
                userStore.uid = uid;
            }
        }

        if (uid == null || uid.trim().isEmpty()) {
            // No hay forma de identificar al usuario actual
            loading.setValue(false);
            errorMessage.setValue("No se pudo identificar al usuario actual. Vuelva a iniciar sesión.");
            return;
        }

        // 2) Actualizar UserStore en memoria
        userStore.nombre = (nombre != null) ? nombre : "";
        userStore.setOptionalData(apellido, telefono, direccion);

        // 3) Sincronizar con Firestore usando tu método real updateUserProfile(...)
        userRepository
                .updateUserProfile(
                        uid,
                        userStore.nombre,
                        userStore.apellido,
                        userStore.telefono,
                        userStore.direccion
                )
                .addOnSuccessListener(unused -> {
                    // 4) Éxito
                    loading.setValue(false);
                    saveSuccess.setValue(true);
                })
                .addOnFailureListener(e -> {
                    // 4) Error
                    loading.setValue(false);
                    String msg = (e != null && e.getMessage() != null)
                            ? e.getMessage()
                            : "Error desconocido al guardar el perfil.";
                    errorMessage.setValue("Error al guardar en la nube: " + msg);
                });
    }
}
