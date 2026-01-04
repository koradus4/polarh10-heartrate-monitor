package pl.fitness.polarh10;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class UserSelectionActivity extends Activity {
    
    private static final String PREFS_NAME = "user_prefs";
    private static final String PREF_SELECTED_USER = "selected_user";
    private static final String PREF_SELECTED_MAC = "selected_mac";
    
    // Definicje użytkowników
    private static final String USER_KRYSPIN = "KRYSPIN";
    private static final String USER_JOGIN = "JOGIN";
    private static final String MAC_KRYSPIN = "24:AC:AC:07:CA:8D";
    private static final String MAC_JOGIN = "24:AC:AC:09:A6:4D";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Sprawdź czy użytkownik był już wybrany
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String lastUser = prefs.getString(PREF_SELECTED_USER, null);
        
        // Jeśli był ostatni wybór, pokaż go na 1 sekundę i kontynuuj
        if (lastUser != null) {
            showQuickStart(lastUser);
            return;
        }
        
        // Budowanie ekranu wyboru
        buildSelectionScreen();
    }
    
    private void showQuickStart(String user) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(0xFF1a1a1a);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(40, 40, 40, 40);
        
        TextView welcomeText = new TextView(this);
        welcomeText.setText("Witaj ponownie!");
        welcomeText.setTextSize(32);
        welcomeText.setTextColor(0xFFFFFFFF);
        welcomeText.setGravity(Gravity.CENTER);
        layout.addView(welcomeText);
        
        TextView userText = new TextView(this);
        userText.setText(user.equals(USER_KRYSPIN) ? "💪 KRYSPIN" : "🏃 JOGIN");
        userText.setTextSize(48);
        userText.setTextColor(0xFF00FF00);
        userText.setGravity(Gravity.CENTER);
        userText.setPadding(0, 30, 0, 30);
        layout.addView(userText);
        
        TextView macText = new TextView(this);
        String mac = user.equals(USER_KRYSPIN) ? MAC_KRYSPIN : MAC_JOGIN;
        macText.setText("MAC: " + mac);
        macText.setTextSize(18);
        macText.setTextColor(0xFFCCCCCC);
        macText.setGravity(Gravity.CENTER);
        layout.addView(macText);
        
        setContentView(layout);
        
        // Automatyczne przejście po 1 sekundzie
        layout.postDelayed(() -> startMainActivity(user, mac), 1000);
    }
    
    private void buildSelectionScreen() {
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(0xFF1a1a1a);
        mainLayout.setGravity(Gravity.CENTER);
        mainLayout.setPadding(40, 40, 40, 40);
        
        // Tytuł
        TextView titleText = new TextView(this);
        titleText.setText("🏋️ POLAR H10 TRAINER");
        titleText.setTextSize(28);
        titleText.setTextColor(0xFFFFFFFF);
        titleText.setGravity(Gravity.CENTER);
        titleText.setPadding(0, 0, 0, 20);
        mainLayout.addView(titleText);
        
        TextView subtitleText = new TextView(this);
        subtitleText.setText("Wybierz profil użytkownika:");
        subtitleText.setTextSize(18);
        subtitleText.setTextColor(0xFFCCCCCC);
        subtitleText.setGravity(Gravity.CENTER);
        subtitleText.setPadding(0, 0, 0, 60);
        mainLayout.addView(subtitleText);
        
        // Przycisk KRYSPIN
        Button kryspinButton = createUserButton(
            "💪 KRYSPIN",
            MAC_KRYSPIN,
            Color.parseColor("#2ECC71"),
            Color.parseColor("#27AE60")
        );
        kryspinButton.setOnClickListener(v -> selectUser(USER_KRYSPIN, MAC_KRYSPIN));
        mainLayout.addView(kryspinButton);
        
        // Separator
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 40));
        mainLayout.addView(spacer);
        
        // Przycisk JOGIN
        Button joginButton = createUserButton(
            "🏃 JOGIN",
            MAC_JOGIN,
            Color.parseColor("#3498DB"),
            Color.parseColor("#2980B9")
        );
        joginButton.setOnClickListener(v -> selectUser(USER_JOGIN, MAC_JOGIN));
        mainLayout.addView(joginButton);
        
        setContentView(mainLayout);
    }
    
    private Button createUserButton(String userName, String mac, int color1, int color2) {
        Button button = new Button(this);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            300
        );
        button.setLayoutParams(params);
        
        // Gradient tło
        GradientDrawable gradient = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{color1, color2, color1}
        );
        gradient.setCornerRadius(20);
        button.setBackground(gradient);
        
        // Tekst
        button.setText(userName + "\n" + mac);
        button.setTextSize(32);
        button.setTextColor(0xFFFFFFFF);
        button.setGravity(Gravity.CENTER);
        
        return button;
    }
    
    private void selectUser(String user, String mac) {
        // Zapisz wybór
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_SELECTED_USER, user);
        editor.putString(PREF_SELECTED_MAC, mac);
        editor.apply();
        
        // Przejdź do MainActivity
        startMainActivity(user, mac);
    }
    
    private void startMainActivity(String user, String mac) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("USER_NAME", user);
        intent.putExtra("USER_MAC", mac);
        startActivity(intent);
        finish();
    }
}
