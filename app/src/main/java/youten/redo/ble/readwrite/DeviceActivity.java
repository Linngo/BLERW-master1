/*
 * Copyright (C) 2013 youten
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
package youten.redo.ble.readwrite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import youten.redo.ble.msg.Msg;
import youten.redo.ble.msg.MsgAdapter;
import youten.redo.ble.util.BleUtil;
import youten.redo.ble.util.BleUuid;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

/**
 * BLEデバイスへのconnect・Service
 * Discoveryを実施し、Characteristicsのread/writeをハンドリングするActivity
 */
public class DeviceActivity extends Activity implements View.OnClickListener {
	private static final String TAG = "BLEDevice";

	public static final String EXTRA_BLUETOOTH_DEVICE = "BT_DEVICE";
	private BluetoothAdapter mBTAdapter;
	private BluetoothDevice mDevice;
	private BluetoothGatt mConnGatt;
	private int mStatus;

    private boolean flag=true;

    BluetoothGattCharacteristic characteristic;

	private Button mReadManufacturerNameButton;
	private Button mReadSerialNumberButton;
	private Button mWriteAlertLevelButton;
    private Button mcleardata;

    private ToggleButton mSetPROPERTY_NOTIFY;
    private TextView readdata;

    private MsgAdapter adapter;
    private List<Msg> msgList = new ArrayList<Msg>();
    private ListView msgListView;

