package com.eec136b.davit.tempsensble;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = MainActivity.class.getSimpleName();

    private Toolbar mToolbar;
    private Button mScanButton;
    private Button mConnectButton;
    private Button mNotifButton;
    private Button mDiscoverButton;
    private TextView mTempTextView;
    private TextView mHeadingTextView;
    private TextView mAccelTextView;
    private TextView mHeartRateTextView;
    private TextView mSP02TextView;
    private TextView mBatteryLevelTextView;


    // Variables to manage BLE connection
    private static boolean mConnectState;
    private static boolean mServiceConnected;
    private static boolean mBleStarted;
    private static BleService mBleService;


    private static final int REQUEST_ENABLE_BLE = 1;

    // Required for Android 6.0 (Marshmallow)
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    /**
     * This manages the lifecycle of the BLE service.
     * When the service starts we get the service object and initialize the service.
     */

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        /**
         * This is called when the BleService is connected
         *
         * @param name the component name of the service that has been connected
         * @param service service being bound
         */
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "onServiceConnected");
            mBleService = ((BleService.LocalBinder) service).getService();
            mServiceConnected = true;
            Log.d(TAG, "mServiceConnected is Enabled");
            mBleService.initialize();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG,"onServiceDisconnected");
            mBleService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mToolbar = (Toolbar)(findViewById(R.id.toolbar));
        mScanButton = (Button)(findViewById(R.id.scan_button));
        mConnectButton = (Button)(findViewById(R.id.connect_button));
        mNotifButton = (Button)(findViewById(R.id.notifications_button));
        mDiscoverButton = (Button)(findViewById(R.id.discover_services_button));
        mTempTextView = (TextView)(findViewById(R.id.temp_sense_TV));
        mHeadingTextView = (TextView)(findViewById(R.id.heading_TV));
        mAccelTextView = (TextView)(findViewById(R.id.accel_TV));
        mHeartRateTextView = (TextView)(findViewById(R.id.heart_rate_TV));
        mSP02TextView = (TextView)(findViewById(R.id.sp02_TV));
        mBatteryLevelTextView = (TextView)(findViewById(R.id.battery_level_TV));
        mConnectState = false;
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);




        //This section required for Android 6.0 (Marshmallow)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check 
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access ");
                builder.setMessage("Please grant location access so this app can detect devices.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                builder.show();
            }
        } //End of section for Android 6.0 (Marshmallow)
    }

    //This method required for Android 6.0 (Marshmallow)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Permission for 6.0:", "Coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }
                    });
                    builder.show();
                }
            }
        }
    } //End of section for Android 6.0 (Marshmallow)


    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        //Inflate the menu; this adds items to the actionbar if it is present
        getMenuInflater().inflate(R.menu.menu_main,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        //Handle actionbar item clicks here
        //Actionbar will automatically handle up/back clicks
        //As long as a parent activity is specified in the manifest file

        int id = item.getItemId();

        if(id == R.id.action_bluetooth){
            if(!mBleStarted)
                startBluetooth();
            return true;
        }

        return super.onOptionsItemSelected(item);

    }
    @Override
    protected void onResume() {
        super.onResume();
        // Register the broadcast receiver. This specified the messages the main activity looks for from the PSoCCapSenseLedService
        final IntentFilter filter = new IntentFilter();
        filter.addAction(BleService.ACTION_BLESCAN_CALLBACK);
        filter.addAction(BleService.ACTION_CONNECTED);
        filter.addAction(BleService.ACTION_DISCONNECTED);
        filter.addAction(BleService.ACTION_SERVICES_DISCOVERED);
        filter.addAction(BleService.ACTION_DATA_RECEIVED);
        registerReceiver(mBleUpdateReceiver, filter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BLE && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mBleUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Close and unbind the service when the activity goes away
        mBleService.close();
        unbindService(mServiceConnection);
        mBleService = null;
        mServiceConnected = false;
    }

    public void startBluetooth(){
        mBleStarted = true;
        //Find the BLE service and adapter
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLE);
        }

        // Start the BLE Service
        Log.d(TAG, "Starting BLE Service");
        Intent gattServiceIntent = new Intent(this, BleService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        // Disable the start button and turn on the search  button
        mScanButton.setEnabled(true);
        Log.d(TAG, "Bluetooth is Enabled");
    }
    public void tempSenseScan(View view){
        Log.d(TAG," " + mServiceConnected);
        if(mServiceConnected) {
            mBleService.scan();
        }
        else {
            Toast.makeText(MainActivity.this,"Service not connected, will not scan",Toast.LENGTH_SHORT).show();
        }
    }

    public void tempSenseConnect(View view){
        mBleService.connect();
    }
    public void discoverServices(View view) { mBleService.discoverServices();}
    public void enableNotif(View view){
        mBleService.notify(true);
    }


    /**
     * Listener for BLE event broadcasts
     */
    private final BroadcastReceiver mBleUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case BleService.ACTION_BLESCAN_CALLBACK:
                    // Disable the search button and enable the connect button
                    mScanButton.setEnabled(false);
                    mConnectButton.setEnabled(true);
                    Toast.makeText(MainActivity.this,"CALLBACK received", Toast.LENGTH_SHORT).show();
                    break;
                case BleService.ACTION_CONNECTED:
                    /* This if statement is needed because we sometimes get a GATT_CONNECTED */
                    /* action when sending notifications */
                    if (!mConnectState) {
                        // Disable the connect button, discover services
                        mConnectButton.setEnabled(false);
                        mDiscoverButton.setEnabled(true);
                        mConnectState = true;

                        Log.d(TAG, "Connected to Device");
                        mHeadingTextView.setText("CONNECTED");
                        Toast.makeText(MainActivity.this,"Connected to device.",Toast.LENGTH_LONG).show();
                    }
                    break;
                case BleService.ACTION_DISCONNECTED:
                    // Disable the disconnect, discover svc, discover char button, and enable the search button
                    mConnectState = false;
                    mScanButton.setEnabled(true);
                    mDiscoverButton.setEnabled(false);
                    mNotifButton.setEnabled(false);
                    mConnectButton.setEnabled(false);
                    mHeadingTextView.setText("DISCONNECTED");
                    Log.d(TAG, "Disconnected");
                    break;
                case BleService.ACTION_SERVICES_DISCOVERED:
                    // Disable the discover services button
                    Log.d(TAG, "Services Discovered");
                    mDiscoverButton.setEnabled(false);
                    mNotifButton.setEnabled(true);
                    break;
                case BleService.ACTION_DATA_RECEIVED:
                    // This is called after a notify or a read completes
                    mNotifButton.setEnabled(false);
                    String tempVal = mBleService.getTemperature();
                    mHeadingTextView.setText("RECEIVING DATA");
                    mTempTextView.setText(tempVal);
                    mAccelTextView.setText(mBleService.getXYZ());
                    mHeartRateTextView.setText(mBleService.getHeartRate());
                    mSP02TextView.setText(mBleService.getSP02());
                    mBatteryLevelTextView.setText(mBleService.getBatteryLevel());

                default:
                    break;
            }
        }
    };
}
