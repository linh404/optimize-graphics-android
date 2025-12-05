package com.example.optimize_layout_demo;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ViewStub extends AppCompatActivity {

    // Biến cờ để kiểm tra xem đã inflate hay chưa
    private boolean isInflated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.viewstub_activity);
        setTitle("3. Good: ViewStub");

        // ViewStub này siêu nhẹ, chưa chiếm bộ nhớ layout con
        // Thêm android.view. vào trước
        android.view.ViewStub stub = findViewById(R.id.stubHeavy);

        findViewById(R.id.btnInflate).setOnClickListener(v -> {
            if (!isInflated) {
                // ĐÂY LÀ LÚC QUAN TRỌNG:
                // Gọi inflate() thì layout nặng mới bắt đầu được tạo và đưa vào RAM
                View inflatedView = stub.inflate();

                isInflated = true;
                Toast.makeText(this, "Giờ mới tốn RAM (Inflated)!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Đã inflate rồi!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}