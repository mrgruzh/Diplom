// Top-level build file where you can add configuration options common to all sub-projects/modules.
import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
if (JavaVersion.current() < JavaVersion.VERSION_11) {
    throw GradleException(
        "Требуется Java 11 или новее. Сейчас: ${JavaVersion.current()}. " +
        "Установите JDK 11+ и задайте JAVA_HOME или в gradle.properties укажите org.gradle.java.home"
    )
}
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}