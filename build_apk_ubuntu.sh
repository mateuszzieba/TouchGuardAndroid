#!/usr/bin/env bash
# One-command debug APK build on Ubuntu (no Android Studio required).
set -Eeuo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"
PROJECT_DIR="$PWD"
TOOLS_DIR="$PROJECT_DIR/.tools"
SDK_DIR="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Android/Sdk}}"
export ANDROID_SDK_ROOT="$SDK_DIR"
export ANDROID_HOME="$SDK_DIR"
GRADLE_VERSION="8.9"
ANDROID_TOOLS_ARCHIVE="commandlinetools-linux-15859902_latest.zip"
ANDROID_TOOLS_URL="https://dl.google.com/android/repository/$ANDROID_TOOLS_ARCHIVE"
GRADLE_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"

say() { printf '\n\033[1;36m[TouchGuard]\033[0m %s\n' "$*"; }
die() { echo "BŁĄD: $*" >&2; exit 1; }

say "Sprawdzam zależności Ubuntu..."
missing=()
for program in java curl unzip; do
    if ! command -v "$program" >/dev/null 2>&1; then missing+=("$program"); fi
done
if ((${#missing[@]})); then
    say "Instaluję wymagane pakiety (sudo)..."
    sudo apt-get update
    sudo apt-get install -y openjdk-17-jdk curl unzip ca-certificates
fi
command -v java >/dev/null 2>&1 || die "Brak Java. Zainstaluj openjdk-17-jdk."
JAVA_MAJOR="$(java -version 2>&1 | sed -nE '1s/.*version "([0-9]+)(\.[0-9]+)*.*".*/\1/p')"
if [[ "$JAVA_MAJOR" =~ ^[0-9]+$ ]] && (( JAVA_MAJOR < 17 )); then
    say "Potrzebna Java 17 lub nowsza; wykryto Java $JAVA_MAJOR."
    if [[ ! -x /usr/lib/jvm/java-17-openjdk-amd64/bin/java ]]; then
        sudo apt-get update
        sudo apt-get install -y openjdk-17-jdk
    fi
    export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
    export PATH="$JAVA_HOME/bin:$PATH"
fi
mkdir -p "$TOOLS_DIR" "$SDK_DIR" "$PROJECT_DIR/output"

# Prefer existing SDK's sdkmanager. Otherwise download official command-line tools.
SDKMANAGER="$SDK_DIR/cmdline-tools/latest/bin/sdkmanager"
if [[ ! -x "$SDKMANAGER" ]]; then
    say "Pobieram oficjalne narzędzia Android SDK (ok. 182 MB, tylko pierwszy raz)..."
    curl -fL --retry 3 --connect-timeout 20 "$ANDROID_TOOLS_URL" -o "$TOOLS_DIR/$ANDROID_TOOLS_ARCHIVE"
    rm -rf "$TOOLS_DIR/android-cmdline-unpack"
    mkdir -p "$TOOLS_DIR/android-cmdline-unpack" "$SDK_DIR/cmdline-tools"
    unzip -q "$TOOLS_DIR/$ANDROID_TOOLS_ARCHIVE" -d "$TOOLS_DIR/android-cmdline-unpack"
    [[ -f "$TOOLS_DIR/android-cmdline-unpack/cmdline-tools/bin/sdkmanager" ]] || die "Nieprawidłowe archiwum SDK."
    rm -rf "$SDK_DIR/cmdline-tools/latest"
    mv "$TOOLS_DIR/android-cmdline-unpack/cmdline-tools" "$SDK_DIR/cmdline-tools/latest"
    rm -rf "$TOOLS_DIR/android-cmdline-unpack"
fi

say "Instaluję Android SDK Platform 35 i Build Tools 35.0.0..."
# sdkmanager needs accepted Google Android SDK licenses.
set +o pipefail
yes | "$SDKMANAGER" --sdk_root="$SDK_DIR" --licenses >/dev/null
set -o pipefail
"$SDKMANAGER" --sdk_root="$SDK_DIR" "platforms;android-35" "build-tools;35.0.0" "platform-tools"

GRADLE="$TOOLS_DIR/gradle-${GRADLE_VERSION}/bin/gradle"
if [[ ! -x "$GRADLE" ]]; then
    say "Pobieram oficjalny Gradle ${GRADLE_VERSION} (tylko pierwszy raz)..."
    curl -fL --retry 3 --connect-timeout 20 "$GRADLE_URL" -o "$TOOLS_DIR/gradle-${GRADLE_VERSION}-bin.zip"
    unzip -q -o "$TOOLS_DIR/gradle-${GRADLE_VERSION}-bin.zip" -d "$TOOLS_DIR"
fi
[[ -x "$GRADLE" ]] || die "Nie udało się przygotować Gradle."

say "Buduję APK... (pierwsze uruchomienie pobierze także wtyczkę Androida)"
"$GRADLE" --no-daemon --project-dir "$PROJECT_DIR" :app:assembleDebug
SOURCE_APK="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
[[ -f "$SOURCE_APK" ]] || die "Gradle zakończył się bez oczekiwanego APK."
cp "$SOURCE_APK" "$PROJECT_DIR/output/TouchGuard-debug.apk"
say "Gotowe: $PROJECT_DIR/output/TouchGuard-debug.apk"
sha256sum "$PROJECT_DIR/output/TouchGuard-debug.apk"
cat <<EOF

Aby zainstalować na telefonie przez USB (debugowanie USB):
  ./install_apk_ubuntu.sh

Aby przenieść APK ręcznie na telefon:
  output/TouchGuard-debug.apk

EOF
