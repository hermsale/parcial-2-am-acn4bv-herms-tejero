package com.example.lamontana;
// -----------------------------------------------------------------------------
// Archivo: LaMontanaApp.java
// Responsabilidad:
//   - Clase Application principal de la app "La Montaña".
//   - Inicializar Firebase en el arranque de la aplicación.
//   - Configurar Firebase Firestore con persistencia offline habilitada.
// Alcance:
//   - Se ejecuta una sola vez cuando se abre la app.
//   - Cualquier Activity/Repository podrá usar FirebaseAuth y FirebaseFirestore
//     ya inicializados.
// Métodos presentes:
//   - onCreate(): punto de entrada de la Application, inicializa Firebase y
//                 configura Firestore.
// -----------------------------------------------------------------------------
import android.app.Application;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

public class LaMontanaApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Inicializa Firebase (se basa en google-services.json del módulo app)
        FirebaseApp.initializeApp(this);

        // Obtiene la instancia de Firestore
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        // Configura Firestore con persistencia offline habilitada
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true) // cache offline habilitado
                .build();

        firestore.setFirestoreSettings(settings);
    }
}
