package com.example.polarh10;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polyline;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class MapActivity extends Activity {
    
    private static final String TAG = "MapActivity";
    private MapView mapView;
    private TextView statsText;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Konfiguracja osmdroid
        Configuration.getInstance().setUserAgentValue(getPackageName());
        
        // Layout główny
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(20, 20, 20, 20);
        mainLayout.setBackgroundColor(0xFF1a1a1a);
        
        // Tytuł
        TextView titleText = new TextView(this);
        titleText.setText("🗺️ TRASA TRENINGU");
        titleText.setTextSize(24);
        titleText.setTextColor(0xFFFFFFFF);
        titleText.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
        mainLayout.addView(titleText);
        
        // Statystyki
        statsText = new TextView(this);
        statsText.setTextSize(14);
        statsText.setTextColor(0xFFFFFFFF);
        statsText.setPadding(10, 10, 10, 10);
        mainLayout.addView(statsText);
        
        // Mapa
        mapView = new MapView(this);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(15.0);
        
        LinearLayout.LayoutParams mapParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            0
        );
        mapParams.weight = 1;
        mapView.setLayoutParams(mapParams);
        mainLayout.addView(mapView);
        
        // Przycisk zamknij
        Button closeButton = new Button(this);
        closeButton.setText("🔙 POWRÓT");
        closeButton.setTextSize(16);
        closeButton.setOnClickListener(v -> finish());
        mainLayout.addView(closeButton);
        
        setContentView(mainLayout);
        
        // Załaduj trasę
        String gpxPath = getIntent().getStringExtra("GPX_PATH");
        if (gpxPath != null) {
            loadGPXTrack(gpxPath);
        } else {
            statsText.setText("❌ Brak pliku GPX");
        }
    }
    
    private void loadGPXTrack(String filePath) {
        try {
            File gpxFile = new File(filePath);
            if (!gpxFile.exists()) {
                statsText.setText("❌ Plik nie istnieje: " + filePath);
                return;
            }
            
            List<GeoPoint> trackPoints = new ArrayList<>();
            BufferedReader reader = new BufferedReader(new FileReader(gpxFile));
            String lineText;
            int pointCount = 0;
            double totalDistance = 0.0;
            GeoPoint lastPoint = null;
            
            while ((lineText = reader.readLine()) != null) {
                if (lineText.contains("<trkpt")) {
                    // Parsuj współrzędne
                    int latStart = lineText.indexOf("lat=\"") + 5;
                    int latEnd = lineText.indexOf("\"", latStart);
                    int lonStart = lineText.indexOf("lon=\"") + 5;
                    int lonEnd = lineText.indexOf("\"", lonStart);
                    
                    if (latStart > 4 && lonStart > 4) {
                        double lat = Double.parseDouble(lineText.substring(latStart, latEnd));
                        double lon = Double.parseDouble(lineText.substring(lonStart, lonEnd));
                        
                        GeoPoint point = new GeoPoint(lat, lon);
                        trackPoints.add(point);
                        pointCount++;
                        
                        if (lastPoint != null) {
                            totalDistance += lastPoint.distanceToAsDouble(point);
                        }
                        lastPoint = point;
                    }
                }
            }
            reader.close();
            
            if (trackPoints.isEmpty()) {
                statsText.setText("❌ Brak punktów GPS w pliku");
                return;
            }
            
            // Narysuj trasę na mapie
            Polyline line = new Polyline(mapView);
            line.setPoints(trackPoints);
            line.setColor(Color.RED);
            line.setWidth(5f);
            mapView.getOverlayManager().add(line);
            
            // Wycentruj mapę
            GeoPoint center = trackPoints.get(trackPoints.size() / 2);
            mapView.getController().setCenter(center);
            mapView.getController().setZoom(15.0);
            
            // Pokaż statystyki
            double distanceKm = totalDistance / 1000.0;
            statsText.setText(String.format(
                "📏 Dystans: %.2f km\n" +
                "📍 Punktów GPS: %d\n" +
                "📂 Plik: %s",
                distanceKm,
                pointCount,
                gpxFile.getName()
            ));
            
            Log.d(TAG, "✅ Załadowano trasę: " + pointCount + " punktów, " + distanceKm + " km");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Błąd wczytywania GPX: " + e.getMessage());
            statsText.setText("❌ Błąd: " + e.getMessage());
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }
}
