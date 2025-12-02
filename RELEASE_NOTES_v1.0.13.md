# Polar H10 Trainer - Wersja v1.0.13

## 📦 Informacje o wydaniu
- **Wersja:** v1.0.13 (build 14)
- **Data:** 2 grudnia 2025
- **Plik:** app-release.aab (8.7 MB)
- **Lokalizacja:** `app\build\outputs\bundle\release\app-release.aab`

---

## ✨ Nowe funkcje

### 🎯 Automatyczne centrowanie ekranu podczas treningu
- Po rozpoczęciu treningu Custom Workout ekran automatycznie przewija się do timerów
- Widoczne oba timery (czas bloku + całość treningu) oraz tętno
- Co 10 sekund sprawdzanie pozycji i powrót do widoku timerów (jeśli użytkownik przewinął >100px)
- Inteligentne przewijanie z offsetem +100px dla optymalnej widoczności

### 💚 Powiększona i kolorowa cyfra tętna
- Wartość tętna (BPM) powiększona 2.5x względem reszty tekstu
- Jaskrawozielony kolor (#00FF00) - taki sam jak timery
- Lepsza widoczność podczas treningu
- Format: ❤️ Tętno: **70** BPM (40% HRMAX)

---

## 🔧 Poprawki i ulepszenia

### Przewijanie ekranu
- Naprawiono logikę przewijania - teraz przewija dokładnie do właściwej pozycji
- Używa `getLocationOnScreen()` zamiast `getTop()` dla precyzyjnego pozycjonowania
- Płynne animacje przewijania (`smoothScrollTo`)
- Zabezpieczenie przed ciągłym przewijaniem w górę/dół

### Stabilność
- Poprawiono licznik sprawdzania przewijania (używa pełnych 10-sekundowych interwałów)
- Dodano zmienne `lastScrollCheckSecond` i `targetScrollY` dla lepszej kontroli
- Optymalizacja wydajności - sprawdzanie tylko co 10 sekund zamiast każdej sekundy

---

## 📊 Strefy tętna - przypomnienie działania

Aplikacja używa **4 stref tętna** z progresywnym systemem kolorów:

| Strefa | Próg | Kolor | Efekt |
|--------|------|-------|-------|
| **Komfort** | ≤60% HRmax | 🟢 Zielony (#2ECC71) | Stały |
| **Ostrzeżenie** | 60-65% HRmax | 🟡 Żółty (#F1C40F) | Stały |
| **Aerobik** | 65-86% HRmax | 🟠 Gradient (#F1C40F→#E67E22) | Płynne przejście |
| **Alarm** | ≥86% HRmax | 🔴 Czerwony (#C0392B↔#922B21) | Miganie co 500ms |

**Funkcje stref:**
- Automatyczna aktywacja po połączeniu z Polar H10
- TTS (głosowe ogłoszenia) przy zmianie strefy
- Zmiana koloru tła całej aplikacji
- Zmiana kolorów wszystkich przycisków (gradient zgodny ze strefą)
- Konfiguracja progów w ustawieniach

---

## 🏃 Custom Workout (Trening Crossfit)

### Funkcje timerow:
- **Typ bloku:** 💪 TRENING / 😮‍💨 REST / ☕ PRZERWA / 🏁 KONIEC (40sp, zielony)
- **Czas bloku:** Duży licznik 144sp, zielony (#00FF00)
- **Całkowity czas:** 40sp, zielony, format "Całość: M:SS"
- **Lista kolejki:** Wizualizacja nadchodzących bloków z emoji

### TTS (ogłoszenia głosowe):
- Start treningu: "Rozpoczynam trening custom"
- Rozpoczęcie bloku: "Trening 1 minuta" / "Rest 30 sekund" / "Przerwa 1 minuta"
- Ostrzeżenie PRZERWA: "30 sekund do końca przerwy"
- Odliczanie: 10, 9, 8... 1 (dla wszystkich bloków w ostatnich 10 sekundach)
- Koniec: "Koniec treningu"

### WakeLock i stabilność:
- PARTIAL_WAKE_LOCK utrzymuje CPU podczas treningu
- Timery działają stabilnie nawet po zminimalizowaniu aplikacji
- Automatyczne przewijanie po powrocie do aplikacji

---

## 🔗 Połączenie Bluetooth

### Częstotliwość odczytu tętna:
- **~1 Hz (raz na sekundę)** - standard Bluetooth Heart Rate Profile
- Dane wysyłane automatycznie przez Polar H10 (push, nie pull)
- Callback `onCharacteristicChanged()` wywołany natychmiast po otrzymaniu
- Brak sztucznych opóźnień w aplikacji
- Zgodność z protokołem BLE Heart Rate Service (UUID: 00002a37)

### Funkcje Bluetooth:
- Auto-reconnect po utracie połączenia
- Monitoring poziomu baterii paska (co 60 sekund)
- Przycisk rozłączenia z zabezpieczeniem (przytrzymanie 5s)
- Wizualizacja statusu połączenia: Polar: ✅ Gotowe - odbiera tętno

---

## 🎨 UI/UX

### Przyciski z gradientami:
- **10 przycisków** reaguje na strefy tętna:
  1. Start bieganie
  2. Stop bieganie
  3. Konfiguracja biegania
  4. Reset biegania
  5. Zapisz trasę GPS
  6. Konfigurator Custom Workout
  7. Start/Stop Custom Workout
  8. Głośność TTS
  9. Rozłącz Polar
  10. HRMAX

### Padding dolny:
- **200px** na dole głównego layoutu
- Rozwiązuje problem z paskiem nawigacji na różnych urządzeniach
- Przycisk START zawsze dostępny

---

## 📱 Wymagania systemowe

- **Android:** 8.0 (API 26) lub nowszy
- **Bluetooth:** BLE 4.0+
- **GPS:** Opcjonalnie dla treningów biegowych
- **Uprawnienia:**
  - Bluetooth (BLUETOOTH, BLUETOOTH_ADMIN, BLUETOOTH_CONNECT, BLUETOOTH_SCAN)
  - Lokalizacja (ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, ACCESS_BACKGROUND_LOCATION)
  - Wake Lock (WAKE_LOCK)
  - Foreground Service (FOREGROUND_SERVICE, FOREGROUND_SERVICE_LOCATION)
  - Internet (INTERNET) - dla map OpenStreetMap
  - Storage (WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE)

---

## 🐛 Znane problemy (rozwiązane w tej wersji)

- ✅ ~~Brak automatycznego centrowania timerów podczas treningu~~
- ✅ ~~Mała cyfra tętna trudna do odczytania~~
- ✅ ~~Przewijanie w górę/dół co 10 sekund~~
- ✅ ~~Timery nie widoczne po starcie treningu~~

---

## 🔄 Historia wersji

### v1.0.13 (build 14) - 2024-12-02
- Automatyczne centrowanie ekranu podczas treningu
- Powiększona zielona cyfra tętna (2.5x)
- Poprawiona logika przewijania ekranu

### v1.0.12 (build 13) - 2024-12-01
- Wszystkie przyciski reagują na strefy tętna
- TTS: "30 sekund do końca przerwy"
- Padding dolny 200px

### v1.0.11 (build 12) - 2024-11-30
- HR zones aktywne po połączeniu z paskiem
- Dynamic block type display
- Green 40sp/144sp timers

### v1.0.10 (build 11) - 2024-11-29
- Baseline release
- Custom Workout Timer
- HR Zone monitoring

---

## 📝 Instrukcja wgrania na Google Play Console

1. Zaloguj się do [Google Play Console](https://play.google.com/console)
2. Wybierz aplikację "Polar H10 Trainer"
3. Przejdź do **Wydania** → **Produkcja** (lub **Testy wewnętrzne**)
4. Kliknij **Utwórz nowe wydanie**
5. Wgraj plik: `app\build\outputs\bundle\release\app-release.aab`
6. Skopiuj poniższe notatki o wersji:

---

## 📋 Notatki o wersji dla Google Play (PL)

```
Wersja 1.0.13 - Ulepszenia UI i automatyczne centrowanie

✨ Co nowego:
• Automatyczne przewijanie do timerów podczas treningu
• Powiększona i zielona cyfra tętna (2.5x większa)
• Lepsze centrowanie ekranu - widoczne wszystkie ważne informacje

🔧 Poprawki:
• Inteligentne przewijanie - ekran wraca do timerów co 10s
• Precyzyjne pozycjonowanie widoku timerów
• Optymalizacja wydajności

📊 Funkcje:
• 4 strefy tętna z kolorami i miganiem
• Custom Workout z timerem i TTS
• Bluetooth Heart Rate (1 Hz)
• Auto-reconnect i monitoring baterii
```

---

## 📋 Notatki o wersji dla Google Play (EN)

```
Version 1.0.13 - UI Improvements & Auto-Scrolling

✨ What's new:
• Auto-scroll to timers during workout
• Enlarged green heart rate number (2.5x bigger)
• Better screen centering - all important info visible

🔧 Fixes:
• Smart scrolling - returns to timers every 10s
• Precise timer view positioning
• Performance optimization

📊 Features:
• 4 HR zones with colors and blinking
• Custom Workout with timer and TTS
• Bluetooth Heart Rate (1 Hz)
• Auto-reconnect and battery monitoring
```

---

## 🎯 Testowanie przed publikacją

### Checklist:
- [x] Kompilacja AAB bez błędów
- [ ] Test instalacji na fizycznym urządzeniu
- [ ] Weryfikacja połączenia Bluetooth z Polar H10
- [ ] Test automatycznego przewijania podczas Custom Workout
- [ ] Sprawdzenie widoczności powiększonej cyfry tętna
- [ ] Test wszystkich 4 stref tętna
- [ ] Weryfikacja TTS w języku polskim
- [ ] Test WakeLock (minimalizacja aplikacji podczas treningu)
- [ ] Sprawdzenie auto-reconnect po utracie połączenia

---

## 📞 Kontakt i wsparcie

W razie problemów sprawdź logi w Android Studio lub użyj `adb logcat -s PolarH10Trainer`

---

**Przygotowane przez:** GitHub Copilot  
**Data:** 2 grudnia 2025  
**Wersja dokumentu:** 1.0
