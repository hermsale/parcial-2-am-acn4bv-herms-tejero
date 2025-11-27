package com.example.lamontana.ui.profile;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.lamontana.R;
import com.example.lamontana.data.user.UserStore;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

/*
 * ============================================================
 * Archivo: ModificarContrasenaHelper.java
 * Paquete: com.example.lamontana.ui.profile
 * ------------------------------------------------------------
 * ¿De qué se encarga este archivo?
 *   - Encapsula todo el flujo de cambio de contraseña del usuario
 *     para la app "La Montaña".
 *   - Muestra un diálogo donde el usuario ingresa:
 *       · Contraseña actual.
 *       · Nueva contraseña.
 *       · Confirmación de la nueva contraseña.
 *   - Valida que:
 *       · Ninguno de los campos esté vacío.
 *       · La nueva contraseña cumpla una longitud mínima
 *         (por ejemplo, >= 6 caracteres).
 *       · La nueva contraseña y su confirmación sean iguales.
 *   - Reautentica al usuario en Firebase Auth usando la
 *     contraseña actual.
 *   - Si la reautenticación es correcta:
 *       · Actualiza la contraseña en Firebase Auth.
 *       · Impacta también en Firestore:
 *           - Actualiza el campo "passwordHash" del documento
 *             usuarios/{uid}.
 *           - Actualiza el campo "actualizadoEn" con
 *             FieldValue.serverTimestamp().
 *
 * Alcance:
 *   - Es invocado desde ProfileActivity cuando el usuario toca
 *     el botón "Cambiar contraseña".
 *   - No mantiene estado interno; expone métodos estáticos de
 *     utilidad.
 *
 * Métodos presentes:
 *   - mostrarDialogoCambioContrasena(Activity activity):
 *       · Construye y muestra el AlertDialog con los 3 campos
 *         de contraseña.
 *       · Ejecuta toda la lógica de:
 *           - Validación de campos.
 *           - Reautenticación en Firebase Auth.
 *           - Actualización de contraseña en Auth.
 *           - Actualización de passwordHash + actualizadoEn en
 *             Firestore.
 * ============================================================
 */

public class ModificarContrasenaHelper {

