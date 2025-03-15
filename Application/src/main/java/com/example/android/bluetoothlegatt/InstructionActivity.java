package com.example.android.bluetoothlegatt;

import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class InstructionActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instruction);

        // Optional: set an ActionBar title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Instructions");
        }

        // Title
        TextView title = findViewById(R.id.instruction_title);
        title.setText("How to Use the Device");

        // Image if you want to load dynamically
        ImageView deviceImage = findViewById(R.id.instruction_image);
        // deviceImage.setImageResource(R.drawable.device);

        // Bullet points text
        TextView instructionText = findViewById(R.id.instruction_text);

        // Build a bullet list or just use raw text with bullet characters
        // For example:
        String instructions =
                "• Press the reset button located on the front of the device before starting to scan\n" +
                        "• The device is named Univ. Okla\n" +
                        "• Keep the device within Bluetooth range\n" +
                        "• Once connected to your phone, you can start receiving data\n" +
                        "• When using the breath sample, breathe deeply into the device for 15 seconds\n" +
                        "• The device does not have a power off button; just press disconnect and it will automatically turn off";

        instructionText.setText(instructions);
    }
}
