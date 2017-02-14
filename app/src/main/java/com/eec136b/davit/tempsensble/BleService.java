package com.eec136b.davit.tempsensble;

/**
 * Created by Davit on 1/24/2017.
 */

//TODO
//Add your package here
//Example: com.davit.drivers.bluetoothlowenergy will be used for this driver

import android.annotation.TargetApi;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.bluetooth.BluetoothGattCharacteristic;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@TargetApi(Build.VERSION_CODES.LOLLIPOP) //This is required to use w/ lollipop
public class BleService extends Service {

    private final static String TAG  = BleService.class.getSimpleName();



    //Bluetooth Objects that we need to interact with
    private static BluetoothManager mBluetoothManager;
    private static BluetoothAdapter mBluetoothAdapter;
    private static BluetoothLeScanner mLEScanner;
    private static BluetoothDevice mLeDevice;
    private static BluetoothGatt mBluetoothGatt;

    private static final int ACCELROMETER = 0;
    private static final int BATTERY = 1;
    private static final int HEARTRATE = 2;
    private static final int OXIMETRY = 3;
    private static final int TEMPERATURE = 4;

    /**	Bluetooth characteristics that we need to read or write to
     *	Place characteristics here that are in the PSoC
     *	For example, if in the PSoC we have a custom charactertic named HeartRate
     *	Have a variable for that and if the descriptor will be used, then have a variable
     *	for that as well
     *
     *	EXAMPLE:
     *	private static BluetoothGattCharacteristic mHeartRateCharacteristic;
     *	private static BluetoothGattDescriptor mHeartRateDescriptor
     */



    //TODO
    //Bluetooth Characteristics
    private static BluetoothGattCharacteristic mTemperatureCharacteristic;
    private static BluetoothGattCharacteristic mAccelCharacteristic;
    private static BluetoothGattCharacteristic mHeartRateCharacteristic;
    private static BluetoothGattCharacteristic mOximetryCharacteristic;
    private static BluetoothGattCharacteristic mBatteryCharacteristic;

    private static BluetoothGattDescriptor mTempDescriptor;
    private static BluetoothGattDescriptor mAccelDescriptor;
    private static BluetoothGattDescriptor mHeartRateDescriptor;
    private static BluetoothGattDescriptor mOximetryDescriptor;
    private static BluetoothGattDescriptor mBatteryDescriptor;
    //End Bluetooth Characteristics



    //TODO
    /**
     *	Each characteristic and service needs a UUID
     *	EXAMPLE:
     *	private final static String mBleServiceUUID = "UUID GOES HERE"
     *
     *
     *
     */

        /*

        TempSensor: 109283C8-76F1-40AD-8B6A-3557ECC82441
	        Temperature: D5A8F5DD-D680-4907-A1E0-2D84C92BBBDB
            Client Char..: 00002902-0000-1000-8000-00805F9B34FB
		    Custom Desc. : ECB8A685-D8A2-4CFF-9587-429B7F514085
     */
    private final static String mBleServiceUUID = "0000AAA0-0000-1000-8000-00805F9B34FB";

    private final static String mAccelCharacteristicUUID = "0000AAA1-0000-1000-8000-00805F9B34FB";
    private final static String mBatteryCharacteristicUUID = "0000AAA2-0000-1000-8000-00805F9B34FB";
    private final static String mHeartRateCharacteristicUUID = "0000AAA3-0000-1000-8000-00805F9B34FB";
    private final static String mOximetryCharacteristicUUID = "0000AAA4-0000-1000-8000-00805F9B34FB";
    private final static String mTemperatureCharacteristicUUID = "0000AAA5-0000-1000-8000-00805F9B34FB";
    //CCCDUUID for each characteristic's description
    private final static String CCCDUUID = "00002902-0000-1000-8000-00805F9B34FB";

    //TODO
    //Add remaining variables for service here
    private static int mTemp = 0;
    private static int mSP02 = 0;
    private static int mHeartRate = 0;
    private static int mHeartRatePrev = 0;
    private static int mXYZ = 0;
    private static int mBatteryLevel;
    private static byte xAccel=0,yAccel=0,zAccel=0;
    private static double xG = 0.0, yG = 0.0, zG = 0.0;
    private boolean enabled;
    //TODO
    //Change the package in here to the proper one for custom project
    //Actions used during broadcasts to the main activity
    public final static String ACTION_BLESCAN_CALLBACK = "com.davit.drivers.bluetoothlowenergy.ACTION_BLESCAN_CALLBACK";
    public final static String ACTION_CONNECTED = "com.davit.drivers.bluetoothlowenergy.ACTION_CONNECTED";
    public final static String ACTION_DISCONNECTED = "com.davit.drivers.bluetoothlowenergy.ACTION_DISCONNECTED";
    public final static String ACTION_SERVICES_DISCOVERED = "com.davit.drivers.bluetoothlowenergy.ACTION_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_RECEIVED = "com.davit.drivers.bluetoothlowenergy.ACTION_DATA_RECEIVED";


