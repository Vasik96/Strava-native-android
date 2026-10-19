# 🍽️ Strava.cz Native Android App

Nativní Android aplikace vytvořená pro snadný přístup k objednávkovému systému školního stravování **[Strava.cz](https://www.strava.cz)**. Aplikace umožňuje uživatelům pohodlně spravovat své jídelníčky, přihlašovat a odhlašovat obědy a sledovat stav stravovacího účtu. (hele něco z tohohle README napsal AI takže něco z toho není pravda)

## 🚀 Hlavní Funkce

* **🔐 Přihlášení k jídelně**: Bezpečné přihlášení pomocí čísla zařízení/jídelny, uživatelského jména a hesla.
* **📅 Prohlížení Jídelníčku**: Přehledný týdenní a měsíční rozpis jídel včetně alergenů, gramáže a nutričních hodnot.
* **🍱 Objednávání a Odhlašování Stravy**: Rychlá volba z nabízených druhů jídel (Oběd 1, Oběd 2, Svačina atd.) a možnost odhlášení na konkrétní dny.
* **💰 Stav Účtu**: Zobrazení aktuálního kreditu.
* **🔔 Upozornění**: Notifikace když někdo změní oběd mimo aplikaci.

## 🛠️ Použité Technologie a Architektura

Aplikace využívá moderní přístupy k vývoji nativních aplikací pro Android:

* **Jazyk**: [Kotlin](https://kotlinlang.org/)
* **Architektura**: MVVM (Model-View-ViewModel) / MVI
* **Uživatelské rozhraní (UI)**: [Jetpack Compose](https://developer.android.com/jetpack/compose) s Material Design 3
* **Asynchronní operace**: Kotlin Coroutines & `StateFlow`
* **Síťová komunikace**: [Retrofit 2](https://square.github.io/retrofit/) + [Ktor Client](https://ktor.io/) / [Moshi](https://github.com/square/moshi) pro parsing dat
* **Web Scraping / API**: Zpracování a parsing odpovědí ze servery Strava.cz (HTML/JSON)
* **Dependency Injection**: [Hilt](https://dagger.dev/hilt/)
* **Lokální cache**: [Room Database](https://developer.android.com/training/data-storage/room) & `EncryptedSharedPreferences` pro bezpečné uložení přihlašovacích údajů

## ⚙️ Instalace a Spuštění

### Požadavky
* Android Studio Ladybug / Hedgehog (nebo novější)
* JDK 17
* Android SDK (min. API level 24 – Android 7.0)

### Postup

1. **Klonování repozitáře**:
   ```bash
   git clone https://github.com/Vasik96/Strava-native-android.git
   cd Strava-native-android
   ```

2. **Otevření v Android Studiu**:
   * Otevřete Android Studio a zvolte *Open File or Project*.
   * Vyberte složku s klonovaným projektem.

3. **Sestavení a spuštění**:
   * Počkejte na dokončení synchronizace Gradle.
   * Připojte fyzické zařízení s povoleným vývojářským režimem nebo spusťte Android Emulator.
   * Stiskněte tlačítko **Run** (`Shift + F10`).

## 📱 Snímky obrazovky (Screenshots)

| Přihlášení | Jídelníček & Objednávky | Detail jídla | Stav účtu |
| :-: | :-: | :-: | :-: |
| *Doplnit obrázek* | *Doplnit obrázek* | *Doplnit obrázek* | *Doplnit obrázek* |

## 🧪 Testování

Spuštění jednotkových testů:
```bash
./gradlew test
```

Spuštění instrumentovaných UI testů:
```bash
./gradlew connectedAndroidTest
```

## 🤝 Příspěvky do projektu (Contributing)

Pokud chcete do projektu přispět:
1. Vytvořte Fork projektu.
2. Vytvořte svou větev (`git checkout -b feature/NovaFunkce`).
3. Uložte změny (`git commit -m 'Přidána nová funkce'`).
4. Odeslete změny (`git push origin feature/NovaFunkce`).
5. Otevřete Pull Request.

## 📄 Licence

Tento projekt je šířen pod licencí MIT. Podrobnosti najdete v souboru `LICENSE`.