    /**
     * Muestra el diálogo para cambiar la contraseña del usuario actual.
     *
     * Flujo:
     *  1) Muestra 3 EditText:
     *       - Contraseña actual.
     *       - Nueva contraseña.
     *       - Confirmar nueva contraseña.
     *  2) Valida:
     *       - Que ninguno esté vacío.
     *       - Que la nueva contraseña tenga longitud mínima.
     *       - Que la nueva y la confirmación sean iguales.
     *  3) Reautentica al usuario usando EmailAuthProvider.
     *  4) Actualiza la contraseña en Firebase Auth.
     *  5) Actualiza el campo "passwordHash" y "actualizadoEn"
     *     en la colección "usuarios" de Firestore.
     *
     * @param activity Activity desde la cual se invoca el diálogo.
     */
    public static void mostrarDialogoCambioContrasena(Activity activity) {
        if (activity == null) return;

        // Crear un contenedor vertical para los EditText
        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = activity.getResources().getDimensionPixelSize(R.dimen.spacing_md);
        layout.setPadding(padding, padding, padding, padding);

        // Campo para contraseña actual
        final EditText etCurrentPassword = new EditText(activity);
        etCurrentPassword.setHint("Contraseña actual");
        etCurrentPassword.setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
        );
        layout.addView(
                etCurrentPassword,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        // Campo para nueva contraseña
        final EditText etNewPassword = new EditText(activity);
        etNewPassword.setHint("Nueva contraseña");
        etNewPassword.setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
        );
        layout.addView(
                etNewPassword,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        // Campo para confirmar nueva contraseña
        final EditText etConfirmNewPassword = new EditText(activity);
        etConfirmNewPassword.setHint("Repetir nueva contraseña");
        etConfirmNewPassword.setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
        );
        layout.addView(
                etConfirmNewPassword,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        new AlertDialog.Builder(activity)
                .setTitle("Cambiar contraseña")
                .setView(layout)
                .setPositiveButton("Cambiar", (dialog, which) -> {
                    String currentPassword = etCurrentPassword.getText().toString().trim();
                    String newPassword = etNewPassword.getText().toString().trim();
                    String confirmNewPassword = etConfirmNewPassword.getText().toString().trim();

                    // Validaciones básicas
                    if (currentPassword.isEmpty()) {
                        Toast.makeText(
                                activity,
                                "Ingrese su contraseña actual",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    if (newPassword.isEmpty()) {
                        Toast.makeText(
                                activity,
                                "Ingrese una nueva contraseña",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    if (confirmNewPassword.isEmpty()) {
                        Toast.makeText(
                                activity,
                                "Repita la nueva contraseña",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    // Validar solo cantidad de caracteres (por ejemplo mínimo 6)
                    if (newPassword.length() < 6) {
                        Toast.makeText(
                                activity,
                                "La nueva contraseña debe tener al menos 6 caracteres",
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    // Validar que las dos nuevas contraseñas coincidan
                    if (!newPassword.equals(confirmNewPassword)) {
                        Toast.makeText(
                                activity,
                                "Las contraseñas nuevas no coinciden",
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    // Obtener usuario actual desde FirebaseAuth
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user == null) {
                        Toast.makeText(
                                activity,
                                "No hay usuario logueado. Vuelva a iniciar sesión.",
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    // Obtener email (desde Auth o desde UserStore como backup)
                    String email = user.getEmail();
                    if (email == null || email.trim().isEmpty()) {
                        email = UserStore.get().email;
                    }

                    if (email == null || email.trim().isEmpty()) {
                        Toast.makeText(
                                activity,
                                "No se pudo determinar el email del usuario.",
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    // Crear credencial para reautenticación
                    AuthCredential credential =
                            EmailAuthProvider.getCredential(email, currentPassword);

                    // Reautenticar primero
                    user.reauthenticate(credential)
                            .addOnSuccessListener(authResult -> {
                                // Si la contraseña actual es correcta, actualizamos la contraseña en Auth
                                user.updatePassword(newPassword)
                                        .addOnSuccessListener(unused -> {
                                            // Ahora impactamos también en la base de datos (Firestore)
                                            String uid = UserStore.get().uid;
                                            if (uid == null || uid.trim().isEmpty()) {
                                                // Si por alguna razón no tenemos uid, avisamos
                                                Toast.makeText(
                                                        activity,
                                                        "Contraseña actualizada en Auth, pero no se pudo actualizar en Firestore (UID vacío).",
                                                        Toast.LENGTH_LONG
                                                ).show();
                                            } else {
                                                FirebaseFirestore.getInstance()
                                                        .collection("usuarios")
                                                        .document(uid)
                                                        .update(
                                                                "passwordHash", newPassword,
                                                                "actualizadoEn", FieldValue.serverTimestamp()
                                                        )
                                                        .addOnSuccessListener(unusedDb -> Toast.makeText(
                                                                activity,
                                                                "Contraseña actualizada correctamente",
                                                                Toast.LENGTH_SHORT
                                                        ).show())
                                                        .addOnFailureListener(e -> Toast.makeText(
                                                                activity,
                                                                "Contraseña actualizada en Auth, pero falló al actualizar en Firestore: " + e.getMessage(),
                                                                Toast.LENGTH_LONG
                                                        ).show());
                                            }
                                        })
                                        .addOnFailureListener(e -> Toast.makeText(
                                                activity,
                                                "Error al actualizar la contraseña en Auth: " + e.getMessage(),
                                                Toast.LENGTH_LONG
                                        ).show());
                            })
                            .addOnFailureListener(e -> Toast.makeText(
                                    activity,
                                    "Contraseña actual incorrecta o error de reautenticación.",
                                    Toast.LENGTH_LONG
                            ).show());
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

}
