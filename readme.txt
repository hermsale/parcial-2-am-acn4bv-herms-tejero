---

## 🔐 Usuario de prueba

> Estas credenciales están pensadas para que el docente pueda probar rápidamente la app sin crear una cuenta nueva.

**Email:** `prueba@prueba.com`  
**Contraseña:** `targetgtr`

Con este usuario se puede:

- ✅ Iniciar sesión en la app Android.
- ✅ Navegar el catálogo y agregar productos al carrito.
- ✅ Modificar los datos del perfil en la vista **"Mis datos"**.
- ✅ Probar el cambio de contraseña  
  _(actualiza tanto Firebase Auth como el documento correspondiente en Firestore)._

---

## 3. Tecnologías y dependencias principales

### 3.1 Cliente Android (app móvil)

**Tecnologías base:**

- 🧱 **Android Studio** (IDE principal).
- ☕ **Java** como lenguaje para la lógica de la app.
- 🧩 **Patrón MVVM simple**  
  Activities + ViewModels + Repositories.
- 📦 **AndroidX**:
  - AppCompat
  - ConstraintLayout
  - Material Components

**Firebase:**

- 🔑 **Firebase Authentication**
  - Login
  - Signup
  - Cambio de contraseña
- 🗄️ **Cloud Firestore**  
  - Persistencia offline habilitada
  - Colecciones para productos, usuarios, etc.
- 📲 Integración mediante **SDK oficial de Firebase para Android**.

**Otras dependencias clave:**

- 🖼️ **Glide**  
  Carga y cacheo de imágenes desde URLs (por ejemplo, Firebase Storage).
- 🔁 **AndroidX Lifecycle**
  - ViewModel
  - LiveData  
  Para desacoplar la UI de la lógica de datos.
- 🎨 **Material Design Components**  
  Uso de `MaterialButton` y otros componentes para formularios, botones y pantallas.

---

### 3.2 Arquitectura general del código Android

La app sigue una arquitectura **MVVM simple**, organizada en paquetes con responsabilidades bien definidas:

- 🎛️ **Capa UI**
  - Activities (Login, Signup, Catálogo, Carrito, Checkout, Mis Datos, etc.)
  - Helpers de navegación (por ejemplo, `MenuDesplegableHelper` para el navbar).

- 📦 **Capa de modelo**
  - Clases de dominio:
    - Producto
    - Usuario
    - Carrito (CartItem / CartStore)
    - Otros modelos relacionados

- 💾 **Capa de datos / repositorios**
  - Acceso a:
    - Firebase Auth (login, registro, logout, cambio de contraseña)
    - Firestore (colecciones `productos`, `usuarios`, etc.)
    - Stores en memoria (por ejemplo, `CartStore`, `UserStore`)

- 🧠 **ViewModels por pantalla principal**
  - `LoginViewModel`
  - `SignupViewModel`
  - `CatalogViewModel`
  - `CartViewModel`
  - `ProfileViewModel`
  - `CheckoutViewModel`

Cada ViewModel expone **LiveData** para que la UI observe cambios sin acoplarse directamente a Firebase ni a los repositorios.

---
