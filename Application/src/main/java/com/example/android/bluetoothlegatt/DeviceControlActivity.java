package com.example.android.bluetoothlegatt;

import android.app.AlertDialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.switchmaterial.SwitchMaterial;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class DeviceControlActivity extends AppCompatActivity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private TextView mConnectionState;
    private String mDeviceName;
    private String mDeviceAddress;
    private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<>();
    private boolean mConnected = false;

    // UUIDs of interest
    public final static UUID UUID_TEMPERATURE = UUID.fromString(SampleGattAttributes.TEMPERATURE);
    public final static UUID UUID_HUMIDITY = UUID.fromString(SampleGattAttributes.HUMIDITY);
    public final static UUID UUID_PRESSURE = UUID.fromString(SampleGattAttributes.PRESSURE);
    public final static UUID UUID_CO = UUID.fromString(SampleGattAttributes.CO);

    private boolean isTestModeActive = false;


    // DataPoint class for test data (public static so PlotActivity can access it)
    public static class DataPoint implements java.io.Serializable {
        public long timestamp;
        public float value;
        public DataPoint(long timestamp, float value) {
            this.timestamp = timestamp;
            this.value = value;
        }
    }

    // Data lists for sensors
    private ArrayList<DataPoint> coDataList = new ArrayList<>();
    private ArrayList<DataPoint> temperatureDataList = new ArrayList<>();
    private ArrayList<DataPoint> humidityDataList = new ArrayList<>();
    private ArrayList<DataPoint> pressureDataList = new ArrayList<>();

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    // Service connection with extra logging.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                Toast.makeText(DeviceControlActivity.this, "Bluetooth initialization failed", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            Log.d(TAG, "Service connected. Attempting to connect to: " + mDeviceAddress);
            boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result = " + result);
            if (!result) {
                Toast.makeText(DeviceControlActivity.this, "Failed to initiate connection", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "Service disconnected");
            mBluetoothLeService = null;
        }
    };

    // BroadcastReceiver to handle BLE events with logging.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "Broadcast received: " + action);
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                Toast.makeText(DeviceControlActivity.this, "Connected to device", Toast.LENGTH_SHORT).show();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                clearUI();
                Toast.makeText(DeviceControlActivity.this, "Disconnected from device", Toast.LENGTH_SHORT).show();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                Log.d(TAG, "GATT services discovered");
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                String dataType = intent.getStringExtra(BluetoothLeService.EXTRA_DATA_TYPE);
                String dataValue = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                updateCharacteristicValue(dataType, dataValue);

                // Record raw value if test mode is active.
                if (isTestModeActive) {
                    float rawValue = intent.getFloatExtra(BluetoothLeService.EXTRA_RAW_VALUE, Float.NaN);
                    long timestamp = System.currentTimeMillis();
                    if (dataType.equals(UUID_TEMPERATURE.toString())) {
                        temperatureDataList.add(new DataPoint(timestamp, rawValue));
                    } else if (dataType.equals(UUID_HUMIDITY.toString())) {
                        humidityDataList.add(new DataPoint(timestamp, rawValue));
                    } else if (dataType.equals(UUID_PRESSURE.toString())) {
                        pressureDataList.add(new DataPoint(timestamp, rawValue));
                    } else if (dataType.equals(UUID_CO.toString())) {
                        coDataList.add(new DataPoint(timestamp, rawValue));
                    }
                }
            }
        }
    };

    private void updateCharacteristicValue(String dataType, String dataValue) {
        for (int i = 0; i < mGattCharacteristics.size(); i++) {
            ArrayList<BluetoothGattCharacteristic> charas = mGattCharacteristics.get(i);
            for (BluetoothGattCharacteristic characteristic : charas) {
                if (characteristic.getUuid().toString().equals(dataType)) {
                    HashMap<String, String> childItem = (HashMap<String, String>)
                            ((SimpleExpandableListAdapter) mGattServicesList.getExpandableListAdapter())
                                    .getChild(i, charas.indexOf(characteristic));
                    if (childItem != null) {
                        childItem.put("VALUE", dataValue);
                    }
                    ((SimpleExpandableListAdapter) mGattServicesList.getExpandableListAdapter()).notifyDataSetChanged();
                    break;
                }
            }
        }
    }

    private void clearUI() {
        mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);



        // Existing On/Off buttons.
        SwitchMaterial switchNotifications = findViewById(R.id.switch_notifications);
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            enableNotifications(isChecked);
        });

        Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        mGattServicesList = findViewById(R.id.gatt_services_list);

        if(getSupportActionBar() != null) {
            getSupportActionBar().setTitle(mDeviceName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } else {
            Log.w(TAG, "getSupportActionBar() returned null");
        }

        // Bind to the BluetoothLeService.
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    // Starts a 15-second data test.
    private void startDataTest() {
        coDataList.clear();
        temperatureDataList.clear();
        humidityDataList.clear();
        pressureDataList.clear();

        isTestModeActive = true;
        Toast.makeText(this, "Test started: collecting data for 15 sec", Toast.LENGTH_SHORT).show();
        new CountDownTimer(15000, 1000) {
            public void onTick(long millisUntilFinished) { }
            public void onFinish() {
                isTestModeActive = false;
                Toast.makeText(DeviceControlActivity.this, "Test finished", Toast.LENGTH_SHORT).show();
                showPlotOptions();
            }
        }.start();
    }

    // Displays a dialog to choose which sensor data to plot.
    private void showPlotOptions() {
        String[] options = {"Plot All", "Plot Temperature", "Plot Humidity", "Plot Pressure", "Plot CO"};
        new AlertDialog.Builder(this)
                .setTitle("Select Data to Plot")
                .setItems(options, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(DeviceControlActivity.this, PlotActivity.class);
                        intent.putExtra("plot_option", which);
                        if (which == 0) {
                            intent.putExtra("temperatureData", temperatureDataList);
                            intent.putExtra("humidityData", humidityDataList);
                            intent.putExtra("pressureData", pressureDataList);
                            intent.putExtra("coData", coDataList);
                        } else if (which == 1) {
                            intent.putExtra("temperatureData", temperatureDataList);
                        } else if (which == 2) {
                            intent.putExtra("humidityData", humidityDataList);
                        } else if (which == 3) {
                            intent.putExtra("pressureData", pressureDataList);
                        } else if (which == 4) {
                            intent.putExtra("coData", coDataList);
                        }
                        startActivity(intent);
                    }
                })
                .show();
    }

    // Enables notifications with a slight delay between each.
    private void enableNotificationWithDelay(final BluetoothGattCharacteristic characteristic, final boolean enabled, int delay) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mBluetoothLeService.setCharacteristicNotification(characteristic, enabled);
            }
        }, delay);
    }

    private void enableNotifications(boolean enabled) {
        if (mBluetoothLeService == null || mGattCharacteristics == null) return;
        int delay = 0;
        for (ArrayList<BluetoothGattCharacteristic> charas : mGattCharacteristics) {
            for (BluetoothGattCharacteristic characteristic : charas) {
                UUID uuid = characteristic.getUuid();
                if (UUID_CO.equals(uuid) ||
                        UUID_TEMPERATURE.equals(uuid) ||
                        UUID_HUMIDITY.equals(uuid) ||
                        UUID_PRESSURE.equals(uuid)) {
                    enableNotificationWithDelay(characteristic, enabled, delay);
                    delay += 200;
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "onResume: connect() returned: " + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        // Always show "Disconnect" in the menu
        menu.findItem(R.id.menu_disconnect).setVisible(true);
        return true;
    }


    @Override

    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_disconnect:
                if (mBluetoothLeService != null) {
                    mBluetoothLeService.disconnect();
                }
                return true;

            case R.id.menu_test:
                // This calls the same logic you had for the "Test" button
                startDataTest();
                return true;

            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void updateConnectionState(final int resourceId) {

    }

    // Displays discovered GATT services and their characteristics.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData = new ArrayList<>();
        mGattCharacteristics = new ArrayList<>();

        Set<String> relevantUuids = new HashSet<>(Arrays.asList(
                "00002a6e-0000-1000-8000-00805f9b34fb", // Temperature
                "00002a6f-0000-1000-8000-00805f9b34fb", // Humidity
                "00002bd0-0000-1000-8000-00805f9b34fb", // CO concentration
                "00002a6d-0000-1000-8000-00805f9b34fb"  // Pressure
        ));

        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<>();
            String uuid = gattService.getUuid().toString();
            if (uuid.startsWith("0000181a") || uuid.equals("19b10001-e8f4-537e-4f6c-d104768a1214")) {
                currentServiceData.put(LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
                currentServiceData.put(LIST_UUID, uuid);
                gattServiceData.add(currentServiceData);

                ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<>();
                ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<>();
                for (BluetoothGattCharacteristic gattCharacteristic : gattService.getCharacteristics()) {
                    uuid = gattCharacteristic.getUuid().toString();
                    if (relevantUuids.contains(uuid)) {
                        charas.add(gattCharacteristic);
                        HashMap<String, String> currentCharaData = new HashMap<>();
                        currentCharaData.put(LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                        currentCharaData.put(LIST_UUID, uuid);
                        currentCharaData.put("VALUE", "N/A");
                        gattCharacteristicGroupData.add(currentCharaData);
                    }
                }
                if (!charas.isEmpty()) {
                    mGattCharacteristics.add(charas);
                    gattCharacteristicData.add(gattCharacteristicGroupData);
                }
            }
        }

        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                R.layout.group_item,
                new String[]{LIST_NAME, LIST_UUID},
                new int[]{android.R.id.text1, android.R.id.text2},
                gattCharacteristicData,
                R.layout.child_item,
                new String[]{LIST_NAME, "VALUE"},
                new int[]{R.id.characteristic_name, R.id.characteristic_value}
        );
        mGattServicesList.setAdapter(gattServiceAdapter);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
}