    /**
     *  Values for finding temperature
     */
    int i;

    // Floats used because the calculation for m and b
    // were equating to zero.
    float V1, V2, T1, T2;

    // Need to be floats to properly calculate temperature
    float m = 0, b = 0;

    // Voltage index (mVolts)
    int[] a = {1375, 1350, 1300, 1250, 1199, 1149, 1097,
            1046, 995, 943, 891, 838, 786, 733, 680, 627,
            574, 520, 466, 412, 358, 302};
    // Temperature index (C)
    int[] c = {-55, -50, -40, -30, -20, -10, 0,
            10, 20, 30, 40, 50, 60, 70, 80, 90, 100,
            110, 120, 130, 140, 150};

    private int index;

    public BleService()
    {

    }

    public class LocalBinder extends Binder
    {
        BleService getService() { return BleService.this; }
    }

    @Override
    public IBinder onBind(Intent intent) {return mBinder;}

    @Override
    public boolean onUnbind(Intent intent){
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    public boolean initialize(){
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager

        if(mBluetoothManager == null){
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if(mBluetoothAdapter == null){
            Log.e(TAG, "Unable to obtain a BluetoothAdapter");
            return false;
        }

        return true;
    }

    public void scan() {
		/* Scan for devices and look for the one with the service that we want */
        UUID   bleService =       UUID.fromString(mBleServiceUUID);
        UUID[] bleServiceArray = {bleService};

        // Use old scan method for versions older than lollipop
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            //noinspection deprecation
            mBluetoothAdapter.startLeScan(bleServiceArray, mLeScanCallback);
        } else { // New BLE scanning introduced in LOLLIPOP
            ScanSettings settings;
            List<ScanFilter> filters;
            mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
            settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            filters = new ArrayList<>();
            // We will scan just for the CAR's UUID
            ParcelUuid PUuid = new ParcelUuid(bleService);
            ScanFilter filter = new ScanFilter.Builder().setServiceUuid(PUuid).build();
            filters.add(filter);
            mLEScanner.startScan(filters, settings, mScanCallback);
        }
    }
    public boolean connect(){

        if(mBluetoothAdapter == null){
            Log.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }

        // Previously connected device. Try to reconnect.
        if(mBluetoothGatt != null){
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            return mBluetoothGatt.connect();
        }

        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false;

        mBluetoothGatt = mLeDevice.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "trying to create a new connection.");

        return true;
    }

