# Diplom (полевая медицина)

Android-приложение на Kotlin + Compose.

## Требования

- **Java 11 или новее** (JDK 17 рекомендуется). Android Gradle Plugin 8.x не поддерживает Java 8.

## Сборка

### Если сборка пишет «This build uses a Java 8 JVM»

1. Установите JDK 11 или 17, например:
   - [Eclipse Temurin (Adoptium)](https://adoptium.net/)
   - или [Microsoft Build of OpenJDK](https://www.microsoft.com/openjdk)

2. Укажите эту Java для Gradle.
   ```bat
   set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.x-hotspot
   .\gradlew.bat assembleDebug
   ```
   Подставьте свой путь к установленному JDK.

3. Соберите проект:
   ```bat
   .\gradlew.bat assembleDebug
   ```

APK будет в `app/build/outputs/apk/debug/`.
