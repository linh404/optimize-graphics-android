package com.example.optimize_layout_demo;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ViewStub extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Mở ngay lập tức, không delay vì layout nhẹ
        setContentView(R.layout.viewstub_activity);
        setTitle("3. Good: ViewStub");

        // 50 ViewStub này siêu nhẹ, chưa chiếm bộ nhớ layout con
        android.view.ViewStub[] stubs = new android.view.ViewStub[50];
        int[] stubIds = new int[]{
            R.id.stubHeavy1, R.id.stubHeavy2, R.id.stubHeavy3, R.id.stubHeavy4, R.id.stubHeavy5,
            R.id.stubHeavy6, R.id.stubHeavy7, R.id.stubHeavy8, R.id.stubHeavy9, R.id.stubHeavy10,
            R.id.stubHeavy11, R.id.stubHeavy12, R.id.stubHeavy13, R.id.stubHeavy14, R.id.stubHeavy15,
            R.id.stubHeavy16, R.id.stubHeavy17, R.id.stubHeavy18, R.id.stubHeavy19, R.id.stubHeavy20,
            R.id.stubHeavy21, R.id.stubHeavy22, R.id.stubHeavy23, R.id.stubHeavy24, R.id.stubHeavy25,
            R.id.stubHeavy26, R.id.stubHeavy27, R.id.stubHeavy28, R.id.stubHeavy29, R.id.stubHeavy30,
            R.id.stubHeavy31, R.id.stubHeavy32, R.id.stubHeavy33, R.id.stubHeavy34, R.id.stubHeavy35,
            R.id.stubHeavy36, R.id.stubHeavy37, R.id.stubHeavy38, R.id.stubHeavy39, R.id.stubHeavy40,
            R.id.stubHeavy41, R.id.stubHeavy42, R.id.stubHeavy43, R.id.stubHeavy44, R.id.stubHeavy45,
            R.id.stubHeavy46, R.id.stubHeavy47, R.id.stubHeavy48, R.id.stubHeavy49, R.id.stubHeavy50
        };
        for (int i = 0; i < 50; i++) {
            stubs[i] = findViewById(stubIds[i]);
        }
        
        int[] buttonIds = new int[]{
            R.id.btnInflate1, R.id.btnInflate2, R.id.btnInflate3, R.id.btnInflate4, R.id.btnInflate5,
            R.id.btnInflate6, R.id.btnInflate7, R.id.btnInflate8, R.id.btnInflate9, R.id.btnInflate10,
            R.id.btnInflate11, R.id.btnInflate12, R.id.btnInflate13, R.id.btnInflate14, R.id.btnInflate15,
            R.id.btnInflate16, R.id.btnInflate17, R.id.btnInflate18, R.id.btnInflate19, R.id.btnInflate20,
            R.id.btnInflate21, R.id.btnInflate22, R.id.btnInflate23, R.id.btnInflate24, R.id.btnInflate25,
            R.id.btnInflate26, R.id.btnInflate27, R.id.btnInflate28, R.id.btnInflate29, R.id.btnInflate30,
            R.id.btnInflate31, R.id.btnInflate32, R.id.btnInflate33, R.id.btnInflate34, R.id.btnInflate35,
            R.id.btnInflate36, R.id.btnInflate37, R.id.btnInflate38, R.id.btnInflate39, R.id.btnInflate40,
            R.id.btnInflate41, R.id.btnInflate42, R.id.btnInflate43, R.id.btnInflate44, R.id.btnInflate45,
            R.id.btnInflate46, R.id.btnInflate47, R.id.btnInflate48, R.id.btnInflate49, R.id.btnInflate50
        };

        // Mỗi button inflate 1 ViewStub riêng
        for (int i = 0; i < 50; i++) {
            final int index = i;
            findViewById(buttonIds[i]).setOnClickListener(v -> {
                if (stubs[index] != null) {
                    stubs[index].inflate();
                    stubs[index] = null; // Đánh dấu đã inflate
                    Toast.makeText(this, "Đã inflate Layout " + (index + 1), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Layout " + (index + 1) + " đã được inflate rồi!", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}