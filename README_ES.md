# Brain Tumor LAB 15 — Android nativo con Kotlin (VS Code)

**NO es Flutter.** Proyecto Android convencional (`MainActivity.kt`, XML, Gradle Kotlin DSL) y puede abrirse en Android Studio si el docente lo requiere. Fue preparado para compilar por consola desde VS Code, sin instalar Android Studio.

## Qué viene incluido

- El **mismo** archivo `brain_tumor_model_fp16.tflite` de tu ZIP original, sin reentrenar ni reconvertir.
- `labels.txt`, `model_config.json`, métricas Keras y 4 imágenes MRI de ejemplo.
- Interfaz Kotlin que permite abrir una imagen del teléfono, predecir las 4 clases y ejecutar las 4 imágenes de prueba.
- TensorFlow Lite con `select-tf-ops` / Flex, necesario para las operaciones `Flex*` de este modelo.

## Preparación en Windows

1. Descomprime la carpeta y ábrela en **VS Code** con Archivo > Abrir carpeta.
2. Comprueba `java -version` (JDK 17 recomendado) y `adb devices` (dispositivo autorizado por USB).
3. Debes tener Android SDK API 35 instalado. El script intenta localizarlo en `%LOCALAPPDATA%\Android\Sdk`, `ANDROID_SDK_ROOT` o `ANDROID_HOME`.
4. Ejecuta en PowerShell, dentro de la carpeta:

```powershell
.\INSTALAR_EN_CELULAR.ps1
```

Si PowerShell bloquea scripts, también puedes ejecutar:

```powershell
.\gradlew.bat :app:installDebug
```

También es posible solo compilar el APK:

```powershell
.\gradlew.bat :app:assembleDebug
```

Ubicación del APK: `app\build\outputs\apk\debug\app-debug.apk`.

**Nota:** `gradlew.bat` es un *bootstrap* PowerShell propio (no el wrapper Gradle oficial), porque no se incluye el `gradle-wrapper.jar` binario. Descarga una vez Gradle 8.9 de `services.gradle.org`, valida su SHA-256 oficial y lo deja en `.tools` dentro del proyecto. Descargará además dependencias Android/TF Lite desde Google y Maven Central. Gradle 8.9 se usa con Android Gradle Plugin 8.7.3 y Kotlin 2.1.20.

## Modelo e inferencia

- Archivo: `app/src/main/assets/brain_tumor_model_fp16.tflite`.
- Entrada `[1,260,260,3]` RGB float32 con rango 0..255 (sin dividir entre 255).
- Salida `[1,4]`: glioma, meningioma, notumor, pituitary.
- Flex deps: `org.tensorflow:tensorflow-lite:2.16.1` y `org.tensorflow:tensorflow-lite-select-tf-ops:2.16.1`.

**Importante:** en el ZIP original, el modelo Keras alcanzó 93.375 % de exactitud en TEST, pero su archivo `.tflite` no se ejecutó en la prueba de Colab por ausencia de Flex. En Android la biblioteca Flex puede permitir la inferencia; la **ejecución en tu teléfono no ha sido validada aquí**. Si aparece un error de carga o inferencia, copia el mensaje exacto de la app o `adb logcat`, y podremos corregir la compatibilidad. Puede que el APK sea grande por el runtime Flex. Es exclusivamente una demostración académica, no un dispositivo médico.

## Para la práctica

- Captura de una predicción individual en el teléfono.
- Captura de la pantalla **Run all 4 test images**, donde aparecen las cuatro clases.
- Conservar el notebook `.ipynb` de Colab para evidenciar métricas, matriz de confusión y accuracy.
- Subir el proyecto Android nativo a GitHub. La consigna menciona Android Studio como IDE, pero la estructura es equivalente; confirma con el docente que acepta VS Code y captura desde el teléfono en lugar de simulador.
