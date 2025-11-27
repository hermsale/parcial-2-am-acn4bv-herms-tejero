package com.example.lamontana.data.user;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

// -----------------------------------------------------------------------------
// Archivo: UserRepository.java
// Paquete: com.example.lamontana.data.user
//
// Responsabilidad:
//   - Encapsular el acceso a la colección "usuarios" de Firestore para la app
//     "La Montaña".
//   - Proveer métodos de alto nivel para crear/actualizar el perfil básico de
//     un usuario vinculado a Firebase Auth.
//
// Alcance:
//   - Usado principalmente por SignupViewModel (tras un registro exitoso en
//     Firebase Auth) para guardar el perfil del usuario en Firestore.
//   - También puede ser usado por otras pantallas (por ejemplo, Mis Datos /
//     ProfileActivity) para actualizar campos como apellido, teléfono,
//     dirección, etc.
//   - No conoce detalles de la UI ni de Activities/Fragments.
//
// Métodos presentes:
//   - getInstance():
//       Patrón singleton para obtener una única instancia del repositorio.
//   - createUserProfile(String uid, String nombre, String email):
//       Crea o actualiza el documento del usuario en la colección "usuarios"
//       usando el uid de Firebase Auth como ID de documento.
//   - updateUserProfile(String uid, String nombre, String apellido,
//                       String telefono, String direccion):
//       Actualiza campos de perfil (nombre, apellido, teléfono, dirección)
//       sobre el documento existente del usuario, sin pisar otros campos
//       como creadoEn, passwordHash, etc. gracias a SetOptions.merge().
//
// Notas:
//   - Firestore se obtiene vía FirebaseFirestore.getInstance(), ya configurado
//     previamente en LaMontanaApp con persistencia offline.
//   - Se usa SetOptions.merge() para no pisar campos existentes si el documento
//     ya existía.
// -----------------------------------------------------------------------------
public class UserRepository {

    private static final String COLLECTION_USUARIOS = "usuarios";

    // Singleton
    private static UserRepository instance;

    // Referencias a Firestore
    private final FirebaseFirestore firestore;
    private final CollectionReference usuariosRef;

    private UserRepository() {
        firestore = FirebaseFirestore.getInstance();
        usuariosRef = firestore.collection(COLLECTION_USUARIOS);
    }

    public static synchronized UserRepository getInstance() {
        if (instance == null) {
            instance = new UserRepository();
        }
        return instance;
    }

    /**
     * Crea o actualiza el perfil del usuario en la colección "usuarios".
     *
     * @param uid    UID del usuario (proveniente de FirebaseAuth.getCurrentUser().getUid()).
     * @param nombre Nombre completo del usuario.
     * @param email  Email del usuario.
     * @return Task<Void> para poder escuchar éxito/fracaso desde el ViewModel.
     */
    public Task<Void> createUserProfile(String uid, String nombre, String email) {
        // Mapa de datos a guardar. Podés agregar más campos según tu esquema real.
        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("nombre", nombre);
        data.put("email", email);

        // Usamos el uid como ID de documento para vincular 1:1 Auth ↔ Firestore.
        return usuariosRef.document(uid).set(data, SetOptions.merge());
    }

    /**
     * Actualiza campos de perfil del usuario en la colección "usuarios".
     *
     * No pisa otros campos existentes como:
     *   - creadoEn
     *   - passwordHash
     *   - cualquier otro campo ya guardado en el documento.
     *
     * Solo actualiza los campos:
     *   - nombre
     *   - apellido
     *   - telefono
     *   - direccion
     *
     * @param uid       UID del usuario (documento usuarios/{uid}).
     * @param nombre    Nombre del usuario (puede ser null para no modificarlo).
     * @param apellido  Apellido del usuario (puede ser null para no modificarlo).
     * @param telefono  Teléfono del usuario (puede ser null para no modificarlo).
     * @param direccion Dirección del usuario (puede ser null para no modificarlo).
     * @return Task<Void> para escuchar éxito o error en la capa superior (Activity/ViewModel).
     */
    public Task<Void> updateUserProfile(
            String uid,
            String nombre,
            String apellido,
            String telefono,
            String direccion
    ) {
        if (uid == null || uid.trim().isEmpty()) {
            throw new IllegalArgumentException("El uid del usuario no puede ser nulo ni vacío");
        }

        Map<String, Object> data = new HashMap<>();

        // Solo incluimos en el mapa los campos no nulos, para no sobreescribir con null.
        if (nombre != null) {
            data.put("nombre", nombre);
        }
        if (apellido != null) {
            data.put("apellido", apellido);
        }
        if (telefono != null) {
            data.put("telefono", telefono);
        }
        if (direccion != null) {
            data.put("direccion", direccion);
        }

        // Merge: actualiza solo los campos del mapa, preserva el resto del documento.
        return usuariosRef.document(uid).set(data, SetOptions.merge());
    }
}
