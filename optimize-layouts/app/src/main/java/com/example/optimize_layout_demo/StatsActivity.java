package com.example.optimize_layout_demo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

/**
 * Simple activity that shows benchmark details: mode, average and the list of measured frames.
 * Launched with extras:
 *  - "mode" : String ("Canvas" or "OpenGL")
 *  - "avg"  : double (average frame ms)
 *  - "frame_times" : double[] (individual measured frame times in ms)
 */
public class StatsActivity extends AppCompatActivity {

    public static final String EXTRA_MODE = "mode";
    public static final String EXTRA_AVG = "avg";
    public static final String EXTRA_FRAME_TIMES = "frame_times";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        TextView tv = findViewById(R.id.text_stats);

        Intent intent = getIntent();
        String mode = intent.getStringExtra(EXTRA_MODE);
        double avg = intent.getDoubleExtra(EXTRA_AVG, -1.0);
        double[] times = intent.getDoubleArrayExtra(EXTRA_FRAME_TIMES);

        StringBuilder sb = new StringBuilder();
        sb.append("Benchmark: ").append(mode == null ? "Unknown" : mode).append("\n\n");
        if (avg >= 0) {
            sb.append(String.format(Locale.US, "Average: %.2f ms\n\n", avg));
        }
        if (times != null && times.length > 0) {
            sb.append("Measured frames (most recent last):\n");
            // times were collected in order (first..last). Show with index
            for (int i = 0; i < times.length; i++) {
                sb.append(String.format(Locale.US, "%03d: %.2f ms\n", i + 1, times[i]));
            }
        } else {
            sb.append("No frame times recorded.");
        }

        tv.setText(sb.toString());
    }
}