    private final BluetoothGattCallback mGattcallback = new BluetoothGattCallback() {
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status,
				int newState) {
			if (newState == BluetoothProfile.STATE_CONNECTED) {
				mStatus = newState;
				mConnGatt.discoverServices();
			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
				mStatus = newState;
				runOnUiThread(new Runnable() {
					public void run() {
						mReadManufacturerNameButton.setEnabled(false);
						mReadSerialNumberButton.setEnabled(false);
						mWriteAlertLevelButton.setEnabled(false);
					}
				});
			}
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			for (BluetoothGattService service : gatt.getServices()) {
				if ((service == null) || (service.getUuid() == null)) {
					continue;
				}
				if (BleUuid.SERVICE_DEVICE_INFORMATION.equalsIgnoreCase(service
						.getUuid().toString())) {
					mReadManufacturerNameButton
							.setTag(service.getCharacteristic(UUID
									.fromString(BleUuid.CHAR_MANUFACTURER_NAME_STRING)));
					mReadSerialNumberButton
							.setTag(service.getCharacteristic(UUID
									.fromString(BleUuid.CHAR_SERIAL_NUMBEAR_STRING)));
					runOnUiThread(new Runnable() {
						public void run() {
							mReadManufacturerNameButton.setEnabled(true);
							mReadSerialNumberButton.setEnabled(true);
						}
					});
				}
				if (BleUuid.SERVICE_IMMEDIATE_ALERT.equalsIgnoreCase(service
						.getUuid().toString())) {
					runOnUiThread(new Runnable() {
						public void run() {
							mWriteAlertLevelButton.setEnabled(true);
                            mSetPROPERTY_NOTIFY.setEnabled(true);
						}
					});

					mWriteAlertLevelButton.setTag(service
							.getCharacteristic(UUID
									.fromString(BleUuid.CHAR_DATA_LEVEL)));
                    characteristic = service.getCharacteristic(UUID
                            .fromString(BleUuid.CHAR_DATA_LEVEL));
                    mSetPROPERTY_NOTIFY.setTag(characteristic.getDescriptor(UUID
                                    .fromString(BleUuid.CHAR_PROPERTY_NOTIFY)));
                    setCharacteristicNotification(characteristic,false);
				}
			}

			runOnUiThread(new Runnable() {
				public void run() {
					setProgressBarIndeterminateVisibility(false);
				}
			});
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				if (BleUuid.CHAR_MANUFACTURER_NAME_STRING
						.equalsIgnoreCase(characteristic.getUuid().toString())) {
					final String name = characteristic.getStringValue(0);

					runOnUiThread(new Runnable() {
						public void run() {
							mReadManufacturerNameButton.setText(name);
							setProgressBarIndeterminateVisibility(false);
						}
					});
				} else if (BleUuid.CHAR_SERIAL_NUMBEAR_STRING
						.equalsIgnoreCase(characteristic.getUuid().toString())) {
					final String name = characteristic.getStringValue(0);

					runOnUiThread(new Runnable() {
						public void run() {
							mReadSerialNumberButton.setText(name);
							setProgressBarIndeterminateVisibility(false);
						}
					});
				}
			}
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {

			runOnUiThread(new Runnable() {
				public void run() {
					setProgressBarIndeterminateVisibility(false);
				}
			});
		}
		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt,
											BluetoothGattCharacteristic characteristic) {
//            Log.w(TAG, " have data change");
          	final String name = characteristic.getStringValue(0);

          	runOnUiThread(new Runnable() {
            public void run() {
                Msg msg = new Msg(name, Msg.TYPE_RECEIVED);
                msgList.add(msg);
                adapter.notifyDataSetChanged();
                msgListView.setSelection(msgList.size());
                setProgressBarIndeterminateVisibility(false);
                }
            });
        }
        @Override
        public void onDescriptorRead(BluetoothGatt gatt,
                                     BluetoothGattDescriptor descriptor, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (BleUuid.CHAR_PROPERTY_NOTIFY
                        .equalsIgnoreCase(descriptor.getUuid().toString())) {
                    final String name = Arrays.toString(descriptor.getValue());
                    runOnUiThread(new Runnable() {
                        public void run() {
                            readdata.setText(name);
                            setProgressBarIndeterminateVisibility(false);
                        }
                    });
                }
            }
        }
	};

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {

        mConnGatt.setCharacteristicNotification(characteristic, enabled);

            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(BleUuid.CHAR_PROPERTY_NOTIFY));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mConnGatt.writeDescriptor(descriptor);
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_device);

		// state
		mStatus = BluetoothProfile.STATE_DISCONNECTED;
		mReadManufacturerNameButton = (Button) findViewById(R.id.read_manufacturer_name_button);
		mReadManufacturerNameButton.setOnClickListener(this);
		mReadSerialNumberButton = (Button) findViewById(R.id.read_serial_number_button);
		mReadSerialNumberButton.setOnClickListener(this);
		mWriteAlertLevelButton = (Button) findViewById(R.id.write_alert_level_button);
		mWriteAlertLevelButton.setOnClickListener(this);
        mcleardata = (Button) findViewById(R.id.clean);
        mcleardata.setOnClickListener(this);

        mSetPROPERTY_NOTIFY = (ToggleButton) findViewById(R.id.PROPERTY_NOTIFY);
        mSetPROPERTY_NOTIFY.setOnClickListener(this);
        readdata = (TextView) findViewById(R.id.textView);

        adapter = new MsgAdapter(this, R.layout.msg_item, msgList);
        msgListView = (ListView)findViewById(R.id.msg_list_view);
        msgListView.setAdapter(adapter);
	}

	@Override
	protected void onResume() {
		super.onResume();

		init();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mConnGatt != null) {
			if ((mStatus != BluetoothProfile.STATE_DISCONNECTING)
					&& (mStatus != BluetoothProfile.STATE_DISCONNECTED)) {
				mConnGatt.disconnect();
			}
			mConnGatt.close();
			mConnGatt = null;
		}
	}

	@Override
	public void onClick(View v) {
        if (v.getId() == R.id.read_manufacturer_name_button) {
            if ((v.getTag() != null)
                    && (v.getTag() instanceof BluetoothGattCharacteristic)) {
                BluetoothGattCharacteristic ch = (BluetoothGattCharacteristic) v
                        .getTag();
                if (mConnGatt.readCharacteristic(ch)) {
                    setProgressBarIndeterminateVisibility(true);
                }
            }
        } else if (v.getId() == R.id.read_serial_number_button) {
            if ((v.getTag() != null)
                    && (v.getTag() instanceof BluetoothGattCharacteristic)) {
                BluetoothGattCharacteristic ch = (BluetoothGattCharacteristic) v
                        .getTag();
                if (mConnGatt.readCharacteristic(ch)) {
                    setProgressBarIndeterminateVisibility(true);
                }
            }

        } else if (v.getId() == R.id.write_alert_level_button) {
            if ((v.getTag() != null)
                    && (v.getTag() instanceof BluetoothGattCharacteristic)) {
                BluetoothGattCharacteristic ch = (BluetoothGattCharacteristic) v
                        .getTag();
                EditText editText = (EditText) findViewById(R.id.edit_message);
                String message = editText.getText().toString();
                if(!"".equals(message)) {
                    Msg msg = new Msg(message, Msg.TYPE_SEND);
                    msgList.add(msg);
                    adapter.notifyDataSetChanged();
                    msgListView.setSelection(msgList.size());
                    ch.setValue(message);//new byte[] { (byte) 0x03 });
                 //   editText.setText("");
                }
                if (mConnGatt.writeCharacteristic(ch)) {
                    setProgressBarIndeterminateVisibility(true);
                    Toast.makeText(this, "send ok", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (v.getId() == R.id.PROPERTY_NOTIFY) {
            if ((v.getTag() != null)
                    && (v.getTag() instanceof BluetoothGattDescriptor)) {
                BluetoothGattDescriptor ch = (BluetoothGattDescriptor) v
                        .getTag();
                if(flag==true){
                setCharacteristicNotification(characteristic,true);flag=false;}
                else{
                    setCharacteristicNotification(characteristic,false);flag=true;}
                if (mConnGatt.readDescriptor(ch)) {
                    setProgressBarIndeterminateVisibility(true);
                }
            }
        }
        else if (v.getId() == R.id.clean) {
            if( msgList!= null) {
                msgList.clear();
            }
            adapter.notifyDataSetChanged();
        }
	}

	private void init() {
		// BLE check
		if (!BleUtil.isBLESupported(this)) {
			Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT)
					.show();
			finish();
			return;
		}

		// BT check
		BluetoothManager manager = BleUtil.getManager(this);
		if (manager != null) {
			mBTAdapter = manager.getAdapter();
		}
		if (mBTAdapter == null) {
			Toast.makeText(this, R.string.bt_unavailable, Toast.LENGTH_SHORT)
					.show();
			finish();
			return;
		}

		// check BluetoothDevice
		if (mDevice == null) {
			mDevice = getBTDeviceExtra();
			if (mDevice == null) {
				finish();
				return;
			}
		}

		// button disable
		mReadManufacturerNameButton.setEnabled(false);
		mReadSerialNumberButton.setEnabled(false);
		mWriteAlertLevelButton.setEnabled(false);
        mSetPROPERTY_NOTIFY.setEnabled(false);

		// connect to Gatt
		if ((mConnGatt == null)
				&& (mStatus == BluetoothProfile.STATE_DISCONNECTED)) {
			// try to connect
			mConnGatt = mDevice.connectGatt(this, false, mGattcallback);
			mStatus = BluetoothProfile.STATE_CONNECTING;
		} else {
			if (mConnGatt != null) {
				// re-connect and re-discover Services
				mConnGatt.connect();
				mConnGatt.discoverServices();
			} else {
				Log.e(TAG, "state error");
				finish();
				return;
			}
		}
		setProgressBarIndeterminateVisibility(true);
	}

	private BluetoothDevice getBTDeviceExtra() {
		Intent intent = getIntent();
		if (intent == null) {
			return null;
		}

		Bundle extras = intent.getExtras();
		if (extras == null) {
			return null;
		}

		return extras.getParcelable(EXTRA_BLUETOOTH_DEVICE);
	}

}
