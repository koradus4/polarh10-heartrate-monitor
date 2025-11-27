package pl.fitness.polarh10;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

/**
 * Konfigurator treningów CrossFit - drag & drop klocków
 */
public class CustomWorkoutActivity extends Activity {
    
    private static final String TAG = "CustomWorkout";
    
    // Szablony (domyślne wartości)
    private int workoutTemplateSeconds = 600;  // 10 minut
    private int restTemplateSeconds = 60;       // 1 minuta
    private int breakTemplateSeconds = 300;     // 5 minut
    
    // UI - Szablony (góra)
    private TextView workoutTemplateText;
    private TextView restTemplateText;
    private TextView breakTemplateText;
    private Button workoutMinusButton, workoutPlusButton;
    private Button restMinusButton, restPlusButton;
    private Button breakMinusButton, breakPlusButton;
    private View workoutTemplateCard, restTemplateCard, breakTemplateCard, endTemplateCard;
    
    // UI - Lista treningu (dół)
    private LinearLayout workoutBlocksContainer;
    private TextView totalTimeText;
    private Button clearButton;
    private Button saveButton;
    private ScrollView scrollView;
    
    // Dane
    private ArrayList<TrainingBlock> workoutBlocks = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Ładuj zapisane klocki (jeśli istnieją)
        loadWorkoutBlocks();
        
        // Buduj UI
        createUI();
        
