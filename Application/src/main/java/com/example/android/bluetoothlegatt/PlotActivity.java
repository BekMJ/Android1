package com.example.android.bluetoothlegatt;

import android.graphics.Color;
import android.os.Bundle;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import java.util.ArrayList;
import androidx.appcompat.app.AppCompatActivity;


public class PlotActivity extends AppCompatActivity {
    private LineChart lineChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Ensure you have a layout (e.g. activity_plot.xml) with a LineChart view with id "lineChart"
        setContentView(R.layout.activity_plot);
        lineChart = findViewById(R.id.lineChart);

        int plotOption = getIntent().getIntExtra("plot_option", 0);
        LineData lineData = new LineData();

        // For Plot All, retrieve all data lists and create separate data sets.
        if (plotOption == 0) {
            ArrayList<DeviceControlActivity.DataPoint> tempData =
                    (ArrayList<DeviceControlActivity.DataPoint>) getIntent().getSerializableExtra("temperatureData");
            ArrayList<DeviceControlActivity.DataPoint> humData =
                    (ArrayList<DeviceControlActivity.DataPoint>) getIntent().getSerializableExtra("humidityData");
            ArrayList<DeviceControlActivity.DataPoint> presData =
                    (ArrayList<DeviceControlActivity.DataPoint>) getIntent().getSerializableExtra("pressureData");
            ArrayList<DeviceControlActivity.DataPoint> coData =
                    (ArrayList<DeviceControlActivity.DataPoint>) getIntent().getSerializableExtra("coData");

            LineDataSet tempSet = createDataSet(tempData, "Temperature", Color.RED);
            LineDataSet humSet = createDataSet(humData, "Humidity", Color.BLUE);
            LineDataSet presSet = createDataSet(presData, "Pressure", Color.GREEN);
            LineDataSet coSet = createDataSet(coData, "CO", Color.MAGENTA);

            if (tempSet != null) lineData.addDataSet(tempSet);
            if (humSet != null) lineData.addDataSet(humSet);
            if (presSet != null) lineData.addDataSet(presSet);
            if (coSet != null) lineData.addDataSet(coSet);
        } else if (plotOption == 1) {
            ArrayList<DeviceControlActivity.DataPoint> tempData =
                    (ArrayList<DeviceControlActivity.DataPoint>) getIntent().getSerializableExtra("temperatureData");
            LineDataSet tempSet = createDataSet(tempData, "Temperature", Color.RED);
            if (tempSet != null) lineData.addDataSet(tempSet);
        } else if (plotOption == 2) {
            ArrayList<DeviceControlActivity.DataPoint> humData =
                    (ArrayList<DeviceControlActivity.DataPoint>) getIntent().getSerializableExtra("humidityData");
            LineDataSet humSet = createDataSet(humData, "Humidity", Color.BLUE);
            if (humSet != null) lineData.addDataSet(humSet);
        } else if (plotOption == 3) {
            ArrayList<DeviceControlActivity.DataPoint> presData =
                    (ArrayList<DeviceControlActivity.DataPoint>) getIntent().getSerializableExtra("pressureData");
            LineDataSet presSet = createDataSet(presData, "Pressure", Color.GREEN);
            if (presSet != null) lineData.addDataSet(presSet);
        } else if (plotOption == 4) {
            ArrayList<DeviceControlActivity.DataPoint> coData =
                    (ArrayList<DeviceControlActivity.DataPoint>) getIntent().getSerializableExtra("coData");
            LineDataSet coSet = createDataSet(coData, "CO", Color.MAGENTA);
            if (coSet != null) lineData.addDataSet(coSet);
        }

        lineChart.setData(lineData);
        lineChart.invalidate(); // refresh
    }

    // Helper method: creates a LineDataSet from an ArrayList of DataPoint.
    private LineDataSet createDataSet(ArrayList<DeviceControlActivity.DataPoint> dataPoints, String label, int color) {
        if (dataPoints == null || dataPoints.isEmpty()) return null;
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
}
