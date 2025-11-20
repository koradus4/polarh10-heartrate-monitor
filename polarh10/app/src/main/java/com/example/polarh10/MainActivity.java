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
    private TextView workoutTimeSelection;
    private Button minusButton;
    private Button plusButton;
    private Button startWorkoutButton;
    private Button stopWorkoutButton;
    
    private BluetoothGatt bluetoothGatt;
    private Handler handler = new Handler();
    private int currentHeartRate = 0;
    
    // Zmienne timera
    private int selectedWorkoutMinutes = 15;  // domyślnie 15 minut
    private int workoutTimeLeftSeconds = 0;   // pozostały czas w sekundach 
    private int countdownSeconds = 0;         // odliczanie 10-sekund CrossFit
    private boolean isWorkoutActive = false;
    private boolean isCountdownActive = false;
    private boolean isStopPressed = false;
    private int stopPressCounter = 0;
    
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
        
        // SEPARATOR
        TextView separatorText = new TextView(this);
        separatorText.setText("\n⏱️ TIMER TRENINGOWY");
        separatorText.setTextSize(20);
        separatorText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        layout.addView(separatorText);
        
        // Wybór czasu treningu
        LinearLayout timeSelectionLayout = new LinearLayout(this);
        timeSelectionLayout.setOrientation(LinearLayout.HORIZONTAL);
        
        minusButton = new Button(this);
        minusButton.setText("➖");
        minusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adjustWorkoutTime(-15);
            }
        });
        timeSelectionLayout.addView(minusButton);
        
        workoutTimeSelection = new TextView(this);
        workoutTimeSelection.setText(selectedWorkoutMinutes + " min");
        workoutTimeSelection.setTextSize(18);
        workoutTimeSelection.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        workoutTimeSelection.setPadding(30, 20, 30, 20);
        timeSelectionLayout.addView(workoutTimeSelection);
        
        plusButton = new Button(this);
        plusButton.setText("➕");
        plusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adjustWorkoutTime(15);
            }
        });
        timeSelectionLayout.addView(plusButton);
        
        layout.addView(timeSelectionLayout);
        
        // Timer treningu
        workoutTimerText = new TextView(this);
        workoutTimerText.setText("Gotowy do treningu!");
        workoutTimerText.setTextSize(24);
        workoutTimerText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        layout.addView(workoutTimerText);
        
        // Przycisk START
        startWorkoutButton = new Button(this);
        startWorkoutButton.setText("🏃‍♂️ START TRENINGU");
        startWorkoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startWorkoutCountdown();
            }
        });
        layout.addView(startWorkoutButton);
        
        // Przycisk STOP z zabezpieczeniem
        stopWorkoutButton = new Button(this);
        stopWorkoutButton.setText("🛑 STOP (przytrzymaj 5s)");
        stopWorkoutButton.setEnabled(false);
        stopWorkoutButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startStopTimer();
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        cancelStopTimer();
                        return true;
                }
                return false;
            }
        });
        layout.addView(stopWorkoutButton);
        
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
    
    private void adjustWorkoutTime(int deltaMinutes) {
        if (!isWorkoutActive && !isCountdownActive) {
            selectedWorkoutMinutes += deltaMinutes;
            if (selectedWorkoutMinutes < 5) selectedWorkoutMinutes = 5;   // min 5 minut
            if (selectedWorkoutMinutes > 120) selectedWorkoutMinutes = 120; // max 2 godziny
            
            workoutTimeSelection.setText(selectedWorkoutMinutes + " min");
            Log.d(TAG, "⏱️ Wybrano czas treningu: " + selectedWorkoutMinutes + " minut");
        }
    }
    
    private void startWorkoutCountdown() {
        if (isWorkoutActive || isCountdownActive) return;
        
        Log.d(TAG, "🏃‍♂️ Rozpoczynam odliczanie CrossFit (10 sekund)");
        isCountdownActive = true;
        countdownSeconds = 10;
        
        // Wyłącz przyciski wyboru czasu
        minusButton.setEnabled(false);
        plusButton.setEnabled(false);
        startWorkoutButton.setEnabled(false);
        
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
                    startActualWorkout();
                }
            }, 500);
        }
    }
    
    private void startActualWorkout() {
        Log.d(TAG, "🔥 TRENING ROZPOCZĘTY - " + selectedWorkoutMinutes + " minut");
        isCountdownActive = false;
        isWorkoutActive = true;
        workoutTimeLeftSeconds = selectedWorkoutMinutes * 60;
        
        // Włącz przycisk STOP
        stopWorkoutButton.setEnabled(true);
        
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
        stopWorkoutButton.setText("🛑 STOP " + stopPressCounter + "s");
        
        stopTimerTick();
    }
    
    private void stopTimerTick() {
        if (isStopPressed && stopPressCounter > 0) {
            stopPressCounter--;
            stopWorkoutButton.setText("🛑 STOP " + stopPressCounter + "s");
            
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
            stopWorkoutButton.setText("🛑 STOP (przytrzymaj 5s)");
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
        stopWorkoutButton.setText("🛑 STOP (przytrzymaj 5s)");
        
        minusButton.setEnabled(true);
        plusButton.setEnabled(true);
        startWorkoutButton.setEnabled(true);
        stopWorkoutButton.setEnabled(false);
        
        Log.d(TAG, "🔄 Interface zurückgesetzt für nächstes Training");
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnectFromPolar();
    }
}