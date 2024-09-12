/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.bluetoothlegatt;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";


    private TextView mConnectionState;


    private String mDeviceName;
    private String mDeviceAddress;
    private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private BluetoothGattCharacteristic mCharacteristic;

    private int [] mTrendData = new int[15];
    private int mTimeSeconds = 0;

    private CountDownTimer cTimer = null;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    public final static UUID UUID_HEART_RATE_MEASUREMENT =
            UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT);


    public final static UUID UUID_TEMPERATURE =
            UUID.fromString(SampleGattAttributes.TEMPERATURE);

    public final static UUID UUID_CO =
            UUID.fromString(SampleGattAttributes.CO);

    public final static UUID UUID_PRESSURE =
            UUID.fromString(SampleGattAttributes.PRESSURE);

    public final static UUID UUID_HUMIDITY =
            UUID.fromString(SampleGattAttributes.HUMIDITY);

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                invalidateOptionsMenu();
                updateConnectionState(R.string.connected);
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                String dataType = intent.getStringExtra(BluetoothLeService.EXTRA_DATA_TYPE);
                String dataValue = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);

                // Update the ExpandableListView with the data
                updateCharacteristicValue(dataType, dataValue);
            }
        }
    };

    private void updateCharacteristicValue(String dataType, String dataValue) {
        // Loop through the characteristics and update the corresponding value
        for (int i = 0; i < mGattCharacteristics.size(); i++) {
            ArrayList<BluetoothGattCharacteristic> charas = mGattCharacteristics.get(i);
            for (BluetoothGattCharacteristic characteristic : charas) {
                if (characteristic.getUuid().toString().equals(dataType)) {
                    // Get the child item in the ExpandableListView
                    HashMap<String, String> childItem = (HashMap<String, String>)
                            ((SimpleExpandableListAdapter) mGattServicesList.getExpandableListAdapter())
                                    .getChild(i, charas.indexOf(characteristic));

                    // Update the child item's value with the new data
                    if (childItem != null) {
                        childItem.put("VALUE", dataValue);
                    }

                    // Notify the adapter of the change
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

        // Get the On and Off buttons
        Button buttonOn = findViewById(R.id.button_on);
        Button buttonOff = findViewById(R.id.button_off);

        // Set the On button functionality
        buttonOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enableNotifications(true);
            }
        });

        // Set the Off button functionality
        buttonOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enableNotifications(false);
            }
        });

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.
        mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
        mConnectionState = (TextView) findViewById(R.id.connection_state);

        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    private void enableNotificationWithDelay(final BluetoothGattCharacteristic characteristic, final boolean enabled, int delay) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mBluetoothLeService.setCharacteristicNotification(characteristic, enabled);
            }
        }, delay);
    }

    // Enable notifications for all four characteristics
    private void enableNotifications(boolean enabled) {
        if (mBluetoothLeService == null || mGattCharacteristics == null) {
            return;
        }

        // Set notifications for each characteristic with a small delay
        int delay = 0;
        for (ArrayList<BluetoothGattCharacteristic> charas : mGattCharacteristics) {
            for (BluetoothGattCharacteristic characteristic : charas) {
                UUID uuid = characteristic.getUuid();
                if (
                        UUID_CO.equals(uuid) ||
                        UUID_TEMPERATURE.equals(uuid) ||
                        UUID_HUMIDITY.equals(uuid) ||
                        UUID_PRESSURE.equals(uuid)) {

                    // Add a delay to ensure operations are queued
                    enableNotificationWithDelay(characteristic, enabled, delay);
                    delay += 200;  // Add 200ms delay between each operation
                }
            }
        }
    }



    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
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
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;

            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }



    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
// In this sample, we populate the data structure that is bound to the ExpandableListView
// on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;

        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData = new ArrayList<>();
        mGattCharacteristics = new ArrayList<>();

        // Define the UUIDs of interest
        Set<String> relevantUuids = new HashSet<>(Arrays.asList(
                "00002a6e-0000-1000-8000-00805f9b34fb", // Temperature
                "00002a6f-0000-1000-8000-00805f9b34fb", // Humidity
                "00002bd0-0000-1000-8000-00805f9b34fb", // CO concentration
                "00002a6d-0000-1000-8000-00805f9b34fb"  // Pressure
        ));

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<>();
            String uuid = gattService.getUuid().toString();
            if (uuid.startsWith("0000181a") || uuid.equals("19b10001-e8f4-537e-4f6c-d104768a1214")) {
                currentServiceData.put(LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
                currentServiceData.put(LIST_UUID, uuid);
                gattServiceData.add(currentServiceData);

                ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<>();
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<>();

                // Loops through available Characteristics.
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    uuid = gattCharacteristic.getUuid().toString();
                    if (relevantUuids.contains(uuid)) {
                        charas.add(gattCharacteristic);

                        HashMap<String, String> currentCharaData = new HashMap<>();
                        currentCharaData.put(LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                        currentCharaData.put(LIST_UUID, uuid);

                        // Add a placeholder for characteristic value (will be updated later)
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
                android.R.layout.simple_expandable_list_item_2,
                new String[] { LIST_NAME, LIST_UUID },
                new int[] { android.R.id.text1, android.R.id.text2 },
                gattCharacteristicData,
                R.layout.child_item, // Use custom child layout
                new String[] { LIST_NAME, "VALUE" }, // Display name and value
                new int[] { R.id.characteristic_name, R.id.characteristic_value } // Bind data
        );
        mGattServicesList.setAdapter(gattServiceAdapter);
    }



    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }



}