    public void discoverServices() {
        if(mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.discoverServices();
    }
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    mLeDevice = device;
                    //noinspection deprecation
                    mBluetoothAdapter.stopLeScan(mLeScanCallback); // Stop scanning after the first device is found
                    broadcastUpdate(ACTION_BLESCAN_CALLBACK); // Tell the main activity that a device has been found
                }
            };


    /**
     * Implements the callback for when scanning for devices has found a device with
     * the service we are looking for.
     *
     * This is the callback for BLE scanning for LOLLIPOP and later
     */
    private final ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            mLeDevice = result.getDevice();
            mLEScanner.stopScan(mScanCallback); // Stop scanning after the first device is found
            broadcastUpdate(ACTION_BLESCAN_CALLBACK); // Tell the main activity that a device has been found
        }
    };


    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if(newState == BluetoothProfile.STATE_CONNECTED){
                broadcastUpdate(ACTION_CONNECTED);
                Log.i(TAG, "Connected to GATT server.");
            }else if (newState == BluetoothProfile.STATE_DISCONNECTED){
                Log.i(TAG, "Disconnected from GATT server.");
                close();
                broadcastUpdate(ACTION_DISCONNECTED);
            }
        }


        /**
         * When the descriptor is done writing we want to increase the index and set the notification
         * for the next characteristic
         */
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status){
            index++;
            enableNotification();
        }



        /**
         * This is called when a service discovery has completed.
         *
         * It gets the characteristics we are interested in and then
         * broadcasts an update to the main activity.
         *
         * @param gatt The GATT database object
         * @param status Status of whether the write was successful.
         */
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            // Get just the service that we are looking for
            BluetoothGattService mService = gatt.getService(UUID.fromString(mBleServiceUUID));

                /* Get characteristics from our desired service */
            mTemperatureCharacteristic = mService.getCharacteristic(UUID.fromString(mTemperatureCharacteristicUUID));
            mAccelCharacteristic = mService.getCharacteristic(UUID.fromString(mAccelCharacteristicUUID));
            mHeartRateCharacteristic = mService.getCharacteristic(UUID.fromString(mHeartRateCharacteristicUUID));
            mOximetryCharacteristic = mService.getCharacteristic(UUID.fromString(mOximetryCharacteristicUUID));
            mBatteryCharacteristic = mService.getCharacteristic(UUID.fromString(mBatteryCharacteristicUUID));


            mTempDescriptor = mTemperatureCharacteristic.getDescriptor(UUID.fromString(CCCDUUID));
            mAccelDescriptor = mAccelCharacteristic.getDescriptor(UUID.fromString(CCCDUUID));
            mHeartRateDescriptor = mHeartRateCharacteristic.getDescriptor(UUID.fromString(CCCDUUID));
            mOximetryDescriptor = mOximetryCharacteristic.getDescriptor(UUID.fromString(CCCDUUID));
            mBatteryDescriptor = mBatteryCharacteristic.getDescriptor(UUID.fromString(CCCDUUID));

            // Broadcast that service/characteristic/descriptor discovery is done
            broadcastUpdate(ACTION_SERVICES_DISCOVERED);
        }
        //TODO
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {

            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Verify that the read was the LED state
                String uuid = characteristic.getUuid().toString();
                if(uuid.equalsIgnoreCase(mTemperatureCharacteristicUUID)) {
                    mTemp = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32,0);
//                    Log.d(TAG,uuid);
                }
                if(uuid.equalsIgnoreCase(mAccelCharacteristicUUID)) {
//                    Log.d(TAG,uuid);

                    mXYZ = mAccelCharacteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0);
                    xAccel = (byte)((mXYZ & 0x00FF0000) >> 16);
                    yAccel = (byte)((mXYZ & 0x0000FF00) >> 8);
                    zAccel = (byte)(mXYZ & 0x000000FF);

                    xG = ((double)xAccel)/63.5;
                    yG = ((double)yAccel)/63.5;
                    zG = ((double)zAccel)/63.5;
                }
                if(uuid.equalsIgnoreCase(mHeartRateCharacteristicUUID)) {
                    mHeartRate = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32,0);
                    if(mHeartRate == mHeartRatePrev && mHeartRatePrev != 0){
                        Log.d(TAG, "Same Heart Rate as Before");
                    }
                    mHeartRatePrev = mHeartRate;
//                    Log.d(TAG,uuid);
                }
                if(uuid.equalsIgnoreCase(mOximetryCharacteristicUUID)) {
                    mSP02 = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32,0);
