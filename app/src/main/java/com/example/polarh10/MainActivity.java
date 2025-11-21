package com.example.polarh10;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.speech.tts.TextToSpeech;
import android.media.AudioManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.content.Context;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import android.os.Environment;
import android.os.PowerManager;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.content.Intent;

public class MainActivity extends Activity {
    
    private static final String TAG = "PolarH10";
    // TWOJA OPASKA POLAR H10
    private static final String POLAR_H10_MAC = "24:AC:AC:09:A6:4D";
    
    // Heart Rate Service UUID (standardowy)
    private static final String HEART_RATE_SERVICE_UUID = "0000180d-0000-1000-8000-00805f9b34fb";
    private static final String HEART_RATE_MEASUREMENT_CHAR_UUID = "00002a37-0000-1000-8000-00805f9b34fb";
    private static final String CLIENT_CHARACTERISTIC_CONFIG_UUID = "00002902-0000-1000-8000-00805f9b34fb";
    
    private TextView statusText;
    private TextView gpsStatusText;
    private TextView heartRateText;
    private Button connectButton;
    
    // Timer treningowy
    private TextView currentTimeText;
    private TextView workoutTimerText;
    private TextView mainTimerDisplay;
    private Button timerTypeButton;
    private LinearLayout timerSettingsLayout;
    
    // Workout timer controls
    private TextView workoutTimeText;
    private Button workoutMinusButton;
    private Button workoutPlusButton;
    private TextView restTimeText;
    private Button restMinusButton;
    private Button restPlusButton;
    private TextView roundsText;
    private Button roundsMinusButton;
    private Button roundsPlusButton;
    private Button startWorkoutButton;
    private Button stopWorkoutButton;
    private Button runningWorkoutButton;
    private Button closeAppButton;
    private Button showMapButton;
    private LinearLayout mainLayout;
    private ScrollView scrollView;
    
    private BluetoothGatt bluetoothGatt;
    private Handler handler = new Handler();
    private int currentHeartRate = 0;
    private String lastGpxPath = null;
    
    // Zmienne timera
    private String selectedTimerType = "treningowy";  // "treningowy" lub "biegowy"
    private int workoutTimeMinutes = 3;               // czas pracy w minutach
    private int restTimeSeconds = 60;                 // czas odpoczynku w sekundach (domyślnie 60s = 1min)
    private int totalRounds = 5;                      // ilość rund
    private int currentRound = 0;                     // aktualna runda
    private boolean isInWorkoutPhase = true;          // true=workout, false=rest
    private int workoutTimeLeftSeconds = 0;           // pozostały czas w sekundach 
    private int countdownSeconds = 0;                 // odliczanie 10-sekund CrossFit
    private boolean isWorkoutActive = false;
    private boolean isCountdownActive = false;
    private boolean isStopPressed = false;
    private int stopPressCounter = 0;
    
    // Zabezpieczenie przycisku rozłącz - przytrzymanie 5s
    private boolean isDisconnectPressActive = false;
    private long disconnectPressStartTime = 0;
    
    // Zabezpieczenie przycisku zatrzymaj trening biegowy - przytrzymanie 5s
    private boolean isStopRunningPressActive = false;
    private long stopRunningPressStartTime = 0;
    
    // Zabezpieczenie przycisku zamknij - przytrzymanie 5s
    private boolean isClosePressActive = false;
    private long closePressStartTime = 0;
    
    // TTS i Audio
    private TextToSpeech tts;
    private AudioManager audioManager;
    private int originalVolume = -1; // Zapis pierwotnej głośności
    private boolean isTtsReady = false;
    
    // WakeLock - utrzymuje CPU włączony podczas treningu
    private PowerManager.WakeLock wakeLock;
    
    // Trening biegowy - GPS i tracking
    private LocationManager locationManager;
    private LocationListener locationListener;
    private boolean isRunningWorkoutActive = false;
    private Location lastLocation = null;
    private double totalDistance = 0.0; // w metrach
    private long runningStartTime = 0;
    private double maxSpeed = 0.0; // w m/s
    private int heartRateSum = 0;
    private int heartRateCount = 0;
    private int maxHeartRate = 0;
    private ArrayList<TrackPoint> gpsTrack = new ArrayList<>();

    // Strefy tętna
    private static final String PREFS_NAME = "hr_zone_prefs";
    private static final String PREF_HR_MAX = "pref_hr_max";
    private static final String PREF_ZONE_COMFORT = "pref_zone_comfort";
    private static final String PREF_ZONE_AEROBIC = "pref_zone_aerobic";
    private static final String PREF_ZONE_ALARM = "pref_zone_alarm";
    private static final int DEFAULT_HR_MAX = 175;
    private static final int DEFAULT_ZONE_COMFORT = 60;
    private static final int DEFAULT_ZONE_AEROBIC = 85;
    private static final int DEFAULT_ZONE_ALARM = 86;
    private static final int HR_MAX_MIN = 100;
    private static final int HR_MAX_MAX = 220;
    private static final int ZONE_PERCENT_MIN = 40;
    private static final int ZONE_PERCENT_MAX = 100;
    private static final int ZONE_WARNING_WINDOW = 5;

    private Button hrSettingsButton;
    private int hrMaxValue = DEFAULT_HR_MAX;
    private int comfortZonePercent = DEFAULT_ZONE_COMFORT;
    private int aerobicZonePercent = DEFAULT_ZONE_AEROBIC;
    private int alarmZonePercent = DEFAULT_ZONE_ALARM;

    private View zoneBackgroundTarget;
    private int defaultBackgroundColor = Color.TRANSPARENT;
    private boolean isZoneBlinking = false;
    private boolean zoneBlinkState = false;
    private int zoneBlinkPrimaryColor;
    private int zoneBlinkSecondaryColor;
    private Runnable zoneBlinkRunnable;
    private int currentZoneState = -1;
    private long lastZoneAnnouncementTime = 0L;
    private int lastZoneAnnounced = -1;
    private long lastTimerSpeakTimestamp = 0L;
    private long lastZoneSpeakTimestamp = 0L;
    private long lastZoneSettingsChangeTime = 0L;
    private boolean isZoneMonitoringActive = false;
    
    // Klasa pomocnicza do przechowywania punktów GPS
    private static class TrackPoint {
        double latitude;
        double longitude;
        double elevation;
        long timestamp;
        int heartRate;
        
