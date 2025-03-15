package com.example.android.bluetoothlegatt;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class DeviceScanActivity extends AppCompatActivity {
    private static final String TAG = "BTCOM Device List";

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_LOCATION_PERMISSION = 101;
    private static final long SCAN_PERIOD = 10000; // 10 seconds

    private BluetoothAdapter mBluetoothAdapter;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private boolean mScanning = false;
    private Handler mHandler;

    // UI elements
    private ListView mListView;
    private TextView mScanStatus;
    private Button mScanButton;

    // FTP credentials (if still needed)
    private static final String FTPHostName = "ftp.weng.oucreate.com";
    private static final int FTPPort = 21;
    private static final String FTPUsername = "yang@ftp.weng.oucreate.com";
    private static final String FTPPassword = "NPLOUWeng2848";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_scan);

        // Optional: Set title if using native ActionBar
        if (getActionBar() != null) {
            getActionBar().setTitle(R.string.title_devices);
        } else {
            Log.w(TAG, "ActionBar is null");
        }

        mHandler = new Handler();

        // Check if BLE is supported
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Bluetooth adapter
        BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            Toast.makeText(this, "Bluetooth Manager not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set up UI references
        mScanStatus = findViewById(R.id.scan_status);
        mScanButton = findViewById(R.id.button_scan);
        mListView = findViewById(R.id.device_listview);

        // Initialize the list adapter
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        mListView.setAdapter(mLeDeviceListAdapter);

        // Handle device item clicks
        mListView.setOnItemClickListener((parent, view, position, id) -> {
            final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
            if (device == null) return;
            Intent intent = new Intent(DeviceScanActivity.this, DeviceControlActivity.class);
            intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
            intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
            if (mScanning) {
                stopScan();
            }
            startActivity(intent);
        });

        // Set up the Start/Stop Scan button
        mScanButton.setOnClickListener(v -> {
            if (!mScanning) {
                startScan();
            } else {
                stopScan();
            }
        });

        Button instructionsBtn = findViewById(R.id.button_instructions);
        instructionsBtn.setOnClickListener(v -> {
            startActivity(new Intent(this, InstructionActivity.class));
        });
        Button settingsButton = findViewById(R.id.button_settings);
        settingsButton.setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
        });


    }

    @Override
    protected void onResume() {
        super.onResume();

        // Check location permission for BLE scanning
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION
            );
            return; // Wait for the permission result
        }

        // Ensure Bluetooth is enabled
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop scanning if active
        if (mScanning) {
            stopScan();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            Toast.makeText(this, "Bluetooth must be enabled", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // Handle location permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
            } else {
                new AlertDialog.Builder(this)
                        .setTitle("Location Permission Required")
                        .setMessage("Location permission is required for BLE scanning. The app will now close.")
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> finish())
                        .setCancelable(false)
                        .show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    // Start scanning
    private void startScan() {
        mLeDeviceListAdapter.clear();
        mLeDeviceListAdapter.notifyDataSetChanged();

        mScanning = true;
        mScanStatus.setText("Scanning...");
        mScanButton.setText("Stop Scan");

        // Begin BLE scan
        Log.i(TAG, "Starting BLE scan...");
        mBluetoothAdapter.startLeScan(mLeScanCallback);

        // Automatically stop scanning after SCAN_PERIOD
        mHandler.postDelayed(() -> {
            if (mScanning) {
                stopScan();
            }
        }, SCAN_PERIOD);
    }

    // Stop scanning
    private void stopScan() {
        mScanning = false;
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
        mScanStatus.setText("Not Scanning");
        mScanButton.setText("Start Scan");
        Log.i(TAG, "BLE scan stopped.");
    }



    // BLE scan callback
    private final BluetoothAdapter.LeScanCallback mLeScanCallback = (device, rssi, scanRecord) ->
            runOnUiThread(() -> {
                mLeDeviceListAdapter.addDevice(device);
                mLeDeviceListAdapter.notifyDataSetChanged();
            });

    // Adapter for holding discovered BLE devices
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<>();
            mInflator = DeviceScanActivity.this.getLayoutInflater();
            Log.i(TAG, "Device list adapter created.");
        }

        public void addDevice(BluetoothDevice device) {
            String deviceName = device.getName();
            Log.d(TAG, "Discovered device: " + deviceName);
            // Filter for device names that start with "Univ. Okla"
            if (deviceName != null
                    && deviceName.startsWith("Univ. Okla")
                    && !mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        // Inflate each row of the ListView with listitem_device.xml
        @Override
        public View getView(int i, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = mInflator.inflate(R.layout.listitem_device, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.deviceName = convertView.findViewById(R.id.device_name);
                viewHolder.deviceAddress = convertView.findViewById(R.id.device_address);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0) {
                viewHolder.deviceName.setText(deviceName);
            } else {
                viewHolder.deviceName.setText(R.string.unknown_device);
            }
            viewHolder.deviceAddress.setText(device.getAddress());

            return convertView;
        }
    }

    private static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

    // FTP example code (unchanged)
    public FTPClient connectftp() {
        FTPClient ftp = new FTPClient();
        ftp.setConnectTimeout(5000);
        try {
            ftp.connect(FTPHostName, FTPPort);
            boolean status = ftp.login(FTPUsername, FTPPassword);
            if (status) {
                ftp.enterLocalPassiveMode();
                ftp.setFileType(FTP.BINARY_FILE_TYPE);
                ftp.sendCommand("OPTS UTF8 ON");
            }
            Log.d(TAG, "FTP status: " + ftp.getStatus());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ftp;
    }
}
