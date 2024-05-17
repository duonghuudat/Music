package com.example.music;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class MainActivity extends AppCompatActivity {

    Button button;
    ImageView imageView;
    Context context;
    SeekBar seekBar;
    CheckBox repeatCheckBox;
    TextView remainingTimeTextView;

    boolean isPlaying = false;

    private static final String PREFS_NAME = "MusicPrefs";
    private static final String PREF_REPEAT = "pref_repeat";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.activity_main);
        button = findViewById(R.id.button);
        imageView = findViewById(R.id.imageView);
        seekBar = findViewById(R.id.seekBar);
        repeatCheckBox = findViewById(R.id.repeatCheckBox);
        remainingTimeTextView = findViewById(R.id.remainingTimeTextView);


        String imageUrl = "https://static-cse.canva.com/blob/1379502/1600w-1Nr6gsUndKw.jpg";
        MyThread myThread = new MyThread(imageUrl, context);
        myThread.start();

        // Load saved preferences
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean repeat = preferences.getBoolean(PREF_REPEAT, false);
        repeatCheckBox.setChecked(repeat);

        button.setOnClickListener(view -> {
            if (isPlaying) {
                if (MyService.mediaPlayer != null && MyService.mediaPlayer.isPlaying()) {
                    MyService.mediaPlayer.pause();
                    isPlaying = false;
                    button.setText("Play");
                }
            } else {
                Intent serviceIntent = new Intent(this, MyService.class);
                serviceIntent.putExtra("repeat", repeatCheckBox.isChecked());
                startService(serviceIntent);
                isPlaying = true;
                button.setText("Pause");
                updateSeekBar();
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && MyService.mediaPlayer != null) {
                    MyService.mediaPlayer.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        repeatCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save the repeat preference
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(PREF_REPEAT, isChecked);
            editor.apply();

            // Update the service with the new repeat setting
            if (MyService.mediaPlayer != null) {
                MyService.mediaPlayer.setLooping(isChecked);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        updateSeekBar();
    }

    private void updateSeekBar() {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (MyService.mediaPlayer != null && MyService.mediaPlayer.isPlaying()) {
                    isPlaying = true;
                    seekBar.setMax(MyService.mediaPlayer.getDuration());
                    seekBar.setProgress(MyService.mediaPlayer.getCurrentPosition());

                    // Update remaining time
                    int remainingTime = (MyService.mediaPlayer.getDuration() - MyService.mediaPlayer.getCurrentPosition()) / 1000;
                    remainingTimeTextView.setText("Remaining time: " + remainingTime + " seconds");
                }
                handler.postDelayed(this, 1000); // Update seekbar and remaining time every second
            }
        }, 0);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isPlaying && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            Intent notificationIntent = new Intent(context, MainActivity.class);
            notificationIntent.setAction(Intent.ACTION_MAIN);
            notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "default")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Music")
                    .setContentText("Nhạc đang phát")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setSound(null)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);
            notificationManager.notify(1, builder.build());
        }
    }
}
