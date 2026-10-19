# 🏃‍♂️ Strava Native Android App

Nativní Android aplikace vytvořená pomocí moderních postupů pro vývoj na platformě Android, která komunikuje s rozhraním **Strava v3 API**. Aplikace umožňuje uživatelům přihlášení přes OAuth2, prohlížení profilu sportovce, procházení seznamu aktivit, detailní sledování statistik a analýzu výikonu.

## 🚀 Hlavní Funkce

* **🔐 Autentizace přes OAuth 2.0**: Bezpečné přihlášení a autorizace pomocí rozhraní Strava API.

* **📊 Přehled Aktivit**: Zobrazení nedávných aktivit včetně vzdálenosti, času, převýšení a průměrné rychlosti či tempa.

* **👤 Profil Sportovce**: Zobrazení detailů uživatele, celkových statistik a dosažených úspěchů.

* **🗺️ Detail Aktivity**: Podrobný rozbor jednotlivých tréninků včetně mapy trasy, výškového profilu a dat o tepové frekvenci (pokud jsou k dispozici).

* **🌗 Čisté UI / UX**: Moderní vzhled podle standardů Material Design 3 s plynulými interakcemi a přizpůsobitelným rozvržením.

## 🛠️ Použité Technologie a Architektura

Aplikace je postavena na moderních architektonických vzorech pro Android a doporučených knihovnách ze sady Android Jetpack:

* **Jazyk**: [Kotlin](https://kotlinlang.org/)

* **Architektura**: MVVM (Model-View-ViewModel) + principy Clean Architecture

* **Uživatelské rozhraní (UI)**: Jetpack Compose / ViewBinding & XML

* **Asynchronní operace**: Kotlin Coroutines & `StateFlow` / `LiveData`

* **Síťová komunikace**: [Retrofit 2](https://square.github.io/retrofit/) + [Moshi](https://github.com/square/moshi) / [Gson](https://github.com/google/gson)

* **Dependency Injection**: [Hilt](https://dagger.dev/hilt/) / [Koin](https://insert-koin.io/)

* **Načítání obrázků**: [Coil](https://github.com/coil-kt/coil) / [Glide](https://github.com/bumptech/glide)

* **Lokální úložiště / Cache**: [Room Database](https://developer.android.com/training/data-storage/room) & `EncryptedSharedPreferences` / DataStore

## ⚙️ Instalace a Nastavení

Pro lokální spuštění projektu budete potřebovat **Účet vývojáře na Stravě (Strava Developer Account)** pro registraci aplikace a získání API klíčů.

### 1. Požadavky

* Android Studio Hedgehog (nebo novější)

* JDK 17

* Android SDK s minimální verzí API 24 (Android 7.0)

### 2. Registrace Strava API

1. Přihlaste se ke svému účtu Strava a přejděte do [Nastavení Strava API](https://www.strava.com/settings/api).

2. Zaregistrujte novou aplikaci:

   * **Název aplikace (Application Name)**: `Strava Native Android (Dev)`

   * **Kategorie (Category)**: `App` nebo `Visualizer`

   * **Autorizační callback doména (Authorization Callback Domain)**: `localhost` (nebo vlastní deep-link doména aplikace, např. `stravanative://redirect`)

3. Poznamenejte si vaše **Client ID** a **Client Secret**.

### 3. Klonování a Konfigurace Projektu

Klonujte repozitář:

```bash
git clone https://github.com/Vasik96/Strava-native-android.git
cd Strava-native-android
```

Přidejte vaše API údaje do souboru `local.properties` (který se nachází v kořenové složce projektu):

```properties
STRAVA_CLIENT_ID="VASE_CLIENT_ID"
STRAVA_CLIENT_SECRET="VAS_CLIENT_SECRET"
STRAVA_REDIRECT_URI="stravanative://redirect"
```

*Poznámka: Ujistěte se, že je soubor `local.properties` uveden v `.gitignore`, aby nedošlo ke commitnutí vašich tajných klíčů do repozitáře.*

## 📱 Snímky obrazovky (Screenshots)

| Přihlášení / OAuth | Přehled aktivit | Detail aktivity | Profil | 
| ----- | ----- | ----- | ----- | 
| *Doplnit obrázek* | *Doplnit obrázek* | *Doplnit obrázek* | *Doplnit obrázek* | 

## 🧪 Spuštění Testů

Spuštění jednotkových testů (Unit tests):

```bash
./gradlew test
```

Spuštění UI / Instrumentovaných testů:

```bash
./gradlew connectedAndroidTest
```

## 🤝 Příspěvky do projektu (Contributing)

Příspěvky, nahlášení chyb i návrhy na nové funkce jsou vítány!

1. Vytvořte Fork projektu

2. Vytvořte svou větev s funkcí (`git checkout -b feature/NovaFunkce`)

3. Uložte změny (`git commit -m 'Přidána nová funkce'`)

4. Odeslete změny do větve (`git checkout -b feature/NovaFunkce`)

5. Otevřete Pull Request

## 📄 Licence

Projekt je distribuován pod licencí MIT. Více informací naleznete v souboru `LICENSE`.
