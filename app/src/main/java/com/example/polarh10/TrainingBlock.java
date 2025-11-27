package pl.fitness.polarh10;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Klasa reprezentująca pojedynczy klocek treningu w konfiguratorze CrossFit.
 * Każdy klocek ma typ (WORKOUT/REST/BREAK/END) i czas trwania.
 */
public class TrainingBlock {
    
    /**
     * Typ klocka treningowego
     */
    public enum BlockType {
        WORKOUT,  // 🔴 Trening - kolor czerwony
        REST,     // 🟢 Odpoczynek - kolor zielony
        BREAK,    // 🔵 Przerwa treningowa - kolor niebieski
        END       // ⬛ Koniec treningu - kolor czarny
    }
    
    private BlockType type;
    private int durationSeconds;
    
    /**
     * Konstruktor tworzący nowy klocek treningowy
     */
    public TrainingBlock(BlockType type, int durationSeconds) {
        this.type = type;
        this.durationSeconds = durationSeconds;
    }
    
    // Gettery
    public BlockType getType() {
        return type;
    }
    
    public int getDurationSeconds() {
        return durationSeconds;
    }
    
    // Settery
    public void setType(BlockType type) {
        this.type = type;
    }
    
    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }
    
    /**
     * Zwraca emoji odpowiadające typowi klocka
     */
    public String getEmoji() {
        switch (type) {
            case WORKOUT: return "🔴";
            case REST: return "🟢";
            case BREAK: return "🔵";
            case END: return "⬛";
            default: return "❓";
        }
    }
    
    /**
     * Zwraca nazwę typu klocka po polsku
     */
    public String getTypeName() {
        switch (type) {
            case WORKOUT: return "TRENING";
            case REST: return "ODPOCZYNEK";
            case BREAK: return "PRZERWA";
            case END: return "KONIEC";
            default: return "NIEZNANY";
        }
    }
    
    /**
     * Zwraca kolor dla klocka (hex)
     */
    public int getColor() {
        switch (type) {
            case WORKOUT: return 0xFFFF0000; // czerwony
            case REST: return 0xFF00FF00;    // zielony
            case BREAK: return 0xFF0000FF;   // niebieski
            case END: return 0xFF000000;     // czarny
            default: return 0xFF808080;      // szary
        }
    }
    
    /**
     * Formatuje czas w formacie MM:SS
     */
    public String getFormattedDuration() {
        if (type == BlockType.END) {
            return "--:--";
        }
        int minutes = durationSeconds / 60;
        int seconds = durationSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
    
    /**
     * Konwertuje klocek do JSONObject dla zapisu
     */
    public JSONObject toJSON() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("type", type.name());
        obj.put("duration", durationSeconds);
        return obj;
    }
    
    /**
     * Tworzy klocek z JSONObject
     */
    public static TrainingBlock fromJSON(JSONObject obj) throws JSONException {
        String typeStr = obj.getString("type");
        int duration = obj.getInt("duration");
        BlockType type = BlockType.valueOf(typeStr);
        return new TrainingBlock(type, duration);
    }
    
    /**
     * Kopia klocka (dla duplikacji)
     */
    public TrainingBlock copy() {
        return new TrainingBlock(this.type, this.durationSeconds);
    }
    
    @Override
    public String toString() {
        return getEmoji() + " " + getTypeName() + " " + getFormattedDuration();
    }
}
