package com.example.android.bluetoothlegatt;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

public class PlotActivity extends AppCompatActivity {
    private static final String TAG = "PlotActivity";

    private LineChart lineChart;

    // We'll store references to these arrays in onCreate,
    // so we can pass them to writeCsvToUri() inside onActivityResult.
    private ArrayList<DeviceControlActivity.DataPoint> mTempData;
    private ArrayList<DeviceControlActivity.DataPoint> mHumData;
    private ArrayList<DeviceControlActivity.DataPoint> mPresData;
    private ArrayList<DeviceControlActivity.DataPoint> mCoData;

    private static final int REQUEST_CODE_EXPORT_CSV = 2001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plot);

        // 1. Get references to your chart and export button
        lineChart = findViewById(R.id.lineChart);
        Button exportButton = findViewById(R.id.button_export_csv);

        // 2. Retrieve the plot option (which data sets to show)
        int plotOption = getIntent().getIntExtra("plot_option", 0);
        LineData lineData = new LineData();

        // 3. Retrieve data arrays passed from DeviceControlActivity
        mTempData = (ArrayList<DeviceControlActivity.DataPoint>)
                getIntent().getSerializableExtra("temperatureData");
        mHumData = (ArrayList<DeviceControlActivity.DataPoint>)
                getIntent().getSerializableExtra("humidityData");
        mPresData = (ArrayList<DeviceControlActivity.DataPoint>)
                getIntent().getSerializableExtra("pressureData");
        mCoData = (ArrayList<DeviceControlActivity.DataPoint>)
                getIntent().getSerializableExtra("coData");

        // Log the sizes for debugging
        Log.d(TAG, "Temperature size: " + (mTempData == null ? "null" : mTempData.size()));
        Log.d(TAG, "Humidity size: " + (mHumData == null ? "null" : mHumData.size()));
        Log.d(TAG, "Pressure size: " + (mPresData == null ? "null" : mPresData.size()));
        Log.d(TAG, "CO size: " + (mCoData == null ? "null" : mCoData.size()));

        // 4. Plot the selected data sets
        if (plotOption == 0) {
            // Plot All
            LineDataSet tempSet = createDataSet(mTempData, "Temperature", Color.RED);
            LineDataSet humSet = createDataSet(mHumData, "Humidity", Color.BLUE);
            LineDataSet presSet = createDataSet(mPresData, "Pressure", Color.GREEN);
            LineDataSet coSet = createDataSet(mCoData, "CO", Color.MAGENTA);

            if (tempSet != null) lineData.addDataSet(tempSet);
            if (humSet != null) lineData.addDataSet(humSet);
            if (presSet != null) lineData.addDataSet(presSet);
            if (coSet != null) lineData.addDataSet(coSet);

        } else if (plotOption == 1) {
            // Temperature only
            LineDataSet tempSet = createDataSet(mTempData, "Temperature", Color.RED);
            if (tempSet != null) lineData.addDataSet(tempSet);

        } else if (plotOption == 2) {
            // Humidity only
            LineDataSet humSet = createDataSet(mHumData, "Humidity", Color.BLUE);
            if (humSet != null) lineData.addDataSet(humSet);

        } else if (plotOption == 3) {
            // Pressure only
            LineDataSet presSet = createDataSet(mPresData, "Pressure", Color.GREEN);
            if (presSet != null) lineData.addDataSet(presSet);

        } else if (plotOption == 4) {
            // CO only
            LineDataSet coSet = createDataSet(mCoData, "CO", Color.MAGENTA);
            if (coSet != null) lineData.addDataSet(coSet);
        }

        lineChart.setData(lineData);
        lineChart.invalidate(); // refresh the chart

        // 5. Set up the Export CSV button
        exportButton.setOnClickListener(v -> {
            // Launch the system file picker
            exportCsvData();
        });
    }

    // Helper method: creates a LineDataSet from an ArrayList of DataPoint.
    private LineDataSet createDataSet(ArrayList<DeviceControlActivity.DataPoint> dataPoints,
                                      String label, int color) {
        if (dataPoints == null || dataPoints.isEmpty()) {
            Log.w(TAG, label + " dataPoints are empty or null. Not plotting.");
            return null;
        }
        ArrayList<Entry> entries = new ArrayList<>();
        // Use the first timestamp as reference (x=0)
        long baseTime = dataPoints.get(0).timestamp;
        for (DeviceControlActivity.DataPoint dp : dataPoints) {
            float x = (dp.timestamp - baseTime) / 1000f; // seconds elapsed
            entries.add(new Entry(x, dp.value));
        }
        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(color);
        dataSet.setCircleColor(color);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(3f);
        return dataSet;
    }

    // Step 1: Start the "Save As" flow
    private void exportCsvData() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, "SensorData.csv");

        startActivityForResult(intent, REQUEST_CODE_EXPORT_CSV);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_EXPORT_CSV && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                // Step 2: Write CSV to the chosen URI, referencing our local fields
                writeCsvToUri(uri, mTempData, mHumData, mPresData, mCoData);
            }
        }
    }

    // Step 3: Build the CSV and write it out
    private void writeCsvToUri(Uri uri,
                               ArrayList<DeviceControlActivity.DataPoint> tempData,
                               ArrayList<DeviceControlActivity.DataPoint> humData,
                               ArrayList<DeviceControlActivity.DataPoint> presData,
                               ArrayList<DeviceControlActivity.DataPoint> coData) {

        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append("Timestamp,CO,Temperature,Humidity,Pressure\n");

        // Decide how many rows to export based on the smallest array size
        int dataCount = Math.min(
                coData == null ? 0 : coData.size(),
                Math.min(
                        tempData == null ? 0 : tempData.size(),
                        Math.min(
                                humData == null ? 0 : humData.size(),
                                presData == null ? 0 : presData.size()
                        )
                )
        );

        Log.d(TAG, "Exporting " + dataCount + " rows to CSV...");

        for (int i = 0; i < dataCount; i++) {
            long timestamp = coData.get(i).timestamp; // or from tempData if they align
            float coValue = coData.get(i).value;
            float tempValue = tempData.get(i).value;
            float humValue = humData.get(i).value;
            float presValue = presData.get(i).value;

            csvBuilder.append(timestamp).append(",")
                    .append(coValue).append(",")
                    .append(tempValue).append(",")
                    .append(humValue).append(",")
                    .append(presValue).append("\n");
        }

        try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
            if (outputStream != null) {
                outputStream.write(csvBuilder.toString().getBytes());
                outputStream.flush();
                Toast.makeText(this, "CSV exported successfully", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error exporting CSV", Toast.LENGTH_SHORT).show();
        }
    }
}
