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
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
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
    
    private BluetoothGatt bluetoothGatt;
    private Handler handler = new Handler();
    private int currentHeartRate = 0;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Prośba o uprawnienia Bluetooth
        requestBluetoothPermissions();
        
        // Tworzę prosty layout programowo
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);
        
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
        
        setContentView(layout);
        
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
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnectFromPolar();
    }
}