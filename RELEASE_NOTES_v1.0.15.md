# Polar H10 Trainer - Wersja 1.0.15

## 🆕 Nowe funkcje

### 👥 System wyboru użytkownika
- **Ekran startowy z profilami** - wybór między KRYSPIN i JOGIN
- **Zapamiętanie ostatniego użytkownika** - szybkie uruchomienie przy kolejnym starcie
- **Dedykowane MAC dla każdego użytkownika:**
  - KRYSPIN: 24:AC:AC:07:CA:8D (Polar H10 07CA8D33)
  - JOGIN: 24:AC:AC:09:A6:4D (Polar H10 09A64D3F)
- **Przycisk zmiany użytkownika** - możliwość zmiany profilu w trakcie sesji (przytrzymanie 3s)

## 🔧 Poprawki

### ⏱️ Precyzyjny timer Custom Workout
- **Dokładność milisekundowa** - timer oparty na System.currentTimeMillis()
- **Wyeliminowane opóźnienia** - różnica między stoperem a timerem <1 sekunda
- **Poprawiony countdown** - natychmiastowy start po odliczaniu
- **Problem:** Poprzednie wersje miały błąd ~5-12 sekund na dłuższych treningach
- **Rozwiązanie:** Timer nie opiera się już na tickach, tylko na rzeczywistym czasie

### 🎨 Spójność interfejsu
- **Przycisk "ZMIEŃ UŻYTKOWNIKA"** dodany do systemu dynamicznych gradientów
- Wszystkie przyciski reagują jednolicie na strefy tętna

## 📋 Szczegóły techniczne

**Zmiany w timerze Custom Workout:**
- Usunięto akumulację opóźnień z handler.postDelayed()
- Automatyczne wykrywanie zmiany bloków na podstawie czasu
- Licznik czasu całkowitego i blokowego wyliczany dynamicznie
- Countdown zapisuje czas startu dopiero po zakończeniu odliczania

**Zarządzanie użytkownikami:**
- Nowa aktywność: UserSelectionActivity.java
- SharedPreferences dla zapamiętania wyboru
- Automatyczne rozłączanie Bluetooth przy zmianie użytkownika
- Czyszczenie sesji przy przełączeniu profilu

## 🎯 Wpływ dla użytkownika

✅ **Dwie osoby mogą korzystać z aplikacji** na tej samej sali bez konfliktów  
✅ **Timer pokazuje dokładny czas** - idealny dla treningów interwałowych  
✅ **Szybki start** - zapamiętanie ostatniego użytkownika  
✅ **Czytelny interfejs** - wszystkie przyciski spójne kolorystycznie

## 📊 Statystyki

- Wersja: **v1.0.15** (build 16)
- Min SDK: Android 8.0 (API 26)
- Target SDK: Android 14 (API 36)
- Rozmiar: ~8 MB (AAB)

---

**Data wydania:** 4 stycznia 2026