//                    Log.d(TAG,uuid);
                }
                if(uuid.equalsIgnoreCase(mBatteryCharacteristicUUID)){
                    mBatteryLevel = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32,0);
                    mBatteryLevel = mBatteryLevel * 2;
                }
                // Notify the main activity that new data is available
                broadcastUpdate(ACTION_DATA_RECEIVED);
            }
        }




        //TODO
        /**
         * This is called when a characteristic with notify set changes.
         * It broadcasts an update to the main activity with the changed data.
         *
         * @param gatt The GATT database object
         * @param characteristic The characteristic that was changed
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {

            String uuid = characteristic.getUuid().toString();
            // In this case, the only notification the apps gets is the CapSense value.
            // If the application had additional notifications we could
            // use a switch statement here to operate on each one separately.



            if(uuid.equalsIgnoreCase(mTemperatureCharacteristicUUID)) {
                mTemp = mTemperatureCharacteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0);
            }else if(uuid.equalsIgnoreCase(mAccelCharacteristicUUID)) {
                /**
                 * if > 127
                 *  x = (-1 * (~x+1))/128
                 */
                mXYZ = mAccelCharacteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0);
                xAccel = (byte)((mXYZ & 0x00FF0000) >> 16);
                yAccel = (byte)((mXYZ & 0x0000FF00) >> 8);
                zAccel = (byte)(mXYZ & 0x000000FF);


                xG = ((double)xAccel)/63.5;
                yG = ((double)yAccel)/63.5;
                zG = ((double)zAccel)/63.5;

            } else if(uuid.equalsIgnoreCase(mHeartRateCharacteristicUUID)) {
                mHeartRate = mHeartRateCharacteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0);
                if(mHeartRate == mHeartRatePrev && mHeartRatePrev != 0){
                    Log.d(TAG, "Same Heart Rate as Before");
                }
                mHeartRatePrev = mHeartRate;
            } else if(uuid.equalsIgnoreCase(mOximetryCharacteristicUUID)) {

                mSP02 = mOximetryCharacteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0);
            } else if(uuid.equalsIgnoreCase(mBatteryCharacteristicUUID)){
                mBatteryLevel = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32,0);
                mBatteryLevel = mBatteryLevel * 2;
            }
            // Notify the main activity that new data is available
            broadcastUpdate(ACTION_DATA_RECEIVED);
        }
    };


    //TODO
    public void enableNotification() {
        /**
         * Index here gets increased from the other callback function onDescriptorWrite
         * This is so that the BLE can process all the requests and can handle the requests
         * on its own time without being overloaded
         */
        switch(index)
        {
            case ACCELROMETER:
                mBluetoothGatt.setCharacteristicNotification(mAccelCharacteristic,enabled);
                mAccelDescriptor.setValue((enabled) ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                mBluetoothGatt.writeDescriptor(mAccelDescriptor);
                break;
            case BATTERY:
                mBluetoothGatt.setCharacteristicNotification(mBatteryCharacteristic,enabled);
                mBatteryDescriptor.setValue((enabled) ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                mBluetoothGatt.writeDescriptor(mBatteryDescriptor);
            case HEARTRATE:
                mBluetoothGatt.setCharacteristicNotification(mHeartRateCharacteristic,enabled);
                mHeartRateDescriptor.setValue((enabled) ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                mBluetoothGatt.writeDescriptor(mHeartRateDescriptor);
                break;
            case OXIMETRY:
                mBluetoothGatt.setCharacteristicNotification(mOximetryCharacteristic,enabled);
                mOximetryDescriptor.setValue((enabled) ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                mBluetoothGatt.writeDescriptor(mOximetryDescriptor);
                break;
            case TEMPERATURE:
                mBluetoothGatt.setCharacteristicNotification(mTemperatureCharacteristic,enabled);
                mTempDescriptor.setValue((enabled) ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                mBluetoothGatt.writeDescriptor(mTempDescriptor);
                break;

        }

    }

    public void notify(boolean enabled){
        index = 0;
        this.enabled = enabled;
        enableNotification();
    }
    /**
     * Sends a broadcast to the listener in the main activity.
     *
     * @param action The type of action that occurred.
     */
    private void broadcastUpdate(final String action) {
    final Intent intent = new Intent(action);
            sendBroadcast(intent);
    }

    public String getTemperature(){
        float  value = 0;
        for(i = 0; i < a.length;i++) {
            if (mTemp > a[i]) {
                // Voltage index below current reading
                V1 = a[i];
                // Voltage index above current reading
                V2 = a[i - 1];

                // Temp above
                T1 = c[i];
                // Temp below
                T2 = c[i - 1];
                // Slope calculation
                // In case we want to reduce the number
                // of variables, we can use the equation
                // below.
                // m = (c[i-1] - c[i]) / (a[i-1] - a[i]);
//                m = ((T2 - T1) / (V2 - V1));
                m = ((T1 - T2) / (V1 - V2));

                // Intercept calculation
                b = T1 - (m * V1);

                // Temperature calculation
                value = m * mTemp + b;
            }
        }
        return String.format(Locale.getDefault(),"%.2f",value);
    }

    public String getHeartRate(){
        return "Heart Rate: " + Integer.toString(mHeartRate) + "bpm";
    }

    public String getSP02(){
        return "SP02: " + Integer.toString(mSP02);

    }

    public String getBatteryLevel(){
        return "Battery Level: " + Integer.toString(mBatteryLevel) + "mV";
    }

    public String getXYZ(){
        return String.format(Locale.getDefault(),"X: %.2f Y: %.2f Z: %.2f", xG, yG, zG);
//        return String.format(Locale.getDefault(),"X: %c Y: %c Z: %c", xAccel, yAccel, zAccel);
        //return xG + " " + yG + " " + zG;
    }
}
