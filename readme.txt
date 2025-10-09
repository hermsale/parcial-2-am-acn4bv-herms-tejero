Clonar rama master esta funcional para las vistas catalogo y carrito

## 🧰 Entorno y versiones utilizadas

Este proyecto fue desarrollado y probado en el siguiente entorno de compilación:

| Componente | Versión / Configuración | Dónde se define / observa |
|-------------|-------------------------|----------------------------|
| **Android Studio** | 🧩 Narwhal 3 Feature Drop · **2025.1.3** | Entorno principal de desarrollo |
| **Gradle** | ⚙️ **8.13** | `gradle/wrapper/gradle-wrapper.properties` → `distributionUrl=...gradle-8.13-bin.zip` |
| **Android Gradle Plugin (AGP)** | 🧱 **8.13.0** | `gradle/libs.versions.toml` → `agp = "8.13.0"` |
| **Gradle JDK** | ☕ **JetBrains Runtime 21.0.7 (JBR 21)** | Configuración de IDE → *File → Settings → Build, Execution, Deployment → Build Tools → Gradle* |
| **Nivel de lenguaje Java (fuente/bytecode)** | 💻 **Java 11** | `app/build.gradle` → `compileOptions { sourceCompatibility JavaVersion.VERSION_11 }` |
| **compileSdk** | 📱 **36** | `app/build.gradle` |
| **targetSdk** | 🎯 **36** | `app/build.gradle` |
| **minSdk** | 📉 **24** | `app/build.gradle` |

> ⚠️ **Nota:** aunque el entorno de ejecución use JBR 21, el proyecto compila con nivel de lenguaje **Java 11** para asegurar compatibilidad con versiones más amplias del SDK de Android.

