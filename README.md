# TouchGuard — blokada przypadkowych dotknięć ekranu

Prosta, lokalna aplikacja na Androida, bez internetu, kont i reklam. Projekt w Java / Android SDK (bez zewnętrznych bibliotek), zbudowany z myślą o własnym telefonie i opublikowaniu kodu na GitHubie.

## Jak to działa

1. Zainstaluj i uruchom TouchGuard; przyznaj uprawnienie **Wyświetlanie nad innymi aplikacjami**.
2. Opcjonalnie przyznaj powiadomienia (aby mieć przycisk „Odblokuj” w powiadomieniu).
3. Wybierz długość blokady na suwaku: **15 sekund – 30 minut, co 15 sekund**. Domyślnie **60 sekund**.
4. Naciśnij **Włącz blokadę**, lub dodaj kafelek **Blokada dotyku** do szybkich ustawień i włączaj z poziomu innych aplikacji.
5. Po uruchomieniu pojawi się całkowicie przezroczysta nakładka pochłaniająca dotknięcia. Mała kłódka wyświetla czas pozostały do automatycznego odblokowania.
6. Odblokowanie: **przytrzymaj małą kłódkę na ekranie przez 3 sekundy**, użyj kafelka lub przycisku **Odblokuj** w powiadomieniu. Po upływie czasu blokada znika sama.

**WAŻNE:** Fizyczne przyciski **zasilania, głośności + i -** pozostają obsługiwane przez Androida — aplikacja ich nie przechwytuje. **Systemowe gesty** (np. ekran główny, pasek powiadomień, przełączanie aplikacji) oraz pewne systemowe okna mogą nadal działać: zwykła aplikacja nie ma uprawnień do blokowania ich globalnie. Nakładka blokuje dotyk **w obrębie okien aplikacji**, nie jest trybem kioskowym ani blokadą bezpieczeństwa. Podczas używania klawiatury systemowej nakładka może znajdować się pod klawiaturą.

**Dlaczego nie używamy 5-sekundowego przytrzymania Power + Volume?** Android rezerwuje przycisk Power i kombinacje klawiszy do systemowych funkcji, takich jak zrzut ekranu, asystent i wyłączenie. Zwykła aplikacja nie może niezawodnie przechwycić tej kombinacji bez ingerencji w system. Zamiast tego jest kafelek Quick Settings i awaryjna kłódka.

## Budowanie APK na Ubuntu

Wypakuj ZIP i otwórz terminal w folderze `TouchGuardAndroid`:

```bash
chmod +x build_apk_ubuntu.sh install_apk_ubuntu.sh
./build_apk_ubuntu.sh
```

Skrypt sprawdza Java/curl/unzip, instaluje brakujące pakiety przez `sudo apt-get`, a następnie pobiera oficjalne Android Command Line Tools, SDK Platform 35, Build Tools 35 i Gradle 8.9. **Nie jest potrzebne Android Studio.** Pierwsze uruchomienie wymaga internetu, kilkuset MB danych, i może wymagać hasła sudo. Pobrane narzędzia zapisuje w `.tools/` oraz standardowym SDK `~/Android/Sdk` (lub pod `ANDROID_SDK_ROOT`).

Po udanym zbudowaniu:

```
output/TouchGuard-debug.apk
```

`debug` oznacza wersję do prywatnej instalacji/testowania, podpisaną automatycznie kluczem debug. Nie jest to podpisane APK release do Play Store.

### Instalacja przez USB

Na telefonie włącz **Opcje programistyczne** i **Debugowanie USB**. Podepnij telefon do Ubuntu, zaakceptuj monit autoryzacji i uruchom:

```bash
./install_apk_ubuntu.sh
```

Możesz też przekopiować plik APK na telefon i zainstalować ręcznie, zezwalając na instalację z danego źródła.

### Kafelek Quick Settings

Android 13+: w aplikacji użyj **Dodaj kafelek**. Jeśli producent telefonu nie wyświetli monitu, rozwiń panel szybkich ustawień, wybierz **Edytuj** i ręcznie przeciągnij **Blokada dotyku** do aktywnych kafelków. W Androidzie 8–12 dodaj kafelek ręcznie.

