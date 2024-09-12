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

import java.util.HashMap;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class SampleGattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static String HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    public static String GAS_CONCENTRATION_CO = "19b10001-e8f4-537e-4f6c-d104768a1214";
    public static String TEMPERATURE = "00002a6e-0000-1000-8000-00805f9b34fb";
    public static String HUMIDITY = "00002a6f-0000-1000-8000-00805f9b34fb";
    public static String PRESSURE = "00002a6d-0000-1000-8000-00805f9b34fb";
    public static String CO = "00002bd0-0000-1000-8000-00805f9b34fb";
    public static String ENVIRONMENTAL_MONITORING_SERVICE = "0000181a-0000-1000-8000-00805f9b34fb";

    static {
        // Sample Services.
        attributes.put("0000180d-0000-1000-8000-00805f9b34fb", "Heart Rate Service");
        attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service");
        attributes.put(ENVIRONMENTAL_MONITORING_SERVICE , "Environmental Monitoring Service");
        attributes.put(TEMPERATURE, "Temperature");
        attributes.put(HUMIDITY, "Humidity");

        attributes.put(CO, "CO concentration");
        attributes.put(PRESSURE, "Pressure");
        // Sample Characteristics.
        attributes.put(HEART_RATE_MEASUREMENT, "Heart Rate Measurement");
        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");

        attributes.put(GAS_CONCENTRATION_CO, "Carbon Monoxide (CO)");
    }

    public static String     lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}