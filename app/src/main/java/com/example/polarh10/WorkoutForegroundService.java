package pl.fitness.polarh10;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import pl.fitness.polarh10.R;

public class WorkoutForegroundService extends Service {

    private static final String CHANNEL_ID = "workout_channel";
    private static final int NOTIFICATION_ID = 101;
    private static final String ACTION_UPDATE = "pl.fitness.polarh10.action.UPDATE";
    private static final String ACTION_STOP = "pl.fitness.polarh10.action.STOP";
    private static final String EXTRA_CONTENT = "extra_content";
    private boolean isInForeground = false;

    public static void start(Context context, String content) {
        Intent intent = new Intent(context, WorkoutForegroundService.class);
        intent.putExtra(EXTRA_CONTENT, content);
        ContextCompat.startForegroundService(context, intent);
    }

    public static void update(Context context, String content) {
        Intent intent = new Intent(context, WorkoutForegroundService.class);
        intent.setAction(ACTION_UPDATE);
        intent.putExtra(EXTRA_CONTENT, content);
        ContextCompat.startForegroundService(context, intent);
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, WorkoutForegroundService.class);
        intent.setAction(ACTION_STOP);
        ContextCompat.startForegroundService(context, intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        String content = intent != null ? intent.getStringExtra(EXTRA_CONTENT) : null;

        if (ACTION_STOP.equals(action)) {
            stopForeground(true);
            isInForeground = false;
            stopSelf();
            return START_NOT_STICKY;
        }

        Notification notification = buildNotification(content);

        if (ACTION_UPDATE.equals(action) && isInForeground) {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification);
        } else {
            startForeground(NOTIFICATION_ID, notification);
            isInForeground = true;
        }

        return START_STICKY;
    }

    private Notification buildNotification(String content) {
        Intent activityIntent = new Intent(this, MainActivity.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this,
            0,
            activityIntent,
            flags
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Trening biegowy aktywny")
            .setContentText(content != null ? content : "Śledzę trasę i puls...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Trening biegowy",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Utrzymuje aktywny trening biegowy w tle");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        isInForeground = false;
        super.onDestroy();
    }
}
