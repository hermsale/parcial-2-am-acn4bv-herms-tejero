package com.example.lamontana.data.user;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
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
//   - Proveer métodos de alto nivel para crear y actualizar el perfil de un
//     usuario vinculado a Firebase Auth, respetando el esquema esperado de la
//     colección:
//
//       usuarios: [
//         {
//           id:           string (usamos el uid de FirebaseAuth)
//           creadoEn:     timestamp (FieldValue.serverTimestamp al crear)
//           actualizadoEn:timestamp (FieldValue.serverTimestamp al crear/actualizar)
//           nombre:       string
//           apellido:     string
//           email:        string
//           telefono:     string
//           direccion:    string
//           passwordHash: string (campo de compatibilidad con el esquema legado;
//                                 la autenticación real la maneja Firebase Auth)
//         }
//       ]
//
// Alcance:
//   - Usado principalmente por SignupViewModel (tras un registro exitoso en
//     Firebase Auth) para guardar el perfil básico del usuario en Firestore.
//   - Usado por pantallas como ProfileActivity para actualizar datos de perfil
//     mediante updateUserProfile(...) sin preocuparse por campos de auditoría.
//
// Métodos presentes:
//   - getInstance():
//       Patrón singleton para obtener una única instancia del repositorio.
//   - createUserProfile(String uid, String nombre, String email):
//       Crea o inicializa el documento usuarios/{uid} con todos los campos del
//       esquema y los timestamps de auditoría.
//   - updateUserProfile(String uid, String nombre, String apellido,
//                       String telefono, String direccion):
//       Actualiza sólo los campos de perfil indicados y refresca actualizadoEn.
//       No pisa creadoEn ni passwordHash.
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
     * Crea o inicializa el perfil del usuario en la colección "usuarios"
     * respetando el esquema completo. Si el documento ya existe, se hace
     * merge de los campos, de modo que no se pierdan datos previos.
     *
     * Campos que se setean:
     *   - id:            uid de FirebaseAuth (string)
     *   - nombre:        nombre recibido
     *   - apellido:      string vacío por defecto (se completará en Mis Datos)
     *   - email:         email recibido
     *   - telefono:      string vacío por defecto
     *   - direccion:     string vacío por defecto
     *   - passwordHash:  string vacío (la autenticación real la maneja Auth)
     *   - creadoEn:      FieldValue.serverTimestamp() (solo la primera vez)
     *   - actualizadoEn: FieldValue.serverTimestamp()
     *
     * @param uid    UID del usuario (FirebaseAuth.getCurrentUser().getUid()).
     * @param nombre Nombre completo del usuario.
     * @param email  Email del usuario.
     * @return Task<Void> para poder escuchar éxito/fracaso desde el ViewModel.
     */
    public Task<Void> createUserProfile(String uid, String nombre, String email) {
        if (uid == null || uid.trim().isEmpty()) {
            throw new IllegalArgumentException("El uid del usuario no puede ser nulo ni vacío");
        }

        Map<String, Object> data = new HashMap<>();

        // Identificador lógico dentro del documento: usamos el uid
        data.put("id", uid);

        // Datos de perfil básicos
        data.put("nombre", nombre != null ? nombre : "");
        data.put("apellido", "");          // se completará luego en Mis Datos
        data.put("email", email != null ? email : "");
        data.put("telefono", "");          // sin teléfono al momento de signup
        data.put("direccion", "");         // sin dirección al momento de signup

        // Campo de compatibilidad con el esquema legado.
        // La autenticación real la maneja FirebaseAuth, por lo que no guardamos
        // aquí un hash real de contraseña.
        data.put("passwordHash", "");

        // Campos de auditoría: timestamps de servidor
        data.put("creadoEn", FieldValue.serverTimestamp());
        data.put("actualizadoEn", FieldValue.serverTimestamp());

        // Usamos el uid como ID de documento para vincular 1:1 Auth ↔ Firestore.
        return usuariosRef.document(uid).set(data, SetOptions.merge());
    }

    /**
     * Actualiza campos de perfil del usuario en la colección "usuarios".
     *
     * No pisa otros campos existentes como:
     *   - creadoEn
     *   - passwordHash
     *   - id
     *
     * Solo actualiza los campos:
     *   - nombre (si no es null)
     *   - apellido (si no es null)
     *   - telefono (si no es null)
     *   - direccion (si no es null)
     *   - actualizadoEn: siempre se refresca con serverTimestamp.
     *
     * @param uid       UID del usuario (documento usuarios/{uid}).
     * @param nombre    Nombre del usuario (puede ser null para no modificarlo).
     * @param apellido  Apellido del usuario (puede ser null para no modificarlo).
     * @param telefono  Teléfono del usuario (puede ser null para no modificarlo).
     * @param direccion Dirección del usuario (puede ser null para no modificarlo).
     * @return Task<Void> para escuchar éxito o error en la capa superior.
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

        // Campo de auditoría: cada actualización refresca actualizadoEn.
        data.put("actualizadoEn", FieldValue.serverTimestamp());

        // Merge: actualiza solo los campos del mapa, preserva el resto del documento.
        return usuariosRef.document(uid).set(data, SetOptions.merge());
    }
}