### Xiaomi / POCO / HyperOS

HyperOS może mieć dodatkową kontrolę nad **wyświetlaniem wyskakujących okien / nakładek** i działaniem aplikacji w tle. Jeżeli nakładka się nie pojawia, sprawdź systemowe uprawnienia TouchGuard; czasem wymagane jest też zezwolenie na wyświetlanie wyskakujących okien podczas działania w tle. Nazwy opcji różnią się między wersjami HyperOS. Wyłączenie optymalizacji baterii nie jest domyślnie wymagane; w razie problemów sprawdź ograniczenia baterii dla aplikacji.

### Bezpieczne testowanie

Pierwszy test wykonaj z domyślną **1 minutą** i z dostępem do fizycznego przycisku zasilania. Włącz w zwykłej aplikacji, dotknij kilka razy poza kłódką i sprawdź, że dotknięcia nie docierają do aplikacji. Sprawdź regulację głośności, szybki panel, przytrzymanie kłódki 3 sekundy i automatyczne odblokowanie. Jeśli aplikacja zostanie ubita przez system, Android usuwa jej okno nakładki — nie powinno dojść do trwałego zablokowania dotyku.

## Kod i struktura

- `app/src/main/java/pl/touchguard/MainActivity.java` — ekran ustawień, uprawnienia, suwak czasu, dodanie kafelka.
- `.../TouchLockService.java` — przezroczysta nakładka, licznik, przytrzymanie awaryjne, powiadomienie.
- `.../TouchTileService.java` — kafelek Quick Settings.
- `.../ToggleActivity.java` — niewidoczne przejście od kafelka do usługi, zgodne z ograniczeniami uruchamiania usług w tle.
- `.../Prefs.java` — zapis czasu w SharedPreferences.
- `build_apk_ubuntu.sh` — budowanie APK; `install_apk_ubuntu.sh` — instalacja przez ADB.

Minimalny Android: 8.0 (API 26), `compileSdk=35`, `targetSdk=35`, AGP 8.7.3, Gradle 8.9, Java 17+. Żadnych uprawnień internetowych ani dostępu do prywatnych danych. Uprawnienie `SYSTEM_ALERT_WINDOW` jest niezbędne; `POST_NOTIFICATIONS` jest opcjonalne.

## GitHub (gdy będziesz gotowy)

```bash
git init
git add .
git commit -m "Initial TouchGuard Android app"
git branch -M main
# Załóż puste repozytorium na GitHubie, potem:
git remote add origin https://github.com/TWOJ_LOGIN/TouchGuardAndroid.git
git push -u origin main
```

`.gitignore` wyklucza pliki APK, pobrane SDK/Gradle, konfigurację lokalną i katalogi build. APK do pobrania można dodać później jako asset do **GitHub Release**.

## Uwagi techniczne

- Nakładka to `TYPE_APPLICATION_OVERLAY` z pełnoekranowym widokiem konsumującym zdarzenia dotyku, ale bez przejmowania fokusu okna. Android wyświetla ją poniżej krytycznych okien systemowych. Usługa działa jako `foregroundServiceType="specialUse"`, z powiadomieniem i `START_NOT_STICKY`.
- Limit czasu jest liczony z `SystemClock.elapsedRealtime()`. Po wybudzeniu telefonu aplikacja sprawdzi, czy limit już minął; system może opóźnić wykonanie timera podczas głębokiego uśpienia.
- System / producent może ograniczać nakładki dla aplikacji chronionych (np. ekran uprawnień, płatności). Nie gwarantujemy zablokowania ich dotyku.
- Publikacja w Google Play aplikacji z usługą pierwszoplanową typu `specialUse` wymaga dodatkowej deklaracji i może podlegać ocenie Google. Ten projekt jest przede wszystkim do użytku prywatnego / udostępnienia na GitHubie.

## Stan testów

Kod i XML można zweryfikować statycznie, ale końcowy build i zachowanie nakładki wymagają Android SDK oraz testu na fizycznym telefonie. W przygotowanym środowisku wykonania nie było zainstalowanego Android SDK, dlatego nie dołączono binarnego APK i **nie należy traktować projektu jako przetestowanego na urządzeniu**.
