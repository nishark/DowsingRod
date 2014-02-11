package edu.ucsb.cs.capstone.letmypeoplecode.dowsingrod;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.ArrayList;

import edu.ucsb.cs.capstone.letmypeoplecode.dowsingrod.util.SystemUiHider;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class DetectionLauncher extends Activity {
    Handler handler = new Handler();
    private static final int RESULT_SETTINGS = 1;
    private final static int REQUEST_ENABLE_BT = 1;
    private SharedPreferences sharedPref;
    private BluetoothAdapter mBluetoothAdapter;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override

                public void onLeScan(final BluetoothDevice device, final int rssi,
                                     byte[] scanRecord) {

                    waitPeriod = (long)(Math.exp(-0.1151292546 * rssi - 4.029523913)*1.2 + 100);
                    Log.d("bt_scan_results", device.toString() + " " + Integer.toString(rssi) + " " + waitPeriod);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((TextView) findViewById(R.id.rssiVal)).setText(Integer.toString(rssi));
                        }
                    });
                }
            };
    private double duration;
    private long waitPeriod=2000;
    private int flag=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detection_launcher);
        final View contentView = findViewById(R.id.fullscreen_content);
       // ((TextView) findViewById(R.id.rssiVal)).setText("fuck");

        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        // Start scanning for bluetooth devices
        mBluetoothAdapter.startLeScan(mLeScanCallback);

        // Hook in settings
        this.sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        // Disable stop button
        Button button = (Button)findViewById(R.id.stop_button);
        button.setEnabled(false);

        // Example of using the preferences from other project:
        // this.samplingRateInMilliseconds = Integer.parseInt(sharedPref.getString("sampling_rate", "20"));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.action_settings:
                Intent i = new Intent(this, SettingActivity.class);
                startActivityForResult(i, RESULT_SETTINGS);
                break;

        }

        return true;
    }

    public void genSound(View view) {
        Button button = (Button)findViewById(R.id.stop_button);
        button.setEnabled(true);
        button = (Button)findViewById(R.id.start_button);
        button.setEnabled(false);
        this.flag=1;
        this.duration = Double.parseDouble(sharedPref.getString("duration", "0.05"));
        final NoiseGenerator noise = new NoiseGenerator(duration);
        final Thread thread = new Thread(new Runnable() {
            public void run() {
//                noise.genTone(duration);
                //handler.post(new Runnable() {
                    //public void run() {
                        while(flag==1){
                            noise.playSound();
                            try{
                                Thread.sleep(waitPeriod);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            noise.rewind();
                        }
                    //}
                //});
            }
        });
        thread.start();
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = DetectionLauncher.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
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

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

    public void offSound(View view) {
        this.flag=0;
        Button button = (Button)findViewById(R.id.start_button);
        button.setEnabled(true);
        button = (Button)findViewById(R.id.stop_button);
        button.setEnabled(false);
    }

    public SharedPreferences getSharedPref() {
        return sharedPref;
    }

    public void setSharedPref(SharedPreferences sharedPref) {
        this.sharedPref = sharedPref;
    }
}
