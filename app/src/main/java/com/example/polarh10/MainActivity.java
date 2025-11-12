package com.example.polarh10;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends Activity {
    
    private static final String TAG = "PolarH10";
    // TWOJA OPASKA POLAR H10
    private static final String POLAR_H10_MAC = "24:AC:AC:09:A6:4D";
    
    // Heart Rate Service UUID (standardowy)
    private static final String HEART_RATE_SERVICE_UUID = "0000180d-0000-1000-8000-00805f9b34fb";
    private static final String HEART_RATE_MEASUREMENT_CHAR_UUID = "00002a37-0000-1000-8000-00805f9b34fb";
    private static final String CLIENT_CHARACTERISTIC_CONFIG_UUID = "00002902-0000-1000-8000-00805f9b34fb";
    
    private TextView statusText;
    private TextView heartRateText;
    private Button connectButton;
    private Button disconnectButton;
    
    // Timer treningowy
    private TextView currentTimeText;
    private TextView workoutTimerText;
    
    // Control buttons (do blokowania podczas treningu)
    private Button mainMinusButton, mainPlusButton;
    private Button workMinusButton, workPlusButton;
    private Button restMinusButton, restPlusButton;
    private Button roundsMinusButton, roundsPlusButton;

    
    private BluetoothGatt bluetoothGatt;
    private Handler handler = new Handler();
    private int currentHeartRate = 0;
    
    // GŁÓWNY TIMER TRENINGOWY
    private int mainWorkoutMinutes = 45;  // całkowity czas treningu (15-120 min)
    private int workoutTimeLeftSeconds = 0;
    private boolean isWorkoutActive = false;
    private boolean isCountdownActive = false;
    private int countdownSeconds = 0;
    
    // CROSSFIT TIMER
    private int workMinutes = 0;    // Work time (0 = wyłączony)
    private int workSeconds = 0;
    private int restMinutes = 0;    // Rest time (0 = wyłączony)  
    private int restSeconds = 0;
    private int totalRounds = 0;    // Rounds (0 = wyłączony)
    
    // CROSSFIT STATE
    private boolean isCrossFitMode = false;
    private int currentRound = 0;
    private boolean isWorkPhase = true;
    private int crossFitTimeLeft = 0;
    
    // STOP TIMER
    private boolean isStopPressed = false;
    private int stopPressCounter = 0;
    
    // UI REFERENCES
    private TextView mainTimeText;
    private TextView workTimeText;
    private TextView restTimeText; 
    private TextView roundsText;
    private Button dynamicStartButton;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Prośba o uprawnienia Bluetooth
        requestBluetoothPermissions();
        
        // Tworzę prosty layout programowo
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);
        
        // Aktualny czas rzeczywisty
        currentTimeText = new TextView(this);
        updateCurrentTime();
        currentTimeText.setTextSize(16);
        currentTimeText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        layout.addView(currentTimeText);
        
        // Tytuł
        TextView titleText = new TextView(this);
        titleText.setText("🎯 POLAR H10 DIRECT");
        titleText.setTextSize(24);
        layout.addView(titleText);
        
        // Adres MAC
        TextView macText = new TextView(this);
        macText.setText("MAC: " + POLAR_H10_MAC);
        macText.setTextSize(14);
        layout.addView(macText);
        
        // Status
        statusText = new TextView(this);
        statusText.setText("Status: Nie połączono");
        statusText.setTextSize(18);
        layout.addView(statusText);
        
        // Tętno
        heartRateText = new TextView(this);
        heartRateText.setText("❤️ Tętno: 0 BPM");
        heartRateText.setTextSize(28);
        layout.addView(heartRateText);
        
        // Przycisk połącz
        connectButton = new Button(this);
        connectButton.setText("🎯 POŁĄCZ Z TWOJĄ OPASKĄ");
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectToPolar();
            }
        });
        layout.addView(connectButton);
        
        // Przycisk rozłącz
        disconnectButton = new Button(this);
        disconnectButton.setText("❌ ROZŁĄCZ");
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnectFromPolar();
            }
        });
        layout.addView(disconnectButton);
        
        // ===== GŁÓWNY TIMER TRENINGOWY =====
        TextView mainTimerTitle = new TextView(this);
        mainTimerTitle.setText("\n⏱️ GŁÓWNY TIMER TRENINGOWY");
        mainTimerTitle.setTextSize(20);
        mainTimerTitle.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        layout.addView(mainTimerTitle);
        
        // Wybór czasu głównego treningu (co 1 minuta)
        LinearLayout mainTimeLayout = new LinearLayout(this);
        mainTimeLayout.setOrientation(LinearLayout.HORIZONTAL);
        
        mainMinusButton = new Button(this);
        mainMinusButton.setText("➖");
        mainMinusButton.setOnClickListener(v -> adjustMainTime(-1));
        mainTimeLayout.addView(mainMinusButton);
        
        mainTimeText = new TextView(this);
        mainTimeText.setText(mainWorkoutMinutes + " minut");
        mainTimeText.setTextSize(18);
        mainTimeText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        mainTimeText.setPadding(30, 20, 30, 20);
        mainTimeLayout.addView(mainTimeText);
        
        mainPlusButton = new Button(this);
        mainPlusButton.setText("➕");
        mainPlusButton.setOnClickListener(v -> adjustMainTime(1));
        mainTimeLayout.addView(mainPlusButton);
        
        layout.addView(mainTimeLayout);
        
        // Timer treningu
        workoutTimerText = new TextView(this);
        workoutTimerText.setText("Gotowy do treningu!");
        workoutTimerText.setTextSize(24);
        workoutTimerText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        layout.addView(workoutTimerText);
        
        // DYNAMICZNY PRZYCISK START/STOP (będzie się przenosił)
        dynamicStartButton = new Button(this);
        dynamicStartButton.setText("🏃‍♂️ START TRENINGU");
        dynamicStartButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (isWorkoutActive) {
                            // STOP mode - przytrzymaj 5s
                            startStopTimer();
                        } else {
                            // START mode - waliduj i startuj
                            validateAndStart();
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (isWorkoutActive) {
                            cancelStopTimer();
                        }
                        return true;
                }
                return false;
            }
        });
        layout.addView(dynamicStartButton);
        
        // ===== CROSSFIT TIMER =====
        TextView crossFitTitle = new TextView(this);
        crossFitTitle.setText("\n💪 CrossFit Timer (opcjonalny)");
        crossFitTitle.setTextSize(18);
        crossFitTitle.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        layout.addView(crossFitTitle);
        
        // Work Time
        TextView workLabel = new TextView(this);
        workLabel.setText("⚡ WORK TIME:");
        workLabel.setTextSize(14);
        workLabel.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        layout.addView(workLabel);
        
        LinearLayout workLayout = new LinearLayout(this);
        workLayout.setOrientation(LinearLayout.HORIZONTAL);
        
        workMinusButton = new Button(this);
        workMinusButton.setText("➖");
        workMinusButton.setOnClickListener(v -> adjustWorkTime(-15));
        workLayout.addView(workMinusButton);
        
        workTimeText = new TextView(this);
        workTimeText.setText(formatTime(workMinutes, workSeconds));
        workTimeText.setTextSize(16);
        workTimeText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        workTimeText.setPadding(30, 10, 30, 10);
        workLayout.addView(workTimeText);
        
        workPlusButton = new Button(this);
        workPlusButton.setText("➕");
        workPlusButton.setOnClickListener(v -> adjustWorkTime(15));
        workLayout.addView(workPlusButton);
        
        layout.addView(workLayout);
        
        // Rest Time
        TextView restLabel = new TextView(this);
        restLabel.setText("😴 REST TIME:");
        restLabel.setTextSize(14);
        restLabel.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        layout.addView(restLabel);
        
        LinearLayout restLayout = new LinearLayout(this);
        restLayout.setOrientation(LinearLayout.HORIZONTAL);
        
        restMinusButton = new Button(this);
        restMinusButton.setText("➖");
        restMinusButton.setOnClickListener(v -> adjustRestTime(-15));
        restLayout.addView(restMinusButton);
        
        restTimeText = new TextView(this);
        restTimeText.setText(formatTime(restMinutes, restSeconds));
        restTimeText.setTextSize(16);
        restTimeText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        restTimeText.setPadding(30, 10, 30, 10);
        restLayout.addView(restTimeText);
        
        restPlusButton = new Button(this);
        restPlusButton.setText("➕");
        restPlusButton.setOnClickListener(v -> adjustRestTime(15));
        restLayout.addView(restPlusButton);
        
        layout.addView(restLayout);
        
        // Rounds
        TextView roundsLabel = new TextView(this);
        roundsLabel.setText("🔄 ROUNDS:");
        roundsLabel.setTextSize(14);
        roundsLabel.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        layout.addView(roundsLabel);
        
        LinearLayout roundsLayout = new LinearLayout(this);
        roundsLayout.setOrientation(LinearLayout.HORIZONTAL);
        
        roundsMinusButton = new Button(this);
        roundsMinusButton.setText("➖");
        roundsMinusButton.setOnClickListener(v -> adjustRounds(-1));
        roundsLayout.addView(roundsMinusButton);
        
        roundsText = new TextView(this);
        roundsText.setText(String.valueOf(totalRounds));
        roundsText.setTextSize(16);
        roundsText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        roundsText.setPadding(30, 10, 30, 10);
        roundsLayout.addView(roundsText);
        
        roundsPlusButton = new Button(this);
        roundsPlusButton.setText("➕");
        roundsPlusButton.setOnClickListener(v -> adjustRounds(1));
        roundsLayout.addView(roundsPlusButton);
        
        layout.addView(roundsLayout);
        
        // DYNAMICZNY PRZYCISK będzie się tutaj pojawiał w CrossFit mode
        
        // Info o walidacji
        TextView validationInfo = new TextView(this);
        validationInfo.setText("\n💡 Wskazówka: Gdy ustawisz Work/Rest/Rounds > 0,\nprzycisk START przeniesie się tutaj i sprawdzi\nczy czasy się zgadzają z głównym timerem.");
        validationInfo.setTextSize(12);
        validationInfo.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        layout.addView(validationInfo);
        
        setContentView(layout);
        
        // Uruchom timer aktualizacji czasu
        startTimeUpdateTimer();
        
        Log.d(TAG, "Aplikacja uruchomiona - gotowa do łączenia z " + POLAR_H10_MAC);
    }
    
    private void requestBluetoothPermissions() {
        String[] permissions = {
            "android.permission.BLUETOOTH",
            "android.permission.BLUETOOTH_ADMIN", 
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.BLUETOOTH_SCAN",
            "android.permission.BLUETOOTH_CONNECT"
        };
        
        ActivityCompat.requestPermissions(this, permissions, 1);
        Log.d(TAG, "🔐 Proszę o uprawnienia Bluetooth");
    }
    
    private void connectToPolar() {
        Log.d(TAG, "🎯 Próba połączenia z " + POLAR_H10_MAC);
        statusText.setText("Status: Łączenie z Twoją opaską...");
        
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                statusText.setText("Status: Błąd - brak Bluetooth");
                return;
            }
            
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(POLAR_H10_MAC);
            bluetoothGatt = device.connectGatt(this, false, gattCallback);
            
        } catch (Exception e) {
            Log.e(TAG, "Błąd łączenia: " + e.getMessage());
            statusText.setText("Status: Błąd łączenia - " + e.getMessage());
        }
    }
    
    private void disconnectFromPolar() {
        Log.d(TAG, "❌ Rozłączanie");
        
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
        
        statusText.setText("Status: Rozłączono");
        heartRateText.setText("❤️ Tętno: 0 BPM");
    }
    
    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "✅ POŁĄCZONO z Polar H10!");
                
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        statusText.setText("Status: ✅ POŁĄCZONO! Szukam serwisu HR...");
                    }
                });
                
                // Szukam serwisów GATT
                gatt.discoverServices();
                
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "❌ ROZŁĄCZONO z Polar H10");
                
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        statusText.setText("Status: Rozłączono");
                        heartRateText.setText("❤️ Tętno: 0 BPM");
                    }
                });
            }
        }
        
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "🔍 Znaleziono serwisy GATT! Szukam Heart Rate...");
                
                // Szukam serwisu Heart Rate
                BluetoothGattService heartRateService = gatt.getService(java.util.UUID.fromString(HEART_RATE_SERVICE_UUID));
                
                if (heartRateService != null) {
                    Log.d(TAG, "✅ Znaleziono Heart Rate Service!");
                    
                    // Szukam charakterystyki Heart Rate Measurement
                    BluetoothGattCharacteristic heartRateChar = heartRateService.getCharacteristic(
                        java.util.UUID.fromString(HEART_RATE_MEASUREMENT_CHAR_UUID));
                    
                    if (heartRateChar != null) {
                        Log.d(TAG, "✅ Znaleziono Heart Rate Characteristic! Włączam notyfikacje...");
                        
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                statusText.setText("Status: ✅ GOTOWE - odbieranie tętna!");
                            }
                        });
                        
                        // Włączam notyfikacje
                        gatt.setCharacteristicNotification(heartRateChar, true);
                        
                        // Konfiguracja deskryptora dla notyfikacji
                        BluetoothGattDescriptor descriptor = heartRateChar.getDescriptor(
                            java.util.UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG_UUID));
                        
                        if (descriptor != null) {
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            gatt.writeDescriptor(descriptor);
                        }
                        
                    } else {
                        Log.e(TAG, "❌ Nie znaleziono Heart Rate Characteristic");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                statusText.setText("Status: Błąd - brak HR Characteristic");
                            }
                        });
                    }
                } else {
                    Log.e(TAG, "❌ Nie znaleziono Heart Rate Service");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            statusText.setText("Status: Błąd - brak HR Service");
                        }
                    });
                }
            } else {
                Log.e(TAG, "❌ Błąd odkrywania serwisów: " + status);
            }
        }
        
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            // TO JEST PRAWDZIWE TĘTNO Z POLAR H10!
            if (HEART_RATE_MEASUREMENT_CHAR_UUID.equals(characteristic.getUuid().toString())) {
                byte[] data = characteristic.getValue();
                int heartRate = parseHeartRateValue(data);
                
                Log.d(TAG, "❤️ PRAWDZIWE TĘTNO: " + heartRate + " BPM");
                
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        heartRateText.setText("❤️ PRAWDZIWE TĘTNO: " + heartRate + " BPM");
                        currentHeartRate = heartRate;
                    }
                });
            }
        }
    };
    
    /**
     * Parsowanie danych tętna z Polar H10
     * Zgodnie ze standardem Bluetooth Heart Rate Profile
     */
    private int parseHeartRateValue(byte[] data) {
        if (data == null || data.length == 0) {
            return 0;
        }
        
        int heartRate = 0;
        int format = data[0] & 0x01; // Pierwszy bit określa format
        
        if (format == 0) {
            // 8-bit heart rate value
            heartRate = data[1] & 0xFF;
        } else {
            // 16-bit heart rate value
            heartRate = (data[1] & 0xFF) | ((data[2] & 0xFF) << 8);
        }
        
        Log.d(TAG, "📊 Parsed HR data: format=" + format + ", heartRate=" + heartRate);
        return heartRate;
    }
    
    // ===== METODY TIMERA TRENINGOWEGO =====
    
    private void updateCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String currentTime = sdf.format(new Date());
        if (currentTimeText != null) {
            currentTimeText.setText("🕐 " + currentTime);
        }
    }
    
    private void startTimeUpdateTimer() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                updateCurrentTime();
                handler.postDelayed(this, 1000); // aktualizacja co sekundę
            }
        });
    }
    

    
    private void startWorkoutCountdown() {
        if (isWorkoutActive || isCountdownActive) return;
        
        Log.d(TAG, "🏃‍♂️ Rozpoczynam odliczanie treningu (10 sekund)");
        isCountdownActive = true;
        countdownSeconds = 10;
        
        // Wyłącz przycisk startu podczas odliczania
        dynamicStartButton.setEnabled(false);
        
        // Rozpocznij odliczanie
        countdownTick();
    }
    
    private void countdownTick() {
        if (countdownSeconds > 0) {
            workoutTimerText.setText("START za: " + countdownSeconds);
            countdownSeconds--;
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    countdownTick();
                }
            }, 1000);
        } else {
            // Koniec odliczania - rozpocznij trening!
            workoutTimerText.setText("🔥 GO! 🔥");
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isCrossFitMode) {
                        startCrossFitRound();
                    } else {
                        startBasicWorkout();
                    }
                }
            }, 500);
        }
    }
    
    private void startActualWorkout() {
        Log.d(TAG, "🔥 TRENING ROZPOCZĘTY - " + mainWorkoutMinutes + " minut");
        isCountdownActive = false;
        isWorkoutActive = true;
        workoutTimeLeftSeconds = mainWorkoutMinutes * 60;
        
        // Zmień dynamicStartButton na STOP i zablokuj wszystkie przyciski +/-
        dynamicStartButton.setText("🛑 STOP (przytrzymaj 5s)");
        dynamicStartButton.setEnabled(true);
        disableAllControlButtons(); // 🔒 BLOKUJE wszystkie przyciski +/-
        
        // Rozpocznij timer treningu
        workoutTick();
    }
    
    private void workoutTick() {
        if (isWorkoutActive && workoutTimeLeftSeconds > 0) {
            int minutes = workoutTimeLeftSeconds / 60;
            int seconds = workoutTimeLeftSeconds % 60;
            
            workoutTimerText.setText(String.format("⏱️ TRENING: %02d:%02d", minutes, seconds));
            workoutTimeLeftSeconds--;
            
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    workoutTick();
                }
            }, 1000);
        } else if (isWorkoutActive) {
            // Koniec treningu!
            finishWorkout();
        }
    }
    
    private void startStopTimer() {
        if (!isWorkoutActive) return;
        
        isStopPressed = true;
        stopPressCounter = 5;
        dynamicStartButton.setText("🛑 STOP " + stopPressCounter + "s");
        
        stopTimerTick();
    }
    
    private void stopTimerTick() {
        if (isStopPressed && stopPressCounter > 0) {
            stopPressCounter--;
            dynamicStartButton.setText("🛑 STOP " + stopPressCounter + "s");
            
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopTimerTick();
                }
            }, 1000);
        } else if (isStopPressed && stopPressCounter == 0) {
            // Zatrzymaj trening
            stopWorkout();
        }
    }
    
    private void cancelStopTimer() {
        if (isStopPressed) {
            isStopPressed = false;
            dynamicStartButton.setText("🛑 STOP (przytrzymaj 5s)");
            Log.d(TAG, "⚠️ Anulowano zatrzymanie treningu");
        }
    }
    
    private void stopWorkout() {
        Log.d(TAG, "🛑 TRENING ZATRZYMANY przez użytkownika");
        isWorkoutActive = false;
        isStopPressed = false;
        
        resetWorkoutUI();
    }
    
    private void finishWorkout() {
        Log.d(TAG, "✅ TRENING ZAKOŃCZONY - czas minął!");
        isWorkoutActive = false;
        
        workoutTimerText.setText("🎉 TRENING SKOŃCZONY! 🎉");
        
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                resetWorkoutUI();
            }
        }, 3000);
    }
    
    private void resetWorkoutUI() {
        // Przywróć interfejs do stanu początkowego
        workoutTimerText.setText("Gotowy do treningu!");
        // stopWorkoutButton.setText("🛑 STOP (przytrzymaj 5s)"); // Już ustawione wcześniej
        
        // Przywróć dynamicStartButton do trybu START i odblokuj przyciski
        dynamicStartButton.setText("🔴 START TRENINGU");
        dynamicStartButton.setEnabled(true);
        enableAllControlButtons(); // 🔓 ODBLOKUJE wszystkie przyciski +/-
        updateButtonPosition(); // Sprawdź pozycję przycisku po zakończeniu
        
        Log.d(TAG, "🔄 Interface zurückgesetzt für nächstes Training");
    }
    
    // ===== ADJUSTMENT FUNCTIONS =====
    private void adjustMainTime(int deltaMinutes) {
        mainWorkoutMinutes += deltaMinutes;
        if (mainWorkoutMinutes < 15) mainWorkoutMinutes = 15;   // minimum 15 min
        if (mainWorkoutMinutes > 120) mainWorkoutMinutes = 120; // maximum 120 min
        
        mainTimeText.setText(mainWorkoutMinutes + " minut");
        Log.d(TAG, "⏱️ Główny timer: " + mainWorkoutMinutes + " minut");
        
        updateButtonPosition(); // Sprawdź czy przycisk ma się przenieść
    }
    
    private void adjustWorkTime(int deltaSeconds) {
        int totalSeconds = workMinutes * 60 + workSeconds + deltaSeconds;
        if (totalSeconds < 0) totalSeconds = 0;  // minimum 0s (wyłącza CrossFit)
        if (totalSeconds > 600) totalSeconds = 600; // maximum 10min
        
        workMinutes = totalSeconds / 60;
        workSeconds = totalSeconds % 60;
        
        workTimeText.setText(formatTime(workMinutes, workSeconds));
        Log.d(TAG, "⚡ Work time: " + formatTime(workMinutes, workSeconds));
        
        updateButtonPosition(); // Sprawdź czy przycisk ma się przenieść
    }
    
    private void adjustRestTime(int deltaSeconds) {
        int totalSeconds = restMinutes * 60 + restSeconds + deltaSeconds;
        if (totalSeconds < 0) totalSeconds = 0;  // minimum 0s
        if (totalSeconds > 300) totalSeconds = 300; // maximum 5min
        
        restMinutes = totalSeconds / 60;
        restSeconds = totalSeconds % 60;
        
        restTimeText.setText(formatTime(restMinutes, restSeconds));
        Log.d(TAG, "😴 Rest time: " + formatTime(restMinutes, restSeconds));
        
        updateButtonPosition(); // Sprawdź czy przycisk ma się przenieść
    }
    
    private void adjustRounds(int delta) {
        totalRounds += delta;
        if (totalRounds < 0) totalRounds = 0;  // minimum 0 (wyłącza CrossFit)
        if (totalRounds > 20) totalRounds = 20;
        
        roundsText.setText(String.valueOf(totalRounds));
        Log.d(TAG, "🔄 Rounds: " + totalRounds);
        
        updateButtonPosition(); // Sprawdź czy przycisk ma się przenieść
    }
    
    private String formatTime(int minutes, int seconds) {
        return minutes + ":" + String.format("%02d", seconds);
    }
    
    // ===== GŁÓWNE FUNKCJE LOGIKI =====
    private void updateButtonPosition() {
        // Sprawdź czy CrossFit jest aktywny (którykolwiek parametr > 0)
        boolean crossFitActive = (workMinutes > 0 || workSeconds > 0 || restMinutes > 0 || restSeconds > 0 || totalRounds > 0);
        
        if (crossFitActive) {
            // Przenieś przycisk na dół (CrossFit mode)
            dynamicStartButton.setText("🏃‍♂️ START CROSSFIT!");
            isCrossFitMode = true;
        } else {
            // Przycisk na górze (zwykły timer)
            dynamicStartButton.setText("🏃‍♂️ START TRENINGU");
            isCrossFitMode = false;
        }
        
        Log.d(TAG, "� Button mode: " + (isCrossFitMode ? "CrossFit" : "Normal"));
    }
    
    // ===== BUTTON CONTROL FUNCTIONS =====
    private void disableAllControlButtons() {
        mainMinusButton.setEnabled(false);
        mainPlusButton.setEnabled(false);
        workMinusButton.setEnabled(false);
        workPlusButton.setEnabled(false);
        restMinusButton.setEnabled(false);
        restPlusButton.setEnabled(false);
        roundsMinusButton.setEnabled(false);
        roundsPlusButton.setEnabled(false);
        Log.d(TAG, "🔒 Wszystkie przyciski +/- ZABLOKOWANE podczas treningu");
    }
    
    private void enableAllControlButtons() {
        mainMinusButton.setEnabled(true);
        mainPlusButton.setEnabled(true);
        workMinusButton.setEnabled(true);
        workPlusButton.setEnabled(true);
        restMinusButton.setEnabled(true);
        restPlusButton.setEnabled(true);
        roundsMinusButton.setEnabled(true);
        roundsPlusButton.setEnabled(true);
        Log.d(TAG, "🔓 Wszystkie przyciski +/- ODBLOKOWANE po treningu");
    }
    
    private void validateAndStart() {
        if (isCrossFitMode) {
            // WALIDACJA CROSSFIT
            int workRestSeconds = (workMinutes * 60 + workSeconds) + (restMinutes * 60 + restSeconds);
            int totalCrossFitSeconds = workRestSeconds * totalRounds;
            int mainTimerSeconds = mainWorkoutMinutes * 60;
            
            if (totalCrossFitSeconds != mainTimerSeconds) {
                // BŁĄD WALIDACJI - Auto-fix
                Log.d(TAG, "❌ WALIDACJA BŁĄD: CrossFit=" + totalCrossFitSeconds + "s, Main=" + mainTimerSeconds + "s");
                
                workoutTimerText.setText("❌ BŁĄD: Czasy się nie zgadzają!\nDopasowuję automatycznie...");
                
                // Auto-fix: Dopasuj Work time żeby się zgadzało
                int targetWorkRestSeconds = mainTimerSeconds / totalRounds;
                int newWorkSeconds = targetWorkRestSeconds - (restMinutes * 60 + restSeconds);
                
                if (newWorkSeconds > 0) {
                    workMinutes = newWorkSeconds / 60;
                    workSeconds = newWorkSeconds % 60;
                } else {
                    // Jeśli rest za długi, skróć rest
                    restMinutes = (targetWorkRestSeconds - 60) / 60; // Work minimum 1min
                    restSeconds = (targetWorkRestSeconds - 60) % 60;
                    workMinutes = 1;
                    workSeconds = 0;
                }
                
                // Update UI
                workTimeText.setText(formatTime(workMinutes, workSeconds));
                restTimeText.setText(formatTime(restMinutes, restSeconds));
                
                handler.postDelayed(() -> {
                    workoutTimerText.setText("✅ CZASY DOPASOWANE!\nTeraz spróbuj ponownie START");
                }, 2000);
                
                return; // Nie startuj jeszcze
            }
            
            // Walidacja OK - start CrossFit
            startCrossFitCountdown();
        } else {
            // Zwykły timer - start od razu
            startBasicCountdown();
        }
    }
    
    private void startBasicCountdown() {
        Log.d(TAG, "🏃‍♂️ START podstawowy timer: " + mainWorkoutMinutes + " minut");
        isCountdownActive = true;
        countdownSeconds = 10;
        countdownTick();
    }
    
    private void startCrossFitCountdown() {
        Log.d(TAG, "💪 START CrossFit: " + totalRounds + " rounds, Work:" + formatTime(workMinutes, workSeconds) + " Rest:" + formatTime(restMinutes, restSeconds));
        isCountdownActive = true;
        countdownSeconds = 10;
        currentRound = 1;
        isWorkPhase = true;
        countdownTick();
    }
    
    private void startBasicWorkout() {
        isWorkoutActive = true;
        isCountdownActive = false;
        workoutTimeLeftSeconds = mainWorkoutMinutes * 60;
        
        dynamicStartButton.setText("🛑 STOP (przytrzymaj 5s)");
        Log.d(TAG, "🏃 PODSTAWOWY TRENING ROZPOCZĘTY - " + mainWorkoutMinutes + " minut");
        
        basicWorkoutTick();
    }
    
    private void startCrossFitRound() {
        isWorkoutActive = true;
        isCountdownActive = false;
        crossFitTimeLeft = workMinutes * 60 + workSeconds;
        
        dynamicStartButton.setText("🛑 STOP (przytrzymaj 5s)");
        Log.d(TAG, "💪 CROSSFIT Round " + currentRound + "/" + totalRounds + " - WORK");
        
        crossFitTick();
    }
    
    private void crossFitTick() {
        if (crossFitTimeLeft > 0) {
            int minutes = crossFitTimeLeft / 60;
            int seconds = crossFitTimeLeft % 60;
            
            String phase = isWorkPhase ? "⚡ WORK" : "😴 REST";
            workoutTimerText.setText(phase + " " + currentRound + "/" + totalRounds + "\n" + 
                                   String.format("%02d:%02d", minutes, seconds));
            
            crossFitTimeLeft--;
            
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    crossFitTick();
                }
            }, 1000);
            
        } else {
            // Phase finished
            if (isWorkPhase) {
                // Work finished, start rest
                if (restMinutes > 0 || restSeconds > 0) {
                    isWorkPhase = false;
                    crossFitTimeLeft = restMinutes * 60 + restSeconds;
                    crossFitTick();
                } else {
                    // No rest, go to next round
                    nextCrossFitRound();
                }
            } else {
                // Rest finished, next round
                nextCrossFitRound();
            }
        }
    }
    
    private void nextCrossFitRound() {
        currentRound++;
        if (currentRound <= totalRounds) {
            isWorkPhase = true;
            crossFitTimeLeft = workMinutes * 60 + workSeconds;
            crossFitTick();
        } else {
            // CrossFit finished!
            finishCrossFitWorkout();
        }
    }
    
    private void basicWorkoutTick() {
        if (isWorkoutActive && workoutTimeLeftSeconds > 0) {
            int minutes = workoutTimeLeftSeconds / 60;
            int seconds = workoutTimeLeftSeconds % 60;
            
            workoutTimerText.setText("⏱️ TRENING: " + String.format("%02d:%02d", minutes, seconds));
            workoutTimeLeftSeconds--;
            
            handler.postDelayed(() -> basicWorkoutTick(), 1000);
        } else if (workoutTimeLeftSeconds <= 0) {
            finishBasicWorkout();
        }
    }
    
    private void finishBasicWorkout() {
        isWorkoutActive = false;
        workoutTimerText.setText("🎉 TRENING SKOŃCZONY! 🎉");
        Log.d(TAG, "🎉 PODSTAWOWY TRENING SKOŃCZONY!");
        
        dynamicStartButton.setText("🏃‍♂️ START TRENINGU");
        handler.postDelayed(() -> {
            workoutTimerText.setText("Gotowy do treningu!");
        }, 5000);
    }
    
    private void finishCrossFitWorkout() {
        isWorkoutActive = false;
        
        workoutTimerText.setText("🎉 CROSSFIT SKOŃCZONY! 🎉");
        Log.d(TAG, "🎉 CROSSFIT COMPLETED!");
        
        dynamicStartButton.setText("🏃‍♂️ START CROSSFIT!");
        handler.postDelayed(() -> {
            workoutTimerText.setText("Gotowy do treningu!");
        }, 5000);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnectFromPolar();
    }
}