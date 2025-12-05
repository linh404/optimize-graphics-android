package com.example.optimize_layout_demo;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class NotViewStub extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.not_viewstub_activity);
        setTitle("3. Bad: Visibility GONE");

        // 50 View này đã được tạo (Inflated) ngay khi mở màn hình
        // Dù visibility="gone" nhưng chúng vẫn chiếm bộ nhớ RAM
        View[] panels = new View[50];
        int[] panelIds = new int[]{
            R.id.heavyPanel1, R.id.heavyPanel2, R.id.heavyPanel3, R.id.heavyPanel4, R.id.heavyPanel5,
            R.id.heavyPanel6, R.id.heavyPanel7, R.id.heavyPanel8, R.id.heavyPanel9, R.id.heavyPanel10,
            R.id.heavyPanel11, R.id.heavyPanel12, R.id.heavyPanel13, R.id.heavyPanel14, R.id.heavyPanel15,
            R.id.heavyPanel16, R.id.heavyPanel17, R.id.heavyPanel18, R.id.heavyPanel19, R.id.heavyPanel20,
            R.id.heavyPanel21, R.id.heavyPanel22, R.id.heavyPanel23, R.id.heavyPanel24, R.id.heavyPanel25,
            R.id.heavyPanel26, R.id.heavyPanel27, R.id.heavyPanel28, R.id.heavyPanel29, R.id.heavyPanel30,
            R.id.heavyPanel31, R.id.heavyPanel32, R.id.heavyPanel33, R.id.heavyPanel34, R.id.heavyPanel35,
            R.id.heavyPanel36, R.id.heavyPanel37, R.id.heavyPanel38, R.id.heavyPanel39, R.id.heavyPanel40,
            R.id.heavyPanel41, R.id.heavyPanel42, R.id.heavyPanel43, R.id.heavyPanel44, R.id.heavyPanel45,
            R.id.heavyPanel46, R.id.heavyPanel47, R.id.heavyPanel48, R.id.heavyPanel49, R.id.heavyPanel50
        };
        for (int i = 0; i < 50; i++) {
            panels[i] = findViewById(panelIds[i]);
        }
        
        int[] buttonIds = new int[]{
            R.id.btnShow1, R.id.btnShow2, R.id.btnShow3, R.id.btnShow4, R.id.btnShow5,
            R.id.btnShow6, R.id.btnShow7, R.id.btnShow8, R.id.btnShow9, R.id.btnShow10,
            R.id.btnShow11, R.id.btnShow12, R.id.btnShow13, R.id.btnShow14, R.id.btnShow15,
            R.id.btnShow16, R.id.btnShow17, R.id.btnShow18, R.id.btnShow19, R.id.btnShow20,
            R.id.btnShow21, R.id.btnShow22, R.id.btnShow23, R.id.btnShow24, R.id.btnShow25,
            R.id.btnShow26, R.id.btnShow27, R.id.btnShow28, R.id.btnShow29, R.id.btnShow30,
            R.id.btnShow31, R.id.btnShow32, R.id.btnShow33, R.id.btnShow34, R.id.btnShow35,
            R.id.btnShow36, R.id.btnShow37, R.id.btnShow38, R.id.btnShow39, R.id.btnShow40,
            R.id.btnShow41, R.id.btnShow42, R.id.btnShow43, R.id.btnShow44, R.id.btnShow45,
            R.id.btnShow46, R.id.btnShow47, R.id.btnShow48, R.id.btnShow49, R.id.btnShow50
        };

        // Mỗi button hiện 1 layout riêng
        for (int i = 0; i < 50; i++) {
            final int index = i;
            findViewById(buttonIds[i]).setOnClickListener(v -> {
                panels[index].setVisibility(View.VISIBLE);
                Toast.makeText(this, "Đã hiện Layout " + (index + 1) + " (RAM đã tốn từ trước)", Toast.LENGTH_SHORT).show();
            });
        }
    }
}