        // Refresh wyświetlania
        refreshBlockList();
        updateTotalTime();
    }
    
    /**
     * Tworzy cały interfejs użytkownika
     */
    private void createUI() {
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(0xFF1a1a1a);
        mainLayout.setPadding(20, 20, 20, 20);
        
        // === NAGŁÓWEK ===
        createHeader(mainLayout);
        
        // === SZABLONY (góra) ===
        createTemplatesSection(mainLayout);
        
        // === SEPARATOR ===
        View separator = new View(this);
        separator.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 3));
        separator.setBackgroundColor(0xFF444444);
        LinearLayout.LayoutParams sepMargin = (LinearLayout.LayoutParams) separator.getLayoutParams();
        sepMargin.setMargins(0, 20, 0, 20);
        mainLayout.addView(separator);
        
        // === TWÓJ TRENING (dół) ===
        createWorkoutSection(mainLayout);
        
        // === PRZYCISKI AKCJI ===
        createActionButtons(mainLayout);
        
        setContentView(mainLayout);
    }
    
    /**
     * Nagłówek z tytułem
     */
    private void createHeader(LinearLayout parent) {
        LinearLayout headerLayout = new LinearLayout(this);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setGravity(Gravity.CENTER_VERTICAL);
        
        TextView titleText = new TextView(this);
        titleText.setText("⚙️ KONFIGURATOR TRENINGU");
        titleText.setTextSize(22);
        titleText.setTextColor(0xFFFFFFFF);
        titleText.setLayoutParams(new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        
        Button closeButton = new Button(this);
        closeButton.setText("✕");
        closeButton.setTextSize(24);
        closeButton.setTextColor(0xFFFFFFFF);
        closeButton.setBackgroundColor(0xFF444444);
        closeButton.setLayoutParams(new LinearLayout.LayoutParams(
            100, 100));
        closeButton.setOnClickListener(v -> finish());
        
        headerLayout.addView(titleText);
        headerLayout.addView(closeButton);
        parent.addView(headerLayout);
        
        // Spacer
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 30));
        parent.addView(spacer);
    }
    
    /**
     * Sekcja z 4 szablonami klocków
     */
    private void createTemplatesSection(LinearLayout parent) {
        TextView sectionTitle = new TextView(this);
        sectionTitle.setText("📚 SZABLONY (kliknij aby dodać):");
        sectionTitle.setTextSize(16);
        sectionTitle.setTextColor(0xFFCCCCCC);
        parent.addView(sectionTitle);
        
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 15));
        parent.addView(spacer);
        
        // 2x2 grid
        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        
        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        
        // Szablony
        workoutTemplateCard = createTemplateCard(TrainingBlock.BlockType.WORKOUT, row1);
        restTemplateCard = createTemplateCard(TrainingBlock.BlockType.REST, row1);
        breakTemplateCard = createTemplateCard(TrainingBlock.BlockType.BREAK, row2);
        endTemplateCard = createTemplateCard(TrainingBlock.BlockType.END, row2);
        
        parent.addView(row1);
        
        View rowSpacer = new View(this);
        rowSpacer.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 15));
        parent.addView(rowSpacer);
        
        parent.addView(row2);
    }
    
    /**
     * Tworzy pojedynczy szablon klocka
     */
    private View createTemplateCard(TrainingBlock.BlockType type, LinearLayout parentRow) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(15, 15, 15, 15);
        card.setGravity(Gravity.CENTER);
        
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        cardParams.setMargins(0, 0, 10, 0);
        card.setLayoutParams(cardParams);
        
        // Kolor tła
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(new TrainingBlock(type, 0).getColor());
        bg.setCornerRadius(15);
        card.setBackground(bg);
        
        // Emoji + Nazwa
        TextView emojiText = new TextView(this);
        emojiText.setText(new TrainingBlock(type, 0).getEmoji() + " " + 
                         new TrainingBlock(type, 0).getTypeName());
        emojiText.setTextSize(16);
        emojiText.setTextColor(0xFFFFFFFF);
        emojiText.setGravity(Gravity.CENTER);
        card.addView(emojiText);
        
        // Przyciski +/- i czas (tylko dla WORKOUT, REST, BREAK)
        if (type != TrainingBlock.BlockType.END) {
            LinearLayout controlsLayout = new LinearLayout(this);
            controlsLayout.setOrientation(LinearLayout.HORIZONTAL);
            controlsLayout.setGravity(Gravity.CENTER);
            controlsLayout.setPadding(0, 10, 0, 0);
            
            Button minusBtn = new Button(this);
            minusBtn.setText("−");
            minusBtn.setTextSize(32);
            minusBtn.setTextColor(0xFF000000);
            minusBtn.setBackgroundColor(0xFFFFFFFF); // Białe tło
            minusBtn.setPadding(0, 0, 0, 0);
            minusBtn.setLayoutParams(new LinearLayout.LayoutParams(120, 120));
            
            TextView timeText = new TextView(this);
            timeText.setTextSize(18);
            timeText.setTextColor(0xFFFFFFFF);
            timeText.setGravity(Gravity.CENTER);
            timeText.setPadding(15, 0, 15, 0);
            LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            timeText.setLayoutParams(timeParams);
            
            Button plusBtn = new Button(this);
            plusBtn.setText("+");
            plusBtn.setTextSize(32);
            plusBtn.setTextColor(0xFF000000);
            plusBtn.setBackgroundColor(0xFFFFFFFF); // Białe tło
            plusBtn.setPadding(0, 0, 0, 0);
            plusBtn.setLayoutParams(new LinearLayout.LayoutParams(120, 120));
            
            // Zapisz referencje do TextView
            if (type == TrainingBlock.BlockType.WORKOUT) {
                workoutTemplateText = timeText;
                workoutMinusButton = minusBtn;
                workoutPlusButton = plusBtn;
            } else if (type == TrainingBlock.BlockType.REST) {
                restTemplateText = timeText;
                restMinusButton = minusBtn;
                restPlusButton = plusBtn;
            } else if (type == TrainingBlock.BlockType.BREAK) {
                breakTemplateText = timeText;
                breakMinusButton = minusBtn;
                breakPlusButton = plusBtn;
            }
            
            updateTemplateTimeDisplay(type);
            
            // Listenery +/-
            minusBtn.setOnClickListener(v -> adjustTemplateTime(type, -1));
            plusBtn.setOnClickListener(v -> adjustTemplateTime(type, 1));
            
            controlsLayout.addView(minusBtn);
            controlsLayout.addView(timeText);
            controlsLayout.addView(plusBtn);
            card.addView(controlsLayout);
        } else {
            // END nie ma czasu
            TextView noTimeText = new TextView(this);
            noTimeText.setText("--:--");
            noTimeText.setTextSize(18);
            noTimeText.setTextColor(0xFFCCCCCC);
            noTimeText.setGravity(Gravity.CENTER);
            noTimeText.setPadding(0, 10, 0, 0);
            card.addView(noTimeText);
        }
        
        // Kliknięcie - dodaj klocek
        card.setOnClickListener(v -> addBlock(type));
        
        // Długie przytrzymanie - drag
        card.setOnLongClickListener(v -> {
            ClipData data = ClipData.newPlainText("blockType", type.name());
            View.DragShadowBuilder shadow = new View.DragShadowBuilder(v);
            v.startDragAndDrop(data, shadow, type, 0);
            return true;
        });
        
        parentRow.addView(card);
        return card;
    }
    
    /**
     * Regulacja czasu szablonu
     */
    private void adjustTemplateTime(TrainingBlock.BlockType type, int direction) {
        int step = 0;
        
        if (type == TrainingBlock.BlockType.WORKOUT || type == TrainingBlock.BlockType.BREAK) {
            step = 60; // 1 minuta
        } else if (type == TrainingBlock.BlockType.REST) {
            step = 15; // 15 sekund
        }
        
        if (type == TrainingBlock.BlockType.WORKOUT) {
            workoutTemplateSeconds = Math.max(60, workoutTemplateSeconds + (direction * step));
        } else if (type == TrainingBlock.BlockType.REST) {
            restTemplateSeconds = Math.max(15, restTemplateSeconds + (direction * step));
        } else if (type == TrainingBlock.BlockType.BREAK) {
            breakTemplateSeconds = Math.max(60, breakTemplateSeconds + (direction * step));
        }
        
        updateTemplateTimeDisplay(type);
    }
    
    /**
     * Aktualizuje wyświetlany czas szablonu
     */
    private void updateTemplateTimeDisplay(TrainingBlock.BlockType type) {
        int seconds = 0;
        TextView textView = null;
        
        if (type == TrainingBlock.BlockType.WORKOUT) {
            seconds = workoutTemplateSeconds;
            textView = workoutTemplateText;
        } else if (type == TrainingBlock.BlockType.REST) {
            seconds = restTemplateSeconds;
            textView = restTemplateText;
        } else if (type == TrainingBlock.BlockType.BREAK) {
            seconds = breakTemplateSeconds;
            textView = breakTemplateText;
        }
        
        if (textView != null) {
            int min = seconds / 60;
            int sec = seconds % 60;
            textView.setText(String.format("%d:%02d", min, sec));
        }
    }
    
    /**
     * Sekcja z listą klocków treningu
     */
    private void createWorkoutSection(LinearLayout parent) {
        TextView sectionTitle = new TextView(this);
        sectionTitle.setText("📋 TWÓJ TRENING:");
        sectionTitle.setTextSize(16);
        sectionTitle.setTextColor(0xFFCCCCCC);
        parent.addView(sectionTitle);
        
        totalTimeText = new TextView(this);
        totalTimeText.setTextSize(14);
        totalTimeText.setTextColor(0xFF00FF00);
        totalTimeText.setPadding(0, 5, 0, 0);
        parent.addView(totalTimeText);
        
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 15));
        parent.addView(spacer);
        
        // ScrollView dla długich treningów
        scrollView = new ScrollView(this);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
        scrollView.setLayoutParams(scrollParams);
        scrollView.setBackgroundColor(0xFF2a2a2a);
        scrollView.setPadding(15, 15, 15, 15);
        
        workoutBlocksContainer = new LinearLayout(this);
        workoutBlocksContainer.setOrientation(LinearLayout.VERTICAL);
        
        // Drop zone listener
        workoutBlocksContainer.setOnDragListener((v, event) -> {
            switch (event.getAction()) {
                case DragEvent.ACTION_DROP:
                    TrainingBlock.BlockType type = (TrainingBlock.BlockType) event.getLocalState();
                    addBlock(type);
                    return true;
                case DragEvent.ACTION_DRAG_STARTED:
                    return true;
                case DragEvent.ACTION_DRAG_ENTERED:
                    workoutBlocksContainer.setBackgroundColor(0xFF3a3a3a);
                    return true;
                case DragEvent.ACTION_DRAG_EXITED:
                    workoutBlocksContainer.setBackgroundColor(0xFF2a2a2a);
                    return true;
                case DragEvent.ACTION_DRAG_ENDED:
                    workoutBlocksContainer.setBackgroundColor(0xFF2a2a2a);
                    return true;
            }
            return false;
        });
        
        scrollView.addView(workoutBlocksContainer);
        parent.addView(scrollView);
    }
    
    /**
     * Przyciski akcji (wyczyść, zapisz)
     */
    private void createActionButtons(LinearLayout parent) {
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 20));
        parent.addView(spacer);
        
        LinearLayout buttonsLayout = new LinearLayout(this);
        buttonsLayout.setOrientation(LinearLayout.HORIZONTAL);
        
        clearButton = new Button(this);
        clearButton.setText("🗑️ WYCZYŚĆ");
        clearButton.setTextSize(16);
        clearButton.setTextColor(0xFFFFFFFF);
        clearButton.setBackgroundColor(0xFFFF0000);
        clearButton.setLayoutParams(new LinearLayout.LayoutParams(
            0, 120, 1.0f));
        clearButton.setOnClickListener(v -> clearAllBlocks());
        
        View buttonSpacer = new View(this);
        buttonSpacer.setLayoutParams(new LinearLayout.LayoutParams(20, 1));
        
        saveButton = new Button(this);
        saveButton.setText("💾 PRZENIEŚ DO TIMERA");
        saveButton.setTextSize(16);
        saveButton.setTextColor(0xFFFFFFFF);
        saveButton.setBackgroundColor(0xFF00AA00);
        saveButton.setLayoutParams(new LinearLayout.LayoutParams(
            0, 120, 1.5f));
        saveButton.setOnClickListener(v -> saveAndClose());
        
        buttonsLayout.addView(clearButton);
        buttonsLayout.addView(buttonSpacer);
        buttonsLayout.addView(saveButton);
        parent.addView(buttonsLayout);
    }
    
    /**
     * Dodaje klocek do listy
     */
    private void addBlock(TrainingBlock.BlockType type) {
        int duration = 0;
        
        switch (type) {
            case WORKOUT:
                duration = workoutTemplateSeconds;
                break;
            case REST:
                duration = restTemplateSeconds;
                break;
            case BREAK:
                duration = breakTemplateSeconds;
                break;
            case END:
                duration = 0; // END nie ma czasu
                break;
        }
        
        TrainingBlock block = new TrainingBlock(type, duration);
        workoutBlocks.add(block);
        
        refreshBlockList();
        updateTotalTime();
        
        // Scroll do dołu
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }
    
    /**
     * Odświeża wyświetlanie listy klocków
     */
    private void refreshBlockList() {
        workoutBlocksContainer.removeAllViews();
        
        if (workoutBlocks.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("Przeciągnij klocki tutaj...\n\n(lub kliknij szablon powyżej)");
            emptyText.setTextSize(16);
            emptyText.setTextColor(0xFF666666);
            emptyText.setGravity(Gravity.CENTER);
            emptyText.setPadding(0, 50, 0, 50);
            workoutBlocksContainer.addView(emptyText);
            return;
        }
        
        for (int i = 0; i < workoutBlocks.size(); i++) {
            final int index = i;
            TrainingBlock block = workoutBlocks.get(i);
            
            View blockView = createBlockView(block, index);
            workoutBlocksContainer.addView(blockView);
            
            // Spacer między klockami
            if (i < workoutBlocks.size() - 1) {
                View spacer = new View(this);
                spacer.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 10));
                workoutBlocksContainer.addView(spacer);
            }
        }
    }
    
    /**
     * Tworzy widok pojedynczego klocka w liście
     */
    private View createBlockView(TrainingBlock block, int index) {
        LinearLayout blockLayout = new LinearLayout(this);
        blockLayout.setOrientation(LinearLayout.HORIZONTAL);
        blockLayout.setPadding(15, 15, 15, 15);
        blockLayout.setGravity(Gravity.CENTER_VERTICAL);
        
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(block.getColor());
        bg.setCornerRadius(10);
        blockLayout.setBackground(bg);
        
        // Numer + Opis
        TextView blockText = new TextView(this);
        blockText.setText((index + 1) + ". " + block.toString());
        blockText.setTextSize(18);
        blockText.setTextColor(0xFFFFFFFF);
        blockText.setLayoutParams(new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        blockLayout.addView(blockText);
        
        // Tylko przycisk usuń
        Button deleteBtn = new Button(this);
        deleteBtn.setText("🗑");
        deleteBtn.setTextSize(18);
        deleteBtn.setBackgroundColor(0x88000000);
        deleteBtn.setLayoutParams(new LinearLayout.LayoutParams(80, 80));
        deleteBtn.setOnClickListener(v -> deleteBlock(index));
        blockLayout.addView(deleteBtn);
        
        return blockLayout;
    }
    
    /**
     * Usuwa klocek
     */
    private void deleteBlock(int index) {
        workoutBlocks.remove(index);
        refreshBlockList();
        updateTotalTime();
    }
    
    /**
     * Czyści wszystkie klocki
     */
    private void clearAllBlocks() {
        new AlertDialog.Builder(this)
            .setTitle("Wyczyścić wszystko?")
            .setMessage("Wszystkie klocki zostaną usunięte.")
            .setPositiveButton("Tak", (dialog, which) -> {
                workoutBlocks.clear();
                refreshBlockList();
                updateTotalTime();
            })
            .setNegativeButton("Nie", null)
            .show();
    }
    
    /**
     * Aktualizuje wyświetlany łączny czas
     */
    private void updateTotalTime() {
        int totalSeconds = 0;
        int blockCount = 0;
        
        for (TrainingBlock block : workoutBlocks) {
            if (block.getType() != TrainingBlock.BlockType.END) {
                totalSeconds += block.getDurationSeconds();
            }
            blockCount++;
        }
        
        int min = totalSeconds / 60;
        int sec = totalSeconds % 60;
        
        totalTimeText.setText(String.format("⏱️ Łączny czas: %d:%02d | Bloków: %d", 
            min, sec, blockCount));
    }
    
    /**
     * Zapisuje trening i wraca do MainActivity
     */
    private void saveAndClose() {
        if (workoutBlocks.isEmpty()) {
            Toast.makeText(this, "Dodaj przynajmniej 1 klocek!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            JSONArray jsonArray = new JSONArray();
            for (TrainingBlock block : workoutBlocks) {
                jsonArray.put(block.toJSON());
            }
            
            SharedPreferences prefs = getSharedPreferences("PolarH10", MODE_PRIVATE);
            prefs.edit()
                .putString("custom_workout_json", jsonArray.toString())
                .putBoolean("custom_workout_loaded", true)
                .apply();
            
            Toast.makeText(this, "✓ Trening zapisany!", Toast.LENGTH_SHORT).show();
            finish();
            
        } catch (JSONException e) {
            Toast.makeText(this, "Błąd zapisu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Ładuje zapisane klocki
     */
    private void loadWorkoutBlocks() {
        try {
            SharedPreferences prefs = getSharedPreferences("PolarH10", MODE_PRIVATE);
            String json = prefs.getString("custom_workout_json", "[]");
            
            JSONArray array = new JSONArray(json);
            workoutBlocks.clear();
            
            for (int i = 0; i < array.length(); i++) {
                TrainingBlock block = TrainingBlock.fromJSON(array.getJSONObject(i));
                workoutBlocks.add(block);
            }
            
        } catch (JSONException e) {
            workoutBlocks.clear();
        }
    }
}