        TrackPoint(double lat, double lon, double ele, long time, int hr) {
            this.latitude = lat;
            this.longitude = lon;
            this.elevation = ele;
            this.timestamp = time;
            this.heartRate = hr;
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Prośba o uprawnienia Bluetooth
        requestBluetoothPermissions();
        
        // Inicjalizacja TTS i Audio
        initializeTTS();
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        
        // Zapisz pierwotną głośność przy uruchomieniu aplikacji
        originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        Log.d(TAG, "📱 URUCHOMIENIE: Zapisano pierwotną głośność: " + originalVolume);
        
        // Inicjalizacja WakeLock
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PolarH10::WorkoutWakeLock");
        Log.d(TAG, "🔋 WakeLock zainicjalizowany");
        
        // Tworzę ScrollView + LinearLayout dla przewijania
        scrollView = new ScrollView(this);
        zoneBackgroundTarget = scrollView;
        if (scrollView.getBackground() instanceof ColorDrawable) {
            defaultBackgroundColor = ((ColorDrawable) scrollView.getBackground()).getColor();
        } else {
            defaultBackgroundColor = Color.TRANSPARENT;
        }

        mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(30, 30, 30, 30);

        loadHrZonePreferences();
        
        // Aktualny czas rzeczywisty
        currentTimeText = new TextView(this);
        updateCurrentTime();
        currentTimeText.setTextSize(16);
        currentTimeText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        mainLayout.addView(currentTimeText);
        
        // Tytuł
        TextView titleText = new TextView(this);
        titleText.setText("🎯 POLAR H10 DIRECT");
        titleText.setTextSize(24);
        mainLayout.addView(titleText);
        
        // Adres MAC
        TextView macText = new TextView(this);
        macText.setText("MAC: " + POLAR_H10_MAC);
        macText.setTextSize(14);
        mainLayout.addView(macText);
        
        // Status Polar
        statusText = new TextView(this);
        statusText.setText("Polar: Nie połączono");
        statusText.setTextSize(16);
        mainLayout.addView(statusText);
        
        // Status GPS
        gpsStatusText = new TextView(this);
        gpsStatusText.setText("GPS: Sprawdzam...");
        gpsStatusText.setTextSize(16);
        mainLayout.addView(gpsStatusText);
        
        // Tętno
        heartRateText = new TextView(this);
        updateHeartRateDisplay(0);
        heartRateText.setTextSize(28);
        mainLayout.addView(heartRateText);
        
        // Przycisk połącz/rozłącz
        connectButton = new Button(this);
        connectButton.setText("🎯 POŁĄCZ");
        GradientDrawable connectGradient = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{Color.parseColor("#3d4a2c"), Color.parseColor("#5a6b47"), Color.parseColor("#3d4a2c")}
        );
        connectGradient.setCornerRadius(30);
        connectButton.setBackground(connectGradient);
        connectButton.setTextColor(0xFFFFFFFF);
        connectButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (bluetoothGatt != null) {
                            // Połączony - rozpocznij licznik rozłączania
                            isDisconnectPressActive = true;
                            disconnectPressStartTime = System.currentTimeMillis();
                            handler.post(disconnectCountdownRunnable);
                        } else {
                            // Nie połączony - natychmiastowe połączenie
                            toggleConnection();
                        }
                        return true;
                        
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (isDisconnectPressActive) {
                            // Przerwano przytrzymanie
                            isDisconnectPressActive = false;
                            handler.removeCallbacks(disconnectCountdownRunnable);
                            connectButton.setText("🔌 ROZŁĄCZ");
                        }
                        return true;
                }
                return false;
            }
        });
        LinearLayout.LayoutParams connectLayoutParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        connectLayoutParams.setMargins(0, 20, 0, 12);
        connectButton.setLayoutParams(connectLayoutParams);
        mainLayout.addView(connectButton);

        // Przycisk ustawień stref tętna
        hrSettingsButton = new Button(this);
        hrSettingsButton.setText(getHrSettingsButtonLabel());
        GradientDrawable hrGradient = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{Color.parseColor("#3d4a2c"), Color.parseColor("#5a6b47"), Color.parseColor("#3d4a2c")}
        );
        hrGradient.setCornerRadius(30);
        hrSettingsButton.setBackground(hrGradient);
        hrSettingsButton.setTextColor(0xFFFFFFFF);
        hrSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHrZoneSettingsDialog();
            }
        });
        LinearLayout.LayoutParams hrLayoutParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        hrLayoutParams.setMargins(0, 0, 0, 20);
        hrSettingsButton.setLayoutParams(hrLayoutParams);
        mainLayout.addView(hrSettingsButton);
        updateHrSettingsButtonLabel();
        
        // SEPARATOR
        TextView separatorText = new TextView(this);
        separatorText.setText("\n⏱️ TIMERY TRENINGOWE");
        separatorText.setTextSize(20);
        separatorText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        mainLayout.addView(separatorText);
        
        // Przycisk wyboru typu timera
        timerTypeButton = new Button(this);
        timerTypeButton.setText("🏆 Timer CrossFit ▼");
        GradientDrawable timerTypeGradient = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{Color.parseColor("#3d4a2c"), Color.parseColor("#5a6b47"), Color.parseColor("#3d4a2c")}
        );
        timerTypeGradient.setCornerRadius(30);
        timerTypeButton.setBackground(timerTypeGradient);
        timerTypeButton.setTextColor(0xFFFFFFFF);
        timerTypeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleTimerType();
            }
        });
        mainLayout.addView(timerTypeButton);
        
        // Główny timer display
        mainTimerDisplay = new TextView(this);
        updateMainTimerDisplay();
        mainTimerDisplay.setTextSize(28);
        mainTimerDisplay.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        mainTimerDisplay.setPadding(20, 30, 20, 30);
        mainLayout.addView(mainTimerDisplay);
        
        // Layout dla ustawień timera
        timerSettingsLayout = new LinearLayout(this);
        timerSettingsLayout.setOrientation(LinearLayout.VERTICAL);
        timerSettingsLayout.setPadding(20, 10, 20, 10);
        
        // Workout Time
        LinearLayout workoutLayout = new LinearLayout(this);
        workoutLayout.setOrientation(LinearLayout.HORIZONTAL);
        TextView workoutLabel = new TextView(this);
        workoutLabel.setText("Czas pracy: ");
        workoutLabel.setTextSize(16);
        workoutLayout.addView(workoutLabel);
        
        workoutMinusButton = new Button(this);
        workoutMinusButton.setText("➖");
        GradientDrawable workoutMinusGradient = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{Color.parseColor("#3d4a2c"), Color.parseColor("#5a6b47"), Color.parseColor("#3d4a2c")}
        );
        workoutMinusGradient.setCornerRadius(30);
        workoutMinusButton.setBackground(workoutMinusGradient);
        workoutMinusButton.setTextColor(0xFFFFFFFF);
        workoutMinusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adjustWorkoutTime(-1);
            }
        });
        workoutLayout.addView(workoutMinusButton);
        
        workoutTimeText = new TextView(this);
        workoutTimeText.setText(workoutTimeMinutes + ":00");
        workoutTimeText.setTextSize(18);
        workoutTimeText.setPadding(30, 20, 30, 20);
        workoutLayout.addView(workoutTimeText);
        
        workoutPlusButton = new Button(this);
        workoutPlusButton.setText("➕");
        GradientDrawable workoutPlusGradient = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{Color.parseColor("#3d4a2c"), Color.parseColor("#5a6b47"), Color.parseColor("#3d4a2c")}
        );
        workoutPlusGradient.setCornerRadius(30);
        workoutPlusButton.setBackground(workoutPlusGradient);
        workoutPlusButton.setTextColor(0xFFFFFFFF);
        workoutPlusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adjustWorkoutTime(1);
            }
        });
        workoutLayout.addView(workoutPlusButton);
        timerSettingsLayout.addView(workoutLayout);
        
        // Rest Time
        LinearLayout restLayout = new LinearLayout(this);
        restLayout.setOrientation(LinearLayout.HORIZONTAL);
        TextView restLabel = new TextView(this);
        restLabel.setText("Czas odpoczynku: ");
        restLabel.setTextSize(16);
        restLayout.addView(restLabel);
        
        restMinusButton = new Button(this);
        restMinusButton.setText("➖");
        GradientDrawable restMinusGradient = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{Color.parseColor("#3d4a2c"), Color.parseColor("#5a6b47"), Color.parseColor("#3d4a2c")}
        );
        restMinusGradient.setCornerRadius(30);
        restMinusButton.setBackground(restMinusGradient);
        restMinusButton.setTextColor(0xFFFFFFFF);
        restMinusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adjustRestTime(-15);
            }
        });
        restLayout.addView(restMinusButton);
        
        restTimeText = new TextView(this);
        int restMin = restTimeSeconds / 60;
        int restSec = restTimeSeconds % 60;
        restTimeText.setText(String.format("%d:%02d", restMin, restSec));
        restTimeText.setTextSize(18);
        restTimeText.setPadding(30, 20, 30, 20);
        restLayout.addView(restTimeText);
        
        restPlusButton = new Button(this);
        restPlusButton.setText("➕");
        GradientDrawable restPlusGradient = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{Color.parseColor("#3d4a2c"), Color.parseColor("#5a6b47"), Color.parseColor("#3d4a2c")}
        );
        restPlusGradient.setCornerRadius(30);
        restPlusButton.setBackground(restPlusGradient);
        restPlusButton.setTextColor(0xFFFFFFFF);
        restPlusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adjustRestTime(15);
            }
        });
        restLayout.addView(restPlusButton);
        timerSettingsLayout.addView(restLayout);
        
        // Rounds
        LinearLayout roundsLayout = new LinearLayout(this);
        roundsLayout.setOrientation(LinearLayout.HORIZONTAL);
        TextView roundsLabel = new TextView(this);
        roundsLabel.setText("Rundy: ");
        roundsLabel.setTextSize(16);
        roundsLayout.addView(roundsLabel);
        
        roundsMinusButton = new Button(this);
        roundsMinusButton.setText("➖");
        GradientDrawable roundsMinusGradient = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{Color.parseColor("#3d4a2c"), Color.parseColor("#5a6b47"), Color.parseColor("#3d4a2c")}
        );
        roundsMinusGradient.setCornerRadius(30);
        roundsMinusButton.setBackground(roundsMinusGradient);
        roundsMinusButton.setTextColor(0xFFFFFFFF);
        roundsMinusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adjustRounds(-1);
            }
        });
        roundsLayout.addView(roundsMinusButton);
        
        roundsText = new TextView(this);
        roundsText.setText(String.valueOf(totalRounds));
        roundsText.setTextSize(18);
        roundsText.setPadding(30, 20, 30, 20);
        roundsLayout.addView(roundsText);
        
        roundsPlusButton = new Button(this);
        roundsPlusButton.setText("➕");
        GradientDrawable roundsPlusGradient = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{Color.parseColor("#3d4a2c"), Color.parseColor("#5a6b47"), Color.parseColor("#3d4a2c")}
        );
        roundsPlusGradient.setCornerRadius(30);
        roundsPlusButton.setBackground(roundsPlusGradient);
        roundsPlusButton.setTextColor(0xFFFFFFFF);
        roundsPlusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adjustRounds(1);
            }
        });
        roundsLayout.addView(roundsPlusButton);
        timerSettingsLayout.addView(roundsLayout);
        
        mainLayout.addView(timerSettingsLayout);
        
        // Timer treningu
        workoutTimerText = new TextView(this);
        workoutTimerText.setText("Gotowy do treningu!");
        workoutTimerText.setTextSize(24);
        workoutTimerText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        mainLayout.addView(workoutTimerText);
        
        // Przycisk START/STOP połączony
        startWorkoutButton = new Button(this);
        startWorkoutButton.setText("🏃‍♂️ START TRENINGU");
        GradientDrawable startGradient = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{Color.parseColor("#3d4a2c"), Color.parseColor("#5a6b47"), Color.parseColor("#3d4a2c")}
        );
        startGradient.setCornerRadius(30);
        startWorkoutButton.setBackground(startGradient);
        startWorkoutButton.setTextColor(0xFFFFFFFF);
        startWorkoutButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (!isWorkoutActive && !isCountdownActive) {
                            // Trening nieaktywny - start natychmiast
                            startWorkoutCountdown();
                        } else {
                            // Trening aktywny - przytrzymaj 5s do zatrzymania
                            startStopTimer();
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (isWorkoutActive || isCountdownActive) {
                            cancelStopTimer();
                        }
                        return true;
                }
                return false;
            }
        });
        mainLayout.addView(startWorkoutButton);
        
        // Ukryty stopWorkoutButton - używany wewnętrznie
        stopWorkoutButton = new Button(this);
        stopWorkoutButton.setVisibility(View.GONE);
        
        // Przycisk TRENING BIEGOWY - będzie dodawany dynamicznie
        createRunningWorkoutButton();
        
        // Przycisk pokazywania trasy GPS
        showMapButton = new Button(this);
        showMapButton.setText("🗺️ POKAŻ TRASĘ");
        showMapButton.setTextSize(14);
        showMapButton.setVisibility(View.GONE); // Ukryty dopóki nie ma trasy
        
        GradientDrawable mapButtonDrawable = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{
                Color.parseColor("#3d4a2c"),
                Color.parseColor("#5a6b47"),
                Color.parseColor("#3d4a2c")
            }
        );
        mapButtonDrawable.setCornerRadius(30);
        showMapButton.setBackground(mapButtonDrawable);
        showMapButton.setTextColor(0xFFFFFFFF);
        
        LinearLayout.LayoutParams mapParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 120);
        mapParams.setMargins(0, 20, 0, 0);
        showMapButton.setLayoutParams(mapParams);
        showMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (lastGpxPath != null) {
                    Intent intent = new Intent(MainActivity.this, MapActivity.class);
                    intent.putExtra("GPX_PATH", lastGpxPath);
                    startActivity(intent);
                } else {
                    speak("Brak zapisanej trasy");
                }
            }
        });
        mainLayout.addView(showMapButton);
        
        // Przycisk zamknij aplikację - ZAWSZE NA KOŃCU
        closeAppButton = new Button(this);
        closeAppButton.setText("🚺 ZAMKNIJ");
        closeAppButton.setTextSize(12);
        
        // Gradient panterka wojskowa - zaokrąglony
        GradientDrawable closeButtonDrawable = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{
                Color.parseColor("#3d4a2c"), // Ciemnozielony
                Color.parseColor("#5a6b47"), // Średni zielony
                Color.parseColor("#3d4a2c")  // Ciemnozielony
            }
        );
        closeButtonDrawable.setShape(GradientDrawable.RECTANGLE);
        closeButtonDrawable.setCornerRadius(30); // Zaokrąglone rogi
        closeAppButton.setBackground(closeButtonDrawable);
        
        closeAppButton.setTextColor(0xFFFFFFFF); // Biały tekst
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 120);
        closeParams.setMargins(0, 20, 0, 0);
        closeAppButton.setLayoutParams(closeParams);
        closeAppButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        isClosePressActive = true;
                        closePressStartTime = System.currentTimeMillis();
                        handler.post(closeCountdownRunnable);
                        return true;
                        
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        isClosePressActive = false;
                        handler.removeCallbacks(closeCountdownRunnable);
                        closeAppButton.setText("🚺 ZAMKNIJ");
                        return true;
                }
                return false;
            }
        });
        mainLayout.addView(closeAppButton);
        
        // Dodaj layout do ScrollView
        scrollView.addView(mainLayout);
        setContentView(scrollView);
        resetZoneFeedback();
        updateHeartRateZoneUI();
        
        // Uruchom timer aktualizacji czasu
        startTimeUpdateTimer();
        
        Log.d(TAG, "Aplikacja uruchomiona - gotowa do łączenia z " + POLAR_H10_MAC);
        
        // AUTO-CONNECT: Połącz z Polar H10 i inicjalizuj GPS automatycznie
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                autoConnectPolarAndGPS();
            }
        }, 1000); // Odczekaj 1s na inicjalizację UI
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
    
    private void autoConnectPolarAndGPS() {
        Log.d(TAG, "🚀 AUTO-CONNECT: Rozpoczynam automatyczne połączenia...");
        
        // 1. Połącz z Polar H10
        connectToPolar();
        
        // 2. Inicjalizuj GPS (sprawdzenie czy włączony)
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkGPSStatus();
            }
        }, 2000); // Poczekaj 2s na połączenie Polar
    }
    
    private void checkGPSStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        
        if (locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Log.d(TAG, "✅ GPS włączony");
            gpsStatusText.setText("GPS: ✅ Włączony");
            // Komunikat głosowy - tylko że włączony, nie "gotowy"
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isTtsReady) {
                        speak("GPS włączony");
                    }
                }
            }, 500);
        } else {
            Log.w(TAG, "⚠️ GPS wyłączony");
            gpsStatusText.setText("GPS: ❌ Wyłączony");
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isTtsReady) {
                        speak("Ostrzeżenie. GPS nie jest włączony. Włącz GPS w ustawieniach.");
                    }
                }
            }, 500);
        }
    }
    
    private void toggleConnection() {
        if (bluetoothGatt != null) {
            // Już połączony - rozłącz
            disconnectFromPolar();
        } else {
            // Nie połączony - połącz
            connectToPolar();
        }
    }
    
    // Runnable do odliczania 5s przed rozłączeniem
    private Runnable disconnectCountdownRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isDisconnectPressActive) {
                return;
            }
            
            long elapsedTime = System.currentTimeMillis() - disconnectPressStartTime;
            long remainingTime = 5000 - elapsedTime;
            
            if (remainingTime <= 0) {
                // 5 sekund upłynęło - rozłącz
                isDisconnectPressActive = false;
                disconnectFromPolar();
            } else {
                // Aktualizuj tekst przycisku
                int secondsLeft = (int) Math.ceil(remainingTime / 1000.0);
                connectButton.setText("⏱️ PRZYTRZYMAJ " + secondsLeft + "s");
                handler.postDelayed(this, 100);
            }
        }
    };
    
    // Runnable do odliczania 5s przed zatrzymaniem treningu biegowego
    private Runnable stopRunningCountdownRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isStopRunningPressActive) {
                return;
            }
            
            long elapsedTime = System.currentTimeMillis() - stopRunningPressStartTime;
            long remainingTime = 5000 - elapsedTime;
            
            if (remainingTime <= 0) {
                // 5 sekund upłynęło - zatrzymaj trening
                isStopRunningPressActive = false;
                stopRunningWorkout();
                runningWorkoutButton.setText("🏃‍♂️ TRENING BIEGOWY");
                GradientDrawable greenGradient = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    new int[]{Color.parseColor("#3d4a2c"), Color.parseColor("#5a6b47"), Color.parseColor("#3d4a2c")}
                );
                greenGradient.setCornerRadius(30);
                runningWorkoutButton.setBackground(greenGradient);
            } else {
                // Aktualizuj tekst przycisku
                int secondsLeft = (int) Math.ceil(remainingTime / 1000.0);
                runningWorkoutButton.setText("⏱️ PRZYTRZYMAJ " + secondsLeft + "s");
                handler.postDelayed(this, 100);
            }
        }
    };
    
    // Runnable do odliczania 5s przed zamknięciem aplikacji
    private Runnable closeCountdownRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isClosePressActive) {
                return;
            }
            
            long elapsedTime = System.currentTimeMillis() - closePressStartTime;
            long remainingTime = 5000 - elapsedTime;
            
            if (remainingTime <= 0) {
                // 5 sekund upłynęło - zamknij aplikację
                isClosePressActive = false;
                Log.d(TAG, "🚺 Zamykanie aplikacji przez użytkownika");
                finish();
            } else {
                // Aktualizuj tekst przycisku
                int secondsLeft = (int) Math.ceil(remainingTime / 1000.0);
                closeAppButton.setText("⏱️ PRZYTRZYMAJ " + secondsLeft + "s");
                handler.postDelayed(this, 100);
            }
        }
    };
    
    private void connectToPolar() {
        Log.d(TAG, "🎯 Próba połączenia z " + POLAR_H10_MAC);
        statusText.setText("Polar: ⏳ Łączenie...");
        connectButton.setText("⏳ ŁĄCZENIE...");
        
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                statusText.setText("Polar: ❌ Błąd Bluetooth");
                connectButton.setText("🎯 POŁĄCZ");
                return;
            }
            
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(POLAR_H10_MAC);
            bluetoothGatt = device.connectGatt(this, false, gattCallback);
            
        } catch (Exception e) {
            Log.e(TAG, "Błąd łączenia: " + e.getMessage());
            statusText.setText("Polar: ❌ Błąd połączenia");
            connectButton.setText("🎯 POŁĄCZ");
        }
    }
    
    private void disconnectFromPolar() {
        Log.d(TAG, "❌ Rozłączanie");
        
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
        
        statusText.setText("Polar: Rozłączono");
        currentHeartRate = 0;
        updateHeartRateDisplay(0);
        resetZoneFeedback();
        connectButton.setText("🎯 POŁĄCZ");
    }
    
    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "✅ POŁĄCZONO z Polar H10!");
                
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        statusText.setText("Polar: ✅ Połączono, szukam HR...");
                        connectButton.setText("❌ ROZŁĄCZ");
                    }
                });
                
                // Szukam serwisów GATT
                gatt.discoverServices();
                
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "❌ ROZŁĄCZONO z Polar H10");
                
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        statusText.setText("Polar: Rozłączono");
                        currentHeartRate = 0;
                        updateHeartRateDisplay(0);
                        resetZoneFeedback();
                        connectButton.setText("🎯 POŁĄCZ");
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
                                statusText.setText("Polar: ✅ Gotowe - odbiera tętno");
                                // Komunikat głosowy po potwierdzeniu połączenia
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (isTtsReady) {
                                            speak("Połączono z paskiem Polar H10");
                                        }
                                    }
                                }, 500);
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
            // Tętno z Polar H10
            if (HEART_RATE_MEASUREMENT_CHAR_UUID.equals(characteristic.getUuid().toString())) {
                byte[] data = characteristic.getValue();
                int heartRate = parseHeartRateValue(data);
                
                Log.d(TAG, "❤️ Tętno: " + heartRate + " BPM");
                
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        currentHeartRate = heartRate;
                        updateHeartRateDisplay(heartRate);
                        updateHeartRateZoneUI();
                        
                        // Aktualizuj statystyki treningu biegowego
                        if (isRunningWorkoutActive) {
                            heartRateSum += heartRate;
                            heartRateCount++;
                            if (heartRate > maxHeartRate) {
                                maxHeartRate = heartRate;
                            }
                        }
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
    
    // Stara metoda usunięta - zastąpiona nowym systemem
    
    private void startWorkoutCountdown() {
        if (isWorkoutActive || isCountdownActive) return;
        
        Log.d(TAG, "🏃‍♂️ Rozpoczynam odliczanie CrossFit (10 sekund)");
        isCountdownActive = true;
        countdownSeconds = 30;
        currentRound = 1;
        isInWorkoutPhase = true;
        
        // Wyłącz przyciski ustawień
        timerTypeButton.setEnabled(false);
        startWorkoutButton.setText("🛑 ZATRZYMAJ (przytrzymaj 5s)");
        
        // Rozpocznij odliczanie
        countdownTick();
    }
    
    private void countdownTick() {
        if (countdownSeconds > 0) {
            workoutTimerText.setText("START za: " + countdownSeconds);
            
            // TTS odliczanie - tylko ostatnie 10 sekund
            if (countdownSeconds <= 10 && countdownSeconds > 5) {
                speak(String.valueOf(countdownSeconds));
            } else if (countdownSeconds <= 5) {
                speak(String.valueOf(countdownSeconds));
            }
            
            countdownSeconds--;
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    countdownTick();
                }
            }, 1000);
        } else {
            // Koniec odliczania - rozpocznij trening!
            workoutTimerText.setText("🔥 START! 🔥");
            speak("Start!");
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    startActualWorkout();
                }
            }, 500);
        }
    }
    
    private void startActualWorkout() {
        Log.d(TAG, "🔥 TRENING ROZPOCZĘTY - Runda 1/" + totalRounds);
        isCountdownActive = false;
        isWorkoutActive = true;
        setZoneMonitoringActive(true);
        
        // Włącz WakeLock - utrzyma CPU włączony
        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire();
            Log.d(TAG, "🔋 WakeLock włączony - CPU pozostanie aktywny");
        }
        
        // Zapisz i zwiększ głośność na MAX
        originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        Log.d(TAG, "🎧 PRZED: głośność=" + originalVolume + ", max=" + maxVolume);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0);
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        Log.d(TAG, "📢 PO: głośność ustawiona na " + currentVolume + " (max=" + maxVolume + "), poprzednia: " + originalVolume);
        
        // Ustaw czas dla pierwszej fazy (workout)
        workoutTimeLeftSeconds = workoutTimeMinutes * 60;
        
        // TTS start workout
        speak("Work! Runda 1");
        
        // Włącz przycisk STOP
        stopWorkoutButton.setEnabled(true);
        
        // Rozpocznij timer treningu
        workoutTick();
    }
    
    private void workoutTick() {
        if (isWorkoutActive && workoutTimeLeftSeconds > 0) {
            int minutes = workoutTimeLeftSeconds / 60;
            int seconds = workoutTimeLeftSeconds % 60;
            
            String phaseText = isInWorkoutPhase ? "🏋️ WORK" : "😌 REST";
            workoutTimerText.setText(String.format("%s: %02d:%02d (R%d/%d)", 
                phaseText, minutes, seconds, currentRound, totalRounds));
            
            // TTS ostrzeżenia
            if (isInWorkoutPhase && currentRound == totalRounds) {
                // Ostatnia runda - specjalne ostrzeżenia
                if (workoutTimeLeftSeconds == 60 && workoutTimeMinutes > 1) {
                    speak("Jedna minuta do końca treningu");
                }
            } else {
                // Normalne ostrzeżenia dla innych faz
                if (workoutTimeLeftSeconds == 10) {
                    String phaseTextTTS = isInWorkoutPhase ? "do resta" : "Jeszcze 10 sekund resta";
                    speak(isInWorkoutPhase ? "Dziesięć sekund " + phaseTextTTS : phaseTextTTS);
                }
            }
            
            workoutTimeLeftSeconds--;
            
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    workoutTick();
                }
            }, 1000);
        } else if (isWorkoutActive) {
            // Koniec bieżącej fazy
            switchWorkoutPhase();
        }
    }
    
    private void switchWorkoutPhase() {
        if (isInWorkoutPhase) {
            // Sprawdź czy to ostatnia runda - jeśli tak, zakończ trening
            if (currentRound >= totalRounds) {
                speak("Trening ukończony! Świetna robota!");
                finishWorkout();
                return;
            }
            
            // Przechodzę z WORK na REST
            if (restTimeSeconds > 0) {
                isInWorkoutPhase = false;
                workoutTimeLeftSeconds = restTimeSeconds;
                Log.d(TAG, "🔄 Przechodzę na REST - " + restTimeSeconds + "s");
                
                // TTS przejście na REST
                speak("Rest! " + getPolishTime(restTimeSeconds));
                
                workoutTick();
            } else {
                // Brak czasu odpoczynku, idę do następnej rundy
                nextRound();
            }
        } else {
            // Przechodzę z REST na następną rundę
            nextRound();
        }
    }
    
    private void nextRound() {
        currentRound++;
        if (currentRound <= totalRounds) {
            // Następna runda
            isInWorkoutPhase = true;
            workoutTimeLeftSeconds = workoutTimeMinutes * 60;
            Log.d(TAG, "🔄 Runda " + currentRound + "/" + totalRounds);
            
            // TTS nowa runda
            speak("Work! Runda " + currentRound);
            
            workoutTick();
        } else {
            // Koniec wszystkich rund!
            finishWorkout();
        }
    }
    
    private void startStopTimer() {
        if (!isWorkoutActive) return;
        
        isStopPressed = true;
        stopPressCounter = 5;
        startWorkoutButton.setText("🛑 ZATRZYMAJ " + stopPressCounter + "s");
        
        stopTimerTick();
    }
    
    private void stopTimerTick() {
        if (isStopPressed && stopPressCounter > 0) {
            stopPressCounter--;
            startWorkoutButton.setText("🛑 ZATRZYMAJ " + stopPressCounter + "s");
            
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
            startWorkoutButton.setText("🛑 ZATRZYMAJ (przytrzymaj 5s)");
            Log.d(TAG, "⚠️ Anulowano zatrzymanie treningu");
        }
    }
    
    private void stopWorkout() {
        Log.d(TAG, "🚫 TRENING ZATRZYMANY przez użytkownika");
        isWorkoutActive = false;
        setZoneMonitoringActive(false);
        isStopPressed = false;
        
        // Wyłącz WakeLock
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            Log.d(TAG, "🔋 WakeLock wyłączony");
        }
        
        // Przywróć pierwotną głośność
        if (originalVolume != -1) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0);
            Log.d(TAG, "🔊 Głośność przywrócona do: " + originalVolume);
            originalVolume = -1;
        }
        
        // TTS zatrzymanie
        speak("Trening zatrzymany");
        
        resetWorkoutUI();
    }
    
    private void finishWorkout() {
        Log.d(TAG, "✅ TRENING ZAKOŃCZONY - czas minął!");
        isWorkoutActive = false;
        setZoneMonitoringActive(false);
        
        // Wyłącz WakeLock
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            Log.d(TAG, "🔋 WakeLock wyłączony");
        }
        
        // Przywróć pierwotną głośność
        if (originalVolume != -1) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0);
            Log.d(TAG, "🔊 Głośność przywrócona do: " + originalVolume);
            originalVolume = -1;
        }
        
        workoutTimerText.setText("🎉 TRENING SKOŃCZONY! 🎉");
        
        // TTS koniec treningu
        speak("Trening ukończony! Świetna robota!");
        
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
        startWorkoutButton.setText("🏃‍♂️ START TRENINGU");
        
        // Reset zmiennych
        currentRound = 0;
        isInWorkoutPhase = true;
        
        // Włącz przyciski
        timerTypeButton.setEnabled(true);
        
        Log.d(TAG, "🔄 Interface zurückgesetzt für nächstes Training");
    }
    
    // ===== NOWE METODY DLA SYSTEMU TIMERÓW =====
    
    private void toggleTimerType() {
        if (selectedTimerType.equals("treningowy")) {
            selectedTimerType = "biegowy";
            timerTypeButton.setText("🏃 Timer Biegowy ▼");
            timerSettingsLayout.setVisibility(View.GONE);
            mainTimerDisplay.setText("60:00 ⏰");
            // Ukryj przyciski treningu treningowego
            startWorkoutButton.setVisibility(View.GONE);
            stopWorkoutButton.setVisibility(View.GONE);
            // workoutTimerText pozostaje widoczny - potrzebny dla GPS!
            workoutTimerText.setText("🏃‍♂️ Gotowy do treningu biegowego");
            showRunningWorkoutButton();
        } else {
            selectedTimerType = "treningowy";
            timerTypeButton.setText("🏋️‍♂️ Timer Treningowy ▼");
            timerSettingsLayout.setVisibility(View.VISIBLE);
            updateMainTimerDisplay();
            // Pokaż z powrotem przyciski treningu treningowego
            startWorkoutButton.setVisibility(View.VISIBLE);
            stopWorkoutButton.setVisibility(View.VISIBLE);
            workoutTimerText.setVisibility(View.VISIBLE);
            // Przywróć normalny tekst timera
            workoutTimerText.setText("⏱️ Timer: gotowy");
            hideRunningWorkoutButton();
        }
        Log.d(TAG, "Zmieniono typ timera na: " + selectedTimerType);
    }
    
    private void createRunningWorkoutButton() {
        runningWorkoutButton = new Button(this);
        runningWorkoutButton.setText("🏃 START TRENINGU");
        GradientDrawable runningGradient = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{Color.parseColor("#3d4a2c"), Color.parseColor("#5a6b47"), Color.parseColor("#3d4a2c")}
        );
        runningGradient.setCornerRadius(30);
        runningWorkoutButton.setBackground(runningGradient);
        runningWorkoutButton.setTextColor(0xFFFFFFFF); // Biały tekst
        runningWorkoutButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (isRunningWorkoutActive) {
                            // Trening aktywny - rozpocznij licznik zatrzymania
                            isStopRunningPressActive = true;
                            stopRunningPressStartTime = System.currentTimeMillis();
                            handler.post(stopRunningCountdownRunnable);
                        } else {
                            // Trening nieaktywny - natychmiastowy start
                            startRunningWorkout();
                            runningWorkoutButton.setText("🛑 ZATRZYMAJ");
                            GradientDrawable redGradient = new GradientDrawable(
                                GradientDrawable.Orientation.TL_BR,
                                new int[]{Color.parseColor("#3d4a2c"), Color.parseColor("#5a6b47"), Color.parseColor("#3d4a2c")}
                            );
                            redGradient.setCornerRadius(30);
                            runningWorkoutButton.setBackground(redGradient);
                        }
                        return true;
                        
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (isStopRunningPressActive) {
                            // Przerwano przytrzymanie
                            isStopRunningPressActive = false;
                            handler.removeCallbacks(stopRunningCountdownRunnable);
                            runningWorkoutButton.setText("🛑 ZATRZYMAJ");
                            GradientDrawable redGradient = new GradientDrawable(
                                GradientDrawable.Orientation.TL_BR,
                                new int[]{Color.parseColor("#3d4a2c"), Color.parseColor("#5a6b47"), Color.parseColor("#3d4a2c")}
                            );
                            redGradient.setCornerRadius(30);
                            runningWorkoutButton.setBackground(redGradient);
                        }
                        return true;
                }
                return false;
            }
        });
        runningWorkoutButton.setVisibility(View.GONE); // Ukryty na start
    }
    
    private void showRunningWorkoutButton() {
        if (runningWorkoutButton != null && mainLayout != null) {
            runningWorkoutButton.setVisibility(View.VISIBLE);
            if (runningWorkoutButton.getParent() == null) {
                // Znajdź pozycję przed przyciskiem zamknij
                int insertIndex = mainLayout.getChildCount() - 1; // Przed ostatnim przyciskiem
                mainLayout.addView(runningWorkoutButton, insertIndex);
            }
        }
    }
    
    private void hideRunningWorkoutButton() {
        if (runningWorkoutButton != null && mainLayout != null) {
            runningWorkoutButton.setVisibility(View.GONE);
            if (runningWorkoutButton.getParent() != null) {
                mainLayout.removeView(runningWorkoutButton);
            }
        }
    }
    
    private void updateMainTimerDisplay() {
        int totalSeconds = (workoutTimeMinutes * 60 + restTimeSeconds) * totalRounds;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        mainTimerDisplay.setText(String.format("GŁÓWNY: %d:%02d", minutes, seconds));
    }
    
    private void adjustWorkoutTime(int change) {
        workoutTimeMinutes += change;
        if (workoutTimeMinutes < 1) workoutTimeMinutes = 1;
        if (workoutTimeMinutes > 60) workoutTimeMinutes = 60;
        
        workoutTimeText.setText(workoutTimeMinutes + ":00");
        updateMainTimerDisplay();
        Log.d(TAG, "Workout time: " + workoutTimeMinutes + " min");
    }
    
    private void adjustRestTime(int changeSeconds) {
        restTimeSeconds += changeSeconds;
        if (restTimeSeconds < 0) restTimeSeconds = 0;
        if (restTimeSeconds > 1800) restTimeSeconds = 1800; // max 30min
        
        int restMin = restTimeSeconds / 60;
        int restSec = restTimeSeconds % 60;
        restTimeText.setText(String.format("%d:%02d", restMin, restSec));
        updateMainTimerDisplay();
        Log.d(TAG, "Rest time: " + restTimeSeconds + " seconds");
    }
    
    private void adjustRounds(int change) {
        totalRounds += change;
        if (totalRounds < 1) totalRounds = 1;
        if (totalRounds > 20) totalRounds = 20;
        
        roundsText.setText(String.valueOf(totalRounds));
        updateMainTimerDisplay();
        Log.d(TAG, "Rounds: " + totalRounds);
    }
    
    // ===== TTS METHODS =====
    
    private void initializeTTS() {
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    // Spróbuj użyć polskiego, jeśli dostępny
                    int result = tts.setLanguage(new Locale("pl", "PL"));
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.w(TAG, "⚠️ Polski TTS niedostępny, używam angielskiego");
                        tts.setLanguage(Locale.US);
                    }
                    isTtsReady = true;
                    speak("Gotowy do treningu!");
                    Log.d(TAG, "✅ TTS zainicjalizowany");
                } else {
                    Log.e(TAG, "❌ Błąd inicjalizacji TTS");
                }
            }
        });
    }
    
    private void speak(String text) {
        speakInternal(text, false);
    }

    private void speakZone(String text) {
        speakInternal(text, true);
    }

    private void speakInternal(String text, boolean zoneMessage) {
        if (isTtsReady && tts != null) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);

            int queueMode = TextToSpeech.QUEUE_ADD;
            String utteranceId = (zoneMessage ? "ZONE_" : "GEN_") + System.currentTimeMillis();
            tts.speak(text, queueMode, null, utteranceId);
            long now = System.currentTimeMillis();
            if (zoneMessage) {
                lastZoneSpeakTimestamp = now;
            } else {
                lastTimerSpeakTimestamp = now;
            }
            Log.d(TAG, "🔊 TTS: " + text);
        } else {
            Log.w(TAG, "⚠️ TTS nie gotowy: " + text);
        }
    }

    private void showHrZoneSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Strefy tętna");

        ScrollView dialogScroll = new ScrollView(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(40, 30, 40, 30);

        TextView intro = new TextView(this);
        intro.setText("Dostosuj HRMAX i progi stref. Zmiana możliwa co 5 sekund, aby uniknąć przypadkowych kliknięć.");
        intro.setTextSize(14);
        intro.setPadding(0, 0, 0, 20);
        container.addView(intro);

        final List<Button> adjustmentButtons = new ArrayList<>();

        final TextView hrValueView = new TextView(this);
        final TextView comfortValueView = new TextView(this);
        final TextView aerobicValueView = new TextView(this);
        final TextView alarmValueView = new TextView(this);

        container.addView(createZoneSettingRow("HRMAX", hrValueView,
            () -> adjustHrMax(-1, adjustmentButtons, hrValueView, comfortValueView, aerobicValueView, alarmValueView),
            () -> adjustHrMax(1, adjustmentButtons, hrValueView, comfortValueView, aerobicValueView, alarmValueView),
            adjustmentButtons));

        container.addView(createZoneSettingRow("Strefa komfortowa", comfortValueView,
            () -> adjustComfortZone(-1, adjustmentButtons, hrValueView, comfortValueView, aerobicValueView, alarmValueView),
            () -> adjustComfortZone(1, adjustmentButtons, hrValueView, comfortValueView, aerobicValueView, alarmValueView),
            adjustmentButtons));

        container.addView(createZoneSettingRow("Strefa tlenowa", aerobicValueView,
            () -> adjustAerobicZone(-1, adjustmentButtons, hrValueView, comfortValueView, aerobicValueView, alarmValueView),
            () -> adjustAerobicZone(1, adjustmentButtons, hrValueView, comfortValueView, aerobicValueView, alarmValueView),
            adjustmentButtons));

        container.addView(createZoneSettingRow("Strefa alarmowa", alarmValueView,
            () -> adjustAlarmZone(-1, adjustmentButtons, hrValueView, comfortValueView, aerobicValueView, alarmValueView),
            () -> adjustAlarmZone(1, adjustmentButtons, hrValueView, comfortValueView, aerobicValueView, alarmValueView),
            adjustmentButtons));

        refreshZoneSettingsViews(hrValueView, comfortValueView, aerobicValueView, alarmValueView);

        dialogScroll.addView(container);
        builder.setView(dialogScroll);
        builder.setPositiveButton("Zamknij", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private LinearLayout createZoneSettingRow(String label, TextView valueView,
                                              Runnable onDecrease, Runnable onIncrease,
                                              List<Button> buttonCollector) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 10, 0, 10);

        TextView name = new TextView(this);
        name.setText(label);
        name.setTextSize(16);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        name.setLayoutParams(nameParams);
        row.addView(name);

        Button minusButton = createZoneAdjustButton("➖");
        minusButton.setOnClickListener(v -> onDecrease.run());
        row.addView(minusButton);
        buttonCollector.add(minusButton);

        valueView.setTextSize(18);
        valueView.setPadding(30, 0, 30, 0);
        row.addView(valueView);

        Button plusButton = createZoneAdjustButton("➕");
        plusButton.setOnClickListener(v -> onIncrease.run());
        row.addView(plusButton);
        buttonCollector.add(plusButton);

        return row;
    }

    private Button createZoneAdjustButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        GradientDrawable gradient = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{Color.parseColor("#3d4a2c"), Color.parseColor("#5a6b47"), Color.parseColor("#3d4a2c")}
        );
        gradient.setCornerRadius(30);
        button.setBackground(gradient);
        button.setTextColor(0xFFFFFFFF);
        button.setPadding(40, 10, 40, 10);
        button.setMinWidth(140);
        return button;
    }

    private void adjustHrMax(int delta, List<Button> buttons, TextView hrValueView,
                             TextView comfortValueView, TextView aerobicValueView, TextView alarmValueView) {
        handleZoneSettingsChange(() -> {
            hrMaxValue += delta;
            if (hrMaxValue < HR_MAX_MIN) hrMaxValue = HR_MAX_MIN;
            if (hrMaxValue > HR_MAX_MAX) hrMaxValue = HR_MAX_MAX;
            ensureZoneThresholdsIntegrity();
            saveHrZonePreferences();
            updateHrSettingsButtonLabel();
            refreshZoneSettingsViews(hrValueView, comfortValueView, aerobicValueView, alarmValueView);
            updateHeartRateDisplay(currentHeartRate);
            updateHeartRateZoneUI();
        }, buttons);
    }

    private void adjustComfortZone(int delta, List<Button> buttons, TextView hrValueView,
                                   TextView comfortValueView, TextView aerobicValueView, TextView alarmValueView) {
        handleZoneSettingsChange(() -> {
            comfortZonePercent += delta;
            ensureZoneThresholdsIntegrity();
            saveHrZonePreferences();
            refreshZoneSettingsViews(hrValueView, comfortValueView, aerobicValueView, alarmValueView);
            updateHeartRateZoneUI();
        }, buttons);
    }

    private void adjustAerobicZone(int delta, List<Button> buttons, TextView hrValueView,
                                   TextView comfortValueView, TextView aerobicValueView, TextView alarmValueView) {
        handleZoneSettingsChange(() -> {
            aerobicZonePercent += delta;
            ensureZoneThresholdsIntegrity();
            saveHrZonePreferences();
            refreshZoneSettingsViews(hrValueView, comfortValueView, aerobicValueView, alarmValueView);
            updateHeartRateZoneUI();
        }, buttons);
    }

    private void adjustAlarmZone(int delta, List<Button> buttons, TextView hrValueView,
                                 TextView comfortValueView, TextView aerobicValueView, TextView alarmValueView) {
        handleZoneSettingsChange(() -> {
            alarmZonePercent += delta;
            ensureZoneThresholdsIntegrity();
            saveHrZonePreferences();
            refreshZoneSettingsViews(hrValueView, comfortValueView, aerobicValueView, alarmValueView);
            updateHeartRateZoneUI();
        }, buttons);
    }

    private void refreshZoneSettingsViews(TextView hrValueView, TextView comfortValueView,
                                          TextView aerobicValueView, TextView alarmValueView) {
        if (hrValueView != null) {
            hrValueView.setText(hrMaxValue + " BPM");
        }
        if (comfortValueView != null) {
            comfortValueView.setText(formatZoneValue(comfortZonePercent));
        }
        if (aerobicValueView != null) {
            aerobicValueView.setText(formatZoneValue(aerobicZonePercent));
        }
        if (alarmValueView != null) {
            alarmValueView.setText(formatZoneValue(alarmZonePercent));
        }
    }

    private String formatZoneValue(int percent) {
        int bpm = Math.round(hrMaxValue * percent / 100f);
        return percent + "% (" + bpm + " BPM)";
    }

    private void handleZoneSettingsChange(Runnable changeAction, List<Button> buttons) {
        long now = System.currentTimeMillis();
        if (now - lastZoneSettingsChangeTime < 5000) {
            if (statusText != null) {
                statusText.setText("⏳ Odczekaj 5 sekund przed kolejną zmianą stref.");
            }
            return;
        }
        changeAction.run();
        lastZoneSettingsChangeTime = now;
        for (Button button : buttons) {
            button.setEnabled(false);
        }
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                for (Button button : buttons) {
                    button.setEnabled(true);
                }
            }
        }, 5000);
    }

    private void loadHrZonePreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        hrMaxValue = prefs.getInt(PREF_HR_MAX, DEFAULT_HR_MAX);
        comfortZonePercent = prefs.getInt(PREF_ZONE_COMFORT, DEFAULT_ZONE_COMFORT);
        aerobicZonePercent = prefs.getInt(PREF_ZONE_AEROBIC, DEFAULT_ZONE_AEROBIC);
        alarmZonePercent = prefs.getInt(PREF_ZONE_ALARM, DEFAULT_ZONE_ALARM);
        ensureZoneThresholdsIntegrity();
    }

    private void saveHrZonePreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit()
            .putInt(PREF_HR_MAX, hrMaxValue)
            .putInt(PREF_ZONE_COMFORT, comfortZonePercent)
            .putInt(PREF_ZONE_AEROBIC, aerobicZonePercent)
            .putInt(PREF_ZONE_ALARM, alarmZonePercent)
            .apply();
    }

    private void ensureZoneThresholdsIntegrity() {
        if (hrMaxValue < HR_MAX_MIN) hrMaxValue = HR_MAX_MIN;
        if (hrMaxValue > HR_MAX_MAX) hrMaxValue = HR_MAX_MAX;

        if (comfortZonePercent < ZONE_PERCENT_MIN) comfortZonePercent = ZONE_PERCENT_MIN;
        if (comfortZonePercent > ZONE_PERCENT_MAX - 3) comfortZonePercent = ZONE_PERCENT_MAX - 3;

        if (aerobicZonePercent <= comfortZonePercent) {
            aerobicZonePercent = Math.min(comfortZonePercent + 1, ZONE_PERCENT_MAX - 1);
        }
        if (aerobicZonePercent > ZONE_PERCENT_MAX - 1) {
            aerobicZonePercent = ZONE_PERCENT_MAX - 1;
        }

        if (alarmZonePercent <= aerobicZonePercent) {
            alarmZonePercent = Math.min(aerobicZonePercent + 1, ZONE_PERCENT_MAX);
        }
        if (alarmZonePercent > ZONE_PERCENT_MAX) {
            alarmZonePercent = ZONE_PERCENT_MAX;
        }
    }

    private void updateHrSettingsButtonLabel() {
        if (hrSettingsButton != null) {
            hrSettingsButton.setText(getHrSettingsButtonLabel());
        }
    }

    private String getHrSettingsButtonLabel() {
        return String.format(Locale.getDefault(), "HRMAX:: %d", hrMaxValue);
    }

    private void updateHeartRateDisplay(int heartRate) {
        if (heartRateText == null) {
            return;
        }
        float percent = hrMaxValue > 0 ? (heartRate * 100f) / hrMaxValue : 0f;
        if (percent < 0) {
            percent = 0;
        }
        int percentInt = Math.round(percent);
        String suffix = hrMaxValue > 0 ? " (" + percentInt + "% HRMAX)" : "";
        heartRateText.setText("❤️ Tętno: " + heartRate + " BPM" + suffix);
    }

    private void updateHeartRateZoneUI() {
        if (!isZoneMonitoringActive) {
            return;
        }
        if (zoneBackgroundTarget == null) {
            return;
        }
        if (currentHeartRate <= 0 || hrMaxValue <= 0) {
            resetZoneFeedback();
            return;
        }

        ensureZoneThresholdsIntegrity();

        float percent = (currentHeartRate * 100f) / (float) hrMaxValue;
        float comfort = comfortZonePercent;
        float warningUpper = Math.min(comfort + ZONE_WARNING_WINDOW, aerobicZonePercent);
        float aerobic = aerobicZonePercent;
        float alarm = alarmZonePercent;

        int newZone = determineCurrentZone(percent, comfort, warningUpper, aerobic, alarm);
        currentZoneState = newZone;
        applyZoneBackground(newZone, percent, comfort, warningUpper, aerobic, alarm);
        maybeAnnounceZone(newZone);
    }

    private int determineCurrentZone(float percent, float comfort, float warningUpper, float aerobic, float alarm) {
        if (percent <= comfort) {
            return 0;
        }
        if (warningUpper > comfort && percent <= warningUpper) {
            return 1;
        }
        if (percent < alarm) {
            return 2;
        }
        return 3;
    }

    private void applyZoneBackground(int zone, float percent, float comfort, float warningUpper, float aerobic, float alarm) {
        switch (zone) {
            case 0:
                stopZoneBlinking();
                setZoneBackgroundColor(Color.parseColor("#1B5E20"));
                break;
            case 1:
                startZoneBlinking(Color.parseColor("#2E7D32"), Color.parseColor("#1B5E20"));
                break;
            case 2:
                stopZoneBlinking();
                float span = Math.max(1f, alarm - warningUpper);
                float ratio = (percent - warningUpper) / span;
                if (ratio < 0f) ratio = 0f;
                if (ratio > 1f) ratio = 1f;
                int startColor = Color.parseColor("#2ECC71");
                int endColor = Color.parseColor("#E67E22");
                int blended = blendColors(startColor, endColor, ratio);
                setZoneBackgroundColor(blended);
                break;
            case 3:
                startZoneBlinking(Color.parseColor("#C0392B"), Color.parseColor("#922B21"));
                break;
            default:
                stopZoneBlinking();
                setZoneBackgroundColor(defaultBackgroundColor);
                break;
        }
    }

    private void startZoneBlinking(int primaryColor, int secondaryColor) {
        if (zoneBackgroundTarget == null) {
            return;
        }
        if (isZoneBlinking && zoneBlinkPrimaryColor == primaryColor && zoneBlinkSecondaryColor == secondaryColor) {
            return;
        }
        stopZoneBlinking();
        isZoneBlinking = true;
        zoneBlinkPrimaryColor = primaryColor;
        zoneBlinkSecondaryColor = secondaryColor;
        zoneBlinkState = false;
        if (zoneBlinkRunnable == null) {
            zoneBlinkRunnable = new Runnable() {
                @Override
                public void run() {
                    if (!isZoneBlinking) {
                        return;
                    }
                    zoneBlinkState = !zoneBlinkState;
                    setZoneBackgroundColor(zoneBlinkState ? zoneBlinkPrimaryColor : zoneBlinkSecondaryColor);
                    handler.postDelayed(zoneBlinkRunnable, 500);
                }
            };
        }
        handler.post(zoneBlinkRunnable);
    }

    private void stopZoneBlinking() {
        if (zoneBlinkRunnable != null) {
            handler.removeCallbacks(zoneBlinkRunnable);
        }
        isZoneBlinking = false;
        zoneBlinkState = false;
    }

    private void setZoneBackgroundColor(int color) {
        if (zoneBackgroundTarget != null) {
            zoneBackgroundTarget.setBackgroundColor(color);
        }
    }

    private int blendColors(int colorFrom, int colorTo, float ratio) {
        int alpha = (int) (Color.alpha(colorFrom) + ratio * (Color.alpha(colorTo) - Color.alpha(colorFrom)));
        int red = (int) (Color.red(colorFrom) + ratio * (Color.red(colorTo) - Color.red(colorFrom)));
        int green = (int) (Color.green(colorFrom) + ratio * (Color.green(colorTo) - Color.green(colorFrom)));
        int blue = (int) (Color.blue(colorFrom) + ratio * (Color.blue(colorTo) - Color.blue(colorFrom)));
        return Color.argb(alpha, red, green, blue);
    }

    private void maybeAnnounceZone(int zone) {
        if (!isZoneMonitoringActive) {
            return;
        }
        if (!isTtsReady || tts == null) {
            return;
        }

        long now = System.currentTimeMillis();

        if (zone == 0) {
            if (lastZoneAnnounced != zone) {
                lastZoneAnnounced = zone;
                lastZoneAnnouncementTime = now;
                return;
            }
            if (now - lastZoneAnnouncementTime < 15000) {
                return;
            }
            if (now - lastTimerSpeakTimestamp < 2000 || now - lastZoneSpeakTimestamp < 5000) {
                return;
            }
            if (tts.isSpeaking()) {
                return;
            }
            speakZone("Przyspiesz!!!");
            lastZoneAnnouncementTime = now;
        } else if (zone == 3) {
            if (lastZoneAnnounced != zone) {
                lastZoneAnnounced = zone;
                lastZoneAnnouncementTime = now;
                return;
            }
            if (now - lastZoneAnnouncementTime < 5000) {
                return;
            }
            if (now - lastTimerSpeakTimestamp < 2000 || now - lastZoneSpeakTimestamp < 3000) {
                return;
            }
            if (tts.isSpeaking()) {
                return;
            }
            speakZone("Zwolnij, jesteś poza strefą pracy!!!");
            lastZoneAnnouncementTime = now;
        } else {
            lastZoneAnnounced = -1;
            lastZoneAnnouncementTime = 0L;
        }
    }

    private void resetZoneFeedback() {
        stopZoneBlinking();
        setZoneBackgroundColor(defaultBackgroundColor);
        currentZoneState = -1;
        lastZoneAnnounced = -1;
        lastZoneAnnouncementTime = 0L;
        lastZoneSpeakTimestamp = 0L;
    }

    private void setZoneMonitoringActive(boolean active) {
        if (isZoneMonitoringActive == active) {
            return;
        }
        isZoneMonitoringActive = active;
        if (active) {
            lastZoneAnnounced = -1;
            lastZoneAnnouncementTime = 0L;
            lastZoneSpeakTimestamp = 0L;
            updateHeartRateZoneUI();
        } else {
            resetZoneFeedback();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Przywróć pierwotną głośność TYLKO gdy trening NIE jest aktywny
        if (!isWorkoutActive && !isRunningWorkoutActive && originalVolume != -1) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0);
            Log.d(TAG, "🔊 PAUSE: Głośność przywrócona do: " + originalVolume);
        } else {
            Log.d(TAG, "🔊 PAUSE: Trening aktywny - głośność nie zmieniona");
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnectFromPolar();
        
        // Zwolnij WakeLock
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            Log.d(TAG, "🔋 WakeLock zwolniony");
        }
        
        // Przywróć pierwotną głośność przy zamknięciu aplikacji
        if (originalVolume != -1) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0);
            Log.d(TAG, "🔊 ZAMKNIĘCIE: Głośność przywrócona do: " + originalVolume);
        }
        
        // Zwolnij TTS
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
    
    // ===== HELPER METHODS FOR POLISH TTS =====
    
    private String getPolishMinutes(int minutes) {
        if (minutes == 1) return "jedna minuta";
        if (minutes == 2) return "dwie minuty";
        if (minutes == 3) return "trzy minuty";
        if (minutes == 4) return "cztery minuty";
        if (minutes == 5) return "pięć minut";
        if (minutes <= 10) return minutes + " minut";
        return minutes + " minut";
    }
    
    private String getEnglishTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        
        if (minutes == 0) {
            return seconds + " seconds";
        }
        
        String result = minutes + " minute";
        if (minutes > 1) result += "s";
        
        if (seconds > 0) {
            result += " " + seconds + " seconds";
        }
        
        return result;
    }
    
    private String getPolishTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        
        if (minutes == 0) {
            if (seconds == 1) return "jedna sekunda";
            if (seconds <= 4) return seconds + " sekundy";
            return seconds + " sekund";
        }
        
        String result = "";
        if (minutes == 1) result = "jedna minuta";
        else if (minutes == 2) result = "dwie minuty";
        else if (minutes <= 4) result = minutes + " minuty";
        else result = minutes + " minut";
        
        if (seconds > 0) {
            if (seconds == 1) result += " jedna sekunda";
            else if (seconds <= 4) result += " " + seconds + " sekundy";
            else result += " " + seconds + " sekund";
        }
        
        return result;
    }
    
    private String getPolishRoundNumber(int round) {
        switch (round) {
            case 1: return "pierwsza";
            case 2: return "druga";
            case 3: return "trzecia";
            case 4: return "czwarta";
            case 5: return "piąta";
            case 6: return "szósta";
            case 7: return "siódma";
            case 8: return "ósma";
            case 9: return "dziewiąta";
            case 10: return "dziesiąta";
            default: return String.valueOf(round);
        }
    }
    
    // ===== TRENING BIEGOWY - GPS TRACKING =====
    
    private void startRunningWorkout() {
        if (isRunningWorkoutActive) return;
        
        // Sprawdź uprawnienia GPS
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 200);
            return;
        }
        
        // Inicjalizuj LocationManager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        
        // Sprawdź czy GPS jest włączony
        if (locationManager == null || !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            speak("Ostrzeżenie! GPS nie jest włączony. Włącz GPS w ustawieniach.");
            workoutTimerText.setText("⚠️ GPS NIE WŁĄCZONY!\nWłącz GPS w ustawieniach telefonu");
            Log.e(TAG, "❌ GPS_PROVIDER nie jest dostępny lub wyłączony!");
            return;
        }
        
        Log.d(TAG, "🏃‍♂️ ROZPOCZYNAM TRENING BIEGOWY z GPS");
        isRunningWorkoutActive = true;
        setZoneMonitoringActive(true);
        
        // Włącz WakeLock - utrzyma CPU włączony
        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire();
            Log.d(TAG, "🔋 WakeLock włączony - GPS i CPU pozostaną aktywne");
        }
        
        runningStartTime = System.currentTimeMillis();
        totalDistance = 0.0;
        lastLocation = null;
        maxSpeed = 0.0;
        heartRateSum = 0;
        heartRateCount = 0;
        maxHeartRate = 0;
        gpsTrack.clear();
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                updateRunningStats(location);
            }
            
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}
            
            @Override
            public void onProviderEnabled(String provider) {}
            
            @Override
            public void onProviderDisabled(String provider) {
                Log.w(TAG, "⚠️ GPS został wyłączony podczas treningu!");
                speak("Ostrzeżenie! GPS został wyłączony");
            }
        };
        
        // Rozpocznij śledzenie GPS
        // Parametry: provider, minTime (ms), minDistance (m), listener
        // 500ms = co pół sekundy, 0m = każda zmiana pozycji
        try {
            // Dodaj oba providery dla lepszego fix'a
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0, locationListener);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 500, 0, locationListener);
            Log.d(TAG, "📍 GPS + Network tracking włączony: 500ms, 0m minDistance");
            Log.d(TAG, "📍 LocationListener: " + locationListener);
            
            // Próba pobrać ostatnią znaną lokalizację
            Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnownLocation != null) {
                Log.d(TAG, "📍 Ostatnia znana pozycja GPS: " + lastKnownLocation.getLatitude() + ", " + lastKnownLocation.getLongitude());
                speak("Trening biegowy rozpoczęty. GPS aktywny");
            } else {
                lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (lastKnownLocation != null) {
                    Log.d(TAG, "📍 Ostatnia znana pozycja Network: " + lastKnownLocation.getLatitude() + ", " + lastKnownLocation.getLongitude());
                } else {
                    Log.d(TAG, "📍 Brak ostatniej znanej pozycji - oczekiwanie na GPS fix");
                }
                speak("Trening biegowy rozpoczęty. Ustalanie pozycji GPS");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "❌ Błąd uprawnień GPS: " + e.getMessage());
            speak("Błąd! Brak uprawnień GPS");
            isRunningWorkoutActive = false;
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
            return;
        }
        
        // Komunikat już został wywołany wyżej (zależnie od lastKnownLocation)
        updateRunningInterface();
    }
    
    private void updateRunningStats(Location location) {
        if (!isRunningWorkoutActive) return;
        
        Log.d(TAG, "📍 GPS Update RECEIVED: lat=" + location.getLatitude() + ", lon=" + location.getLongitude() + 
            ", accuracy=" + location.getAccuracy() + "m, speed=" + location.getSpeed() + "m/s, provider=" + location.getProvider());
        
        // Zapisz punkt GPS do trasy
        double elevation = location.hasAltitude() ? location.getAltitude() : 0.0;
        TrackPoint point = new TrackPoint(
            location.getLatitude(),
            location.getLongitude(),
            elevation,
            location.getTime(),
            currentHeartRate
        );
        gpsTrack.add(point);
        Log.d(TAG, "✅ Punkt GPS dodany do trasy (total: " + gpsTrack.size() + ")");
        
        if (lastLocation != null) {
            // Oblicz dystans między punktami
            float distance = lastLocation.distanceTo(location);
            
            // Ignoruj małe ruchy (szum GPS) - tylko jeśli accuracy jest dobry
            if (distance > 2 || location.getAccuracy() < 10) {
                totalDistance += distance;
                Log.d(TAG, "📏 Dodano dystans: " + distance + "m, total: " + (totalDistance/1000) + "km");
            } else {
                Log.d(TAG, "⏭️ Pominięto mały ruch: " + distance + "m (accuracy: " + location.getAccuracy() + "m)");
            }
            
            // Oblicz prędkość
            if (location.hasSpeed()) {
                double speedKmh = location.getSpeed() * 3.6; // m/s to km/h
                if (speedKmh > maxSpeed) {
                    maxSpeed = speedKmh;
                }
            }
        }
        lastLocation = location;
        
        // Aktualizuj interfejs co 1 sekundę
        updateRunningInterface();
    }
    
    private void updateRunningInterface() {
        if (!isRunningWorkoutActive) return;
        
        long elapsedTime = System.currentTimeMillis() - runningStartTime;
        long minutes = elapsedTime / 60000;
        long seconds = (elapsedTime % 60000) / 1000;
        
        double distanceKm = totalDistance / 1000.0;
        String pace = calculatePace(distanceKm, elapsedTime);
        
        String stats = String.format(
            "🏃‍♂️ TRENING BIEGOWY AKTYWNY\n" +
            "💓 Puls: %d bpm\n" +
            "📏 Dystans: %.2f km\n" +
            "⚡ Tempo: %s min/km\n" +
            "⏱️ Czas: %02d:%02d",
            currentHeartRate,
            distanceKm,
            pace,
            minutes, seconds
        );
        
        workoutTimerText.setText(stats);
        Log.d(TAG, "🔄 GPS Update: " + minutes + ":" + String.format("%02d", seconds) + " | " + String.format("%.2f", distanceKm) + "km");
        
        // Planuj następną aktualizację za 1 sekundę
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateRunningInterface();
            }
        }, 1000);
    }
    
    private String calculatePace(double distanceKm, long elapsedTimeMs) {
        if (distanceKm <= 0) return "0:00";
        
        double elapsedMinutes = elapsedTimeMs / 60000.0;
        double paceMinPerKm = elapsedMinutes / distanceKm;
        
        int minutes = (int) paceMinPerKm;
        int seconds = (int) ((paceMinPerKm - minutes) * 60);
        
        return String.format("%d:%02d", minutes, seconds);
    }
    
    private void stopRunningWorkout() {
        if (!isRunningWorkoutActive) return;
        
        Log.d(TAG, "🛑 ZATRZYMUJĘ TRENING BIEGOWY");
        isRunningWorkoutActive = false;
        setZoneMonitoringActive(false);
        
        // Wyłącz WakeLock
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            Log.d(TAG, "🔋 WakeLock wyłączony");
        }
        
        // Zatrzymaj GPS
        if (locationManager != null && locationListener != null) {
            try {
                locationManager.removeUpdates(locationListener);
                Log.d(TAG, "📍 GPS tracking zatrzymany");
            } catch (SecurityException e) {
                Log.e(TAG, "❌ Błąd zatrzymania GPS: " + e.getMessage());
            }
        }
        
        // Pokaż raport
        showRunningWorkoutReport();
    }
    
    private void showRunningWorkoutReport() {
        long elapsedTime = System.currentTimeMillis() - runningStartTime;
        long minutes = elapsedTime / 60000;
        long seconds = (elapsedTime % 60000) / 1000;
        
        double distanceKm = totalDistance / 1000.0;
        String avgPace = calculatePace(distanceKm, elapsedTime);
        double avgHeartRate = heartRateCount > 0 ? (double) heartRateSum / heartRateCount : 0;
        
        String report = String.format(
            "🎉 RAPORT TRENINGU BIEGOWEGO\n" +
            "📅 Data: %s\n" +
            "⏱️ Czas: %02d:%02d\n" +
            "📏 Dystans: %.2f km\n" +
            "⚡ Średnie tempo: %s min/km\n" +
            "🏃‍♂️ Maks prędkość: %.1f km/h\n" +
            "💓 Średni puls: %.0f bpm\n" +
            "💓 Maks puls: %d bpm",
            new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(new Date()),
            minutes, seconds,
            distanceKm,
            avgPace,
            maxSpeed,
            avgHeartRate,
            maxHeartRate
        );
        
        workoutTimerText.setText(report);
        speak("Trening biegowy zakończony");
        
        // Zapisz do plików
        saveRunningReport(report);
        saveGPXTrack();
    }
    
    private void saveRunningReport(String report) {
        try {
            // Utwórz folder PolarH10 w Documents
            File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            File polarDir = new File(documentsDir, "PolarH10");
            if (!polarDir.exists()) {
                polarDir.mkdirs();
            }
            
            // Nazwa pliku z datą
            String fileName = "trening_biegowy_" + 
                new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(new Date()) + ".txt";
            File reportFile = new File(polarDir, fileName);
            
            // Zapisz raport
            FileWriter writer = new FileWriter(reportFile);
            writer.write(report);
            writer.close();
            
            Log.d(TAG, "💾 Raport zapisany: " + reportFile.getAbsolutePath());
            
        } catch (IOException e) {
            Log.e(TAG, "❌ Błąd zapisu raportu: " + e.getMessage());
        }
    }
    
    private void saveGPXTrack() {
        if (gpsTrack.isEmpty()) {
            Log.w(TAG, "⚠️ Brak punktów GPS do zapisania");
            return;
        }
        
        try {
            File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            File polarDir = new File(documentsDir, "PolarH10");
            if (!polarDir.exists()) {
                polarDir.mkdirs();
            }
            
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(new Date());
            String fileName = "trening_biegowy_" + timestamp + ".gpx";
            File gpxFile = new File(polarDir, fileName);
            
            // Generuj GPX XML
            StringBuilder gpx = new StringBuilder();
            gpx.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            gpx.append("<gpx version=\"1.1\" creator=\"PolarH10 Workout Tracker\"\n");
            gpx.append("  xmlns=\"http://www.topografix.com/GPX/1/1\"\n");
            gpx.append("  xmlns:gpxtpx=\"http://www.garmin.com/xmlschemas/TrackPointExtension/v1\">\n");
            gpx.append("  <metadata>\n");
            gpx.append("    <name>Trening Biegowy ").append(timestamp).append("</name>\n");
            gpx.append("    <time>").append(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date())).append("</time>\n");
            gpx.append("  </metadata>\n");
            gpx.append("  <trk>\n");
            gpx.append("    <name>Trening Biegowy</name>\n");
            gpx.append("    <trkseg>\n");
            
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
            isoFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            
            for (TrackPoint point : gpsTrack) {
                gpx.append("      <trkpt lat=\"").append(point.latitude).append("\" lon=\"").append(point.longitude).append("\">\n");
                gpx.append("        <ele>").append(point.elevation).append("</ele>\n");
                gpx.append("        <time>").append(isoFormat.format(new Date(point.timestamp))).append("</time>\n");
                
                if (point.heartRate > 0) {
                    gpx.append("        <extensions>\n");
                    gpx.append("          <gpxtpx:TrackPointExtension>\n");
                    gpx.append("            <gpxtpx:hr>").append(point.heartRate).append("</gpxtpx:hr>\n");
                    gpx.append("          </gpxtpx:TrackPointExtension>\n");
                    gpx.append("        </extensions>\n");
                }
                
                gpx.append("      </trkpt>\n");
            }
            
            gpx.append("    </trkseg>\n");
            gpx.append("  </trk>\n");
            gpx.append("</gpx>\n");
            
            // Zapisz plik GPX
            FileWriter writer = new FileWriter(gpxFile);
            writer.write(gpx.toString());
            writer.close();
            
            // Zapisz ścieżkę
            final String savedPath = gpxFile.getAbsolutePath();
            lastGpxPath = savedPath;
            
            Log.d(TAG, "💾 Trasa GPX zapisana: " + savedPath + " (" + gpsTrack.size() + " punktów)");
            
            // Pokaż przycisk na wątku UI
            handler.post(new Runnable() {
                @Override
                public void run() {
                    showMapButton.setVisibility(View.VISIBLE);
                    Log.d(TAG, "✅ Przycisk POKAŻ TRASĘ widoczny");
                    
                    // Przewiń ScrollView na dół żeby pokazać przycisk
                    scrollView.post(new Runnable() {
                        @Override
                        public void run() {
                            scrollView.fullScroll(View.FOCUS_DOWN);
                            Log.d(TAG, "📜 ScrollView przewinięty na dół");
                        }
                    });
                }
            });
            
            speak("Trasa GPS zapisana. " + gpsTrack.size() + " punktów");
            
        } catch (IOException e) {
            Log.e(TAG, "❌ Błąd zapisu GPX: " + e.getMessage());
        }
    }
}