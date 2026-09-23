# Basic Layouts in Compose

https://developer.android.com/codelabs/jetpack-compose-layouts#0

Jetpack Compose-ko oinarrizko diseinuak (layouts) lantzeko proiektua.

## Konponketak eta Konfigurazioa

### Icons Liburutegiaren Inportazioa

`Icons.Default.Search` bezalako ikonoak erabili ahal izateko, proiektuaren konfigurazioan aldaketa hauek egin dira:

1.  **Mendekotasunak Version Catalog-ean (`libs.versions.toml`):**
    `material-icons-core` eta `material-icons-extended` liburutegiak gehitu dira:
    ```toml
    [libraries]
    androidx-compose-material-icons-core = { group = "androidx.compose.material", name = "material-icons-core" }
    androidx-compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
    ```

2.  **App-eko Build Script-a (`app/build.gradle.kts`):**
    Liburutegi berriak aplikatu dira:
    ```kotlin
    dependencies {
        implementation(libs.androidx.compose.material.icons.core)
        implementation(libs.androidx.compose.material.icons.extended)
    }
    ```

3.  **Inportazioak Kodean (`MainActivity.kt`):**
    Ikonoak erabiltzeko beharrezko `import` lerroak gehitu dira:
    ```kotlin
    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.filled.Search
    ```
