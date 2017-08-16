/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
*/
package org.apache.cordova;

import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import com.atid.lib.ATRfidManager;
import com.atid.lib.ATRfidReader;
import com.atid.lib.barcode.type.BarcodeType;
import com.atid.lib.device.type.ConnectionState;
import com.atid.lib.diagnostics.ATLog;
import com.atid.lib.rfid.exception.ATRfidReaderException;
import com.atid.lib.rfid.type.ActionState;
import com.atid.lib.rfid.type.CommandType;
import com.atid.lib.rfid.type.MemoryBank;
import com.atid.lib.rfid.type.OperationMode;
import com.atid.lib.rfid.type.RemoteKeyState;
import com.atid.lib.rfid.type.ResultCode;
import com.jfxx.rfid.hands.IRFIDActivity;
import com.jfxx.rfid.hands.MaskType;
import com.jfxx.rfid.hands.RFIDOprate;

import android.R;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

/**
 * This class is the main Android activity that represents the Cordova
 * application. It should be extended by the user to load the specific
 * html file that contains the application.
 *
 * As an example:
 *
 * <pre>
 *     package org.apache.cordova.examples;
 *
 *     import android.os.Bundle;
 *     import org.apache.cordova.*;
 *
 *     public class Example extends CordovaActivity {
 *       &#64;Override
 *       public void onCreate(Bundle savedInstanceState) {
 *         super.onCreate(savedInstanceState);
 *         super.init();
 *         // Load your application
 *         loadUrl(launchUrl);
 *       }
 *     }
 * </pre>
 *
 * Cordova xml configuration: Cordova uses a configuration file at
 * res/xml/config.xml to specify its settings. See "The config.xml File"
 * guide in cordova-docs at http://cordova.apache.org/docs for the documentation
 * for the configuration. The use of the set*Property() methods is
 * deprecated in favor of the config.xml file.
 *
 */
public class CordovaActivity extends Activity implements IRFIDActivity{
    public static String TAG = "CordovaActivity";

    // The webview for our app
    protected CordovaWebView appView;

    private static int ACTIVITY_STARTING = 0;
    private static int ACTIVITY_RUNNING = 1;
    private static int ACTIVITY_EXITING = 2;

    // Keep app running when pause is received. (default = true)
    // If true, then the JavaScript and native code continue to run in the background
    // when another application (activity) is started.
    protected boolean keepRunning = true;

    // Flag to keep immersive mode if set to fullscreen
    protected boolean immersiveMode;

    // Read from config.xml:
    protected CordovaPreferences preferences;
    protected String launchUrl;
    protected ArrayList<PluginEntry> pluginEntries;
    protected CordovaInterfaceImpl cordovaInterface;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // need to activate preferences before super.onCreate to avoid "requestFeature() must be called before adding content" exception
        loadConfig();

        String logLevel = preferences.getString("loglevel", "ERROR");
        LOG.setLogLevel(logLevel);

        LOG.i(TAG, "Apache Cordova native platform version " + CordovaWebView.CORDOVA_VERSION + " is starting");
        LOG.d(TAG, "CordovaActivity.onCreate()");

        if (!preferences.getBoolean("ShowTitle", false)) {
            getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }

        if (preferences.getBoolean("SetFullscreen", false)) {
            LOG.d(TAG, "The SetFullscreen configuration is deprecated in favor of Fullscreen, and will be removed in a future version.");
            preferences.set("Fullscreen", true);
        }
        if (preferences.getBoolean("Fullscreen", false)) {
            // NOTE: use the FullscreenNotImmersive configuration key to set the activity in a REAL full screen
            // (as was the case in previous cordova versions)
            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) && !preferences.getBoolean("FullscreenNotImmersive", false)) {
                immersiveMode = true;
            } else {
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
        } else {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        }

        super.onCreate(savedInstanceState);

        cordovaInterface = makeCordovaInterface();
        if (savedInstanceState != null) {
            cordovaInterface.restoreInstanceState(savedInstanceState);
        }
        
        loadConfig(this);
        initATRfidReader();
    }

    protected void init() {
        appView = makeWebView();
        createViews();
        if (!appView.isInitialized()) {
            appView.init(cordovaInterface, pluginEntries, preferences);
        }
        cordovaInterface.onCordovaInit(appView.getPluginManager());

        // Wire the hardware volume controls to control media if desired.
        String volumePref = preferences.getString("DefaultVolumeStream", "");
        if ("media".equals(volumePref.toLowerCase(Locale.ENGLISH))) {
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
        }
    }

    @SuppressWarnings("deprecation")
    protected void loadConfig() {
        ConfigXmlParser parser = new ConfigXmlParser();
        parser.parse(this);
        preferences = parser.getPreferences();
        preferences.setPreferencesBundle(getIntent().getExtras());
        launchUrl = parser.getLaunchUrl();
        pluginEntries = parser.getPluginEntries();
        Config.parser = parser;
    }

    //Suppressing warnings in AndroidStudio
    @SuppressWarnings({"deprecation", "ResourceType"})
    protected void createViews() {
        //Why are we setting a constant as the ID? This should be investigated
        appView.getView().setId(100);
        appView.getView().setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        setContentView(appView.getView());

        if (preferences.contains("BackgroundColor")) {
            try {
                int backgroundColor = preferences.getInteger("BackgroundColor", Color.BLACK);
                // Background of activity:
                appView.getView().setBackgroundColor(backgroundColor);
            }
            catch (NumberFormatException e){
                e.printStackTrace();
            }
        }

        appView.getView().requestFocusFromTouch();
    }

    /**
     * Construct the default web view object.
     * <p/>
     * Override this to customize the webview that is used.
     */
    protected CordovaWebView makeWebView() {
        return new CordovaWebViewImpl(makeWebViewEngine());
    }

    protected CordovaWebViewEngine makeWebViewEngine() {
        return CordovaWebViewImpl.createEngine(this, preferences);
    }

    protected CordovaInterfaceImpl makeCordovaInterface() {
        return new CordovaInterfaceImpl(this) {
            @Override
            public Object onMessage(String id, Object data) {
                // Plumb this to CordovaActivity.onMessage for backwards compatibility
                return CordovaActivity.this.onMessage(id, data);
            }
        };
    }

    /**
     * Load the url into the webview.
     */
    public void loadUrl(String url) {
        if (appView == null) {
            init();
        }

        // If keepRunning
        this.keepRunning = preferences.getBoolean("KeepRunning", true);

        appView.loadUrlIntoView(url, true);
    }

    /**
     * Called when the system is about to start resuming a previous activity.
     */
    @Override
    protected void onPause() {
        super.onPause();
        LOG.d(TAG, "Paused the activity.");

        if (this.appView != null) {
            // CB-9382 If there is an activity that started for result and main activity is waiting for callback
            // result, we shoudn't stop WebView Javascript timers, as activity for result might be using them
            boolean keepRunning = this.keepRunning || this.cordovaInterface.activityResultCallback != null;
            this.appView.handlePause(keepRunning);
        }
    }

    /**
     * Called when the activity receives a new intent
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        //Forward to plugins
        if (this.appView != null)
            this.appView.onNewIntent(intent);
    }

    /**
     * Called when the activity will start interacting with the user.
     */
    @Override
    protected void onResume() {
        super.onResume();
        LOG.d(TAG, "Resumed the activity.");

        if (this.appView == null) {
            return;
        }
        // Force window to have focus, so application always
        // receive user input. Workaround for some devices (Samsung Galaxy Note 3 at least)
        this.getWindow().getDecorView().requestFocus();

        this.appView.handleResume(this.keepRunning);
    }

    /**
     * Called when the activity is no longer visible to the user.
     */
    @Override
    protected void onStop() {
        super.onStop();
        LOG.d(TAG, "Stopped the activity.");

        if (this.appView == null) {
            return;
        }
        this.appView.handleStop();
    }

    /**
     * Called when the activity is becoming visible to the user.
     */
    @Override
    protected void onStart() {
        super.onStart();
        LOG.d(TAG, "Started the activity.");

        if (this.appView == null) {
            return;
        }
        this.appView.handleStart();
    }

    /**
     * The final call you receive before your activity is destroyed.
     */
    @Override
    public void onDestroy() {
        LOG.d(TAG, "CordovaActivity.onDestroy()");
        super.onDestroy();

        if (this.appView != null) {
            appView.handleDestroy();
        }
    }

    /**
     * Called when view focus is changed
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && immersiveMode) {
            final int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

            getWindow().getDecorView().setSystemUiVisibility(uiOptions);
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void startActivityForResult(Intent intent, int requestCode, Bundle options) {
        // Capture requestCode here so that it is captured in the setActivityResultCallback() case.
        cordovaInterface.setActivityResultRequestCode(requestCode);
        super.startActivityForResult(intent, requestCode, options);
    }

    /**
     * Called when an activity you launched exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     *
     * @param requestCode The request code originally supplied to startActivityForResult(),
     *                    allowing you to identify who this result came from.
     * @param resultCode  The integer result code returned by the child activity through its setResult().
     * @param intent      An Intent, which can return result data to the caller (various data can be attached to Intent "extras").
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        LOG.d(TAG, "Incoming Result. Request code = " + requestCode);
        super.onActivityResult(requestCode, resultCode, intent);
        cordovaInterface.onActivityResult(requestCode, resultCode, intent);
        
        ATLog.d(TAG, "DEBUG. onActivityResult(%s, %s)", requestCode, requestCode);

        switch (requestCode) {
        case ATRfidManager.REQUEST_DEVICE_LIST:
        case ATRfidManager.REQUEST_BLE_DEVICE_LIST:
        case ATRfidManager.REQUEST_ENABLE_BLUETOOTH:
        	//String address=ATRfidManager.getReaderAddress(data);
            ATRfidManager.onActivityResult(requestCode, resultCode, intent);
            if ((requestCode == ATRfidManager.REQUEST_DEVICE_LIST
                    || requestCode == ATRfidManager.REQUEST_BLE_DEVICE_LIST) && resultCode == Activity.RESULT_OK) {
            	
            	Toast.makeText(this, "连接中", Toast.LENGTH_SHORT).show();
                /*WaitDialog.show(this, "连接中", new DialogInterface.OnCancelListener() {

                    @Override
                    public void onCancel(DialogInterface dialog) {
                        // Disconnect Device
                        //stopCheckBattery(RFIDStateActivity.this);
                        mReader.disconnectDevice();
                    }

                });*/
            }
            break;
    }
    }

    /**
     * Report an error to the host application. These errors are unrecoverable (i.e. the main resource is unavailable).
     * The errorCode parameter corresponds to one of the ERROR_* constants.
     *
     * @param errorCode   The error code corresponding to an ERROR_* value.
     * @param description A String describing the error.
     * @param failingUrl  The url that failed to load.
     */
    public void onReceivedError(final int errorCode, final String description, final String failingUrl) {
        final CordovaActivity me = this;

        // If errorUrl specified, then load it
        final String errorUrl = preferences.getString("errorUrl", null);
        if ((errorUrl != null) && (!failingUrl.equals(errorUrl)) && (appView != null)) {
            // Load URL on UI thread
            me.runOnUiThread(new Runnable() {
                public void run() {
                    me.appView.showWebPage(errorUrl, false, true, null);
                }
            });
        }
        // If not, then display error dialog
        else {
            final boolean exit = !(errorCode == WebViewClient.ERROR_HOST_LOOKUP);
            me.runOnUiThread(new Runnable() {
                public void run() {
                    if (exit) {
                        me.appView.getView().setVisibility(View.GONE);
                        me.displayError("Application Error", description + " (" + failingUrl + ")", "OK", exit);
                    }
                }
            });
        }
    }

    /**
     * Display an error dialog and optionally exit application.
     */
    public void displayError(final String title, final String message, final String button, final boolean exit) {
        final CordovaActivity me = this;
        me.runOnUiThread(new Runnable() {
            public void run() {
                try {
                    AlertDialog.Builder dlg = new AlertDialog.Builder(me);
                    dlg.setMessage(message);
                    dlg.setTitle(title);
                    dlg.setCancelable(false);
                    dlg.setPositiveButton(button,
                            new AlertDialog.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    if (exit) {
                                        finish();
                                    }
                                }
                            });
                    dlg.create();
                    dlg.show();
                } catch (Exception e) {
                    finish();
                }
            }
        });
    }

    /*
     * Hook in Cordova for menu plugins
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (appView != null) {
            appView.getPluginManager().postMessage("onCreateOptionsMenu", menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (appView != null) {
            appView.getPluginManager().postMessage("onPrepareOptionsMenu", menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (appView != null) {
            appView.getPluginManager().postMessage("onOptionsItemSelected", item);
        }
        return true;
    }

    /**
     * Called when a message is sent to plugin.
     *
     * @param id   The message id
     * @param data The message data
     * @return Object or null
     */
    public Object onMessage(String id, Object data) {
        if ("onReceivedError".equals(id)) {
            JSONObject d = (JSONObject) data;
            try {
                this.onReceivedError(d.getInt("errorCode"), d.getString("description"), d.getString("url"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else if ("exit".equals(id)) {
            finish();
        }
        return null;
    }

    protected void onSaveInstanceState(Bundle outState) {
        cordovaInterface.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    /**
     * Called by the system when the device configuration changes while your activity is running.
     *
     * @param newConfig The new device configuration
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (this.appView == null) {
            return;
        }
        PluginManager pm = this.appView.getPluginManager();
        if (pm != null) {
            pm.onConfigurationChanged(newConfig);
        }
    }

    /**
     * Called by the system when the user grants permissions
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],
                                           int[] grantResults) {
        try
        {
            cordovaInterface.onRequestPermissionResult(requestCode, permissions, grantResults);
        }
        catch (JSONException e)
        {
            LOG.d(TAG, "JSONException: Parameters fed into the method are not valid");
            e.printStackTrace();
        }

    }
    
    
    
    
    
    
    
    
    public ATRfidReader mReader = null;

	private Thread bettyThread=null;
	
	public static final String KEY_DEVICE_ADDRESS = "device_address";
    public static final String KEY_BATTERY_INTERVAL = "battery_interval";
    public static final String KEY_MASK_TYPE = "mask_type";
    public static final int DEFAULT_BATTERY_INTERVAL = 6;
    public static  String mDeviceAddress;
    public static int mBatteryInterval=DEFAULT_BATTERY_INTERVAL;
    public static MaskType mMaskType;
    public static final String DEFAULT_DEVICE_ADDRESS = null;
    public static final int DEFAULT_MASK_TYPE = 0;
    //private static boolean mKeyAction;
    
    public static int POWER_GAIN = 300;
    private boolean read_short_card=false;
    
    private RFIDOprate rfidOprate;
    
    private int betty=-1;
    
    private String deviceName=null;
    
    private boolean compatibleLength=true;
    
    private boolean continuousMode=false;
    
    private boolean toconnect=false;
    
    private String deviceAddr=null;
    
    private boolean keepOpenKey=false;
    
    public static void loadConfig(final Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(activity.getLocalClassName(), Context.MODE_PRIVATE);
        mDeviceAddress = prefs.getString(KEY_DEVICE_ADDRESS, DEFAULT_DEVICE_ADDRESS);
        ATLog.d(TAG, "DEBUG. loadConfig() - Device Address : [%s]", mDeviceAddress);
        mBatteryInterval = prefs.getInt(KEY_BATTERY_INTERVAL, DEFAULT_BATTERY_INTERVAL);
        if (mBatteryInterval > 60){
            mBatteryInterval = DEFAULT_BATTERY_INTERVAL;
        }
        ATLog.d(TAG, "DEBUG. loadConfig() - Batery Check Interval : [%d]", mBatteryInterval);
        mMaskType = MaskType.valueOf(prefs.getInt(KEY_MASK_TYPE, DEFAULT_MASK_TYPE));
        ATLog.d(TAG, "DEBUG. loadConfig() - Mask Type : [%s]", mMaskType);
        //mKeyAction = prefs.getBoolean(RfidActivity.KEY_ACTION, RfidActivity.DEFAULT_KEY_ACTION);
        ATLog.d(TAG, "DEBUG. loadConfig(); - Key Action : %s", "");

        ATLog.i(TAG, "INFO. loadConfig()");
    }

    // Save Configuration
    public static void saveConfig(final Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(activity.getLocalClassName(), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString(KEY_DEVICE_ADDRESS, mDeviceAddress);
        ATLog.d(TAG, "DEBUG. saveConfig() - Device Address : [%s]", mDeviceAddress);
        editor.putInt(KEY_BATTERY_INTERVAL, mBatteryInterval);
        ATLog.d(TAG, "DEBUG. saveConfig() - Batery Check Interval : [%d]",mBatteryInterval);
        editor.putInt(KEY_MASK_TYPE, mMaskType.getCode());
        ATLog.d(TAG, "DEBUG. saveConfig() - Mask Type : [%s]", mMaskType);
        //editor.putBoolean(RfidActivity.KEY_ACTION, mKeyAction);
        ATLog.d(TAG, "DEBUG. saveConfig() - Key Action : %s", "");

        editor.commit();

        ATLog.i(TAG, "INFO. saveConfig()");
    }
    
	@Override
	public void onAccessResult(ATRfidReader reader, ResultCode code, ActionState action, String epc, String data, float rssi, float phase) {
		Log.i(TAG,"~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~onAccessResult  tid:"+data+"   "+new Date().getTime());
        if(data==null||data.equals("")){
        	mReader.readMemory(MemoryBank.TID,0,4);
            return;
        }
        if(!keepOpenKey){
        	keyActionChange(false);
        }
        rfidOprate.readCardCallback(data);
		//
	}

	@Override
	public void onActionChanged(ATRfidReader ATRfidReader, ActionState actionState) {
		Log.i(TAG,"~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~onActionChanged  :  "+actionState+"   "+new Date().getTime());

        if(actionState.equals(ActionState.Stop)&&read_short_card){
            onAccessResult(ATRfidReader,ResultCode.Busy,ActionState.ReadMemory,"","",0,0);
        }
	}

	@Override
	public void onCommandComplete(ATRfidReader arg0, CommandType arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDebugMessage(ATRfidReader arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDetactBarcode(ATRfidReader arg0, BarcodeType arg1, String arg2, String arg3) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onLoadTag(ATRfidReader arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onReadedTag(ATRfidReader arg0, ActionState arg1, String arg2, float arg3, float arg4) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRemoteKeyStateChanged(ATRfidReader arg0, RemoteKeyState arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStateChanged(ATRfidReader arg0, ConnectionState state) {
		ATLog.i(TAG, "EVENT. onConnectionStateChanged(%s)", state);
        switch (state) {
            case Disconnected:
                /*if (mReader.getResultCode() == ResultCode.NotSupportFirmware) {
                    AlertDialog.Builder alert = new AlertDialog.Builder(this);
                    alert.setTitle("提示");
                    alert.setIcon(android.R.drawable.ic_dialog_alert);
                    alert.setMessage("断开啦");
                    alert.setPositiveButton("ok", null);
                    alert.show();
                }*/
            	
            	//断开连接代码
            	deviceName=null;
            	Toast.makeText(this, "rfid设备已断开:", Toast.LENGTH_SHORT).show();
            	rfidOprate.updateState(!isDisconnect(), betty, deviceName, "rfid设备已断开:");
            	betty=-1;
            	
            	if(toconnect){
            		toconnect=false;
            		connect(deviceAddr);
            	}
            	
                break;
            case Connecting:
            	//连接中代码
            case Listen:
                //enableMenuButtons(false);
                //imgLogo.setImageResource(R.drawable.ic_connecting_logo);//断开图标设置
                break;
            case Connected:
                // Get Firmware Version
            	saveConfig(this);
            	/*connect.setText("断开");
            	text_connect_state.setText("已连接");*/
            	
            	betty=0;
            	
            	//已连接代码
            	rfidOprate.updateState(!isDisconnect(), betty, deviceName, "rfid设备已连接");
            	
                break;
        }
		
	}

	@Override
	public void setRFIDOprate(RFIDOprate rfidOprate) {
		this.rfidOprate=rfidOprate;
	}

	@Override
	public boolean isDisconnect() {
		return mReader==null||mReader.getState()==null||mReader.getState().equals(ConnectionState.Disconnected)||betty==-1;
	}

	@Override
	public void connect(String addr) {
		if(mReader==null){
			initATRfidReader();
			return;
		}
		
		if(mReader!=null&&mReader.getState().equals(ConnectionState.Disconnected)){
			if(addr!=null&&!addr.equals("")&&!addr.equals("null")){
				mReader.connectDevice(addr);
			}else if (!ATRfidManager.connectMostRecentDevice()) {
				ATRfidManager.openDeviceListActivity(this);
			}
		}else{
			mReader.disconnectDevice();
			toconnect=true;
			deviceAddr=addr;
		}
	}

	@Override
	public void disconnect() {
		if(mReader!=null){
			mReader.disconnectDevice();
		}
	}

	@Override
	public void initATRfidReader() {
		ATRfidManager.setContext(this);
        if (!ATRfidManager.isBluetoothSupported()) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle("系统错误");
            //dialog.setIcon(android.R.drawable.ic_dialog_alert);
            dialog.setMessage("设备不支持蓝牙,或未开启蓝牙");
            dialog.setPositiveButton("ok", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ATLog.i(TAG, "INFO. initATRfidReader()$onClick()");
                    //finish();
                    dialog.dismiss();
                }

            });
            dialog.show();
            return;
        }
        
        
        ATRfidManager.checkEnableBluetooth(this);

        mReader = ATRfidManager.getInstance();
        
        mReader.setEventListener(this);

        ATLog.i(TAG, "INFO. initATRfidReader()");
        
        bettyThread=new Thread(new Runnable() {
			@Override
			public void run() {
				while(true){
					try {
						//电量推送代码
						if(!isDisconnect()){
							if(mReader.getBatteryStatus()!=betty){
								betty=mReader.getBatteryStatus();
								rfidOprate.updateState(!isDisconnect(), betty , deviceName, "rfid设备电池电量"+betty);
							}
						}
						
					}catch (Exception e) {
						ATLog.e(TAG, e.getMessage()+"");
					}
					
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		});
    	bettyThread.start();
	}

	@Override
	public void readCard(int powerGain, String type, int length, boolean jr, boolean continuous, boolean keyStart,boolean keepKey) {
		try {
			if(mReader==null){
				return;
			}
			keepOpenKey=keepKey;
			compatibleLength=jr;
			mReader.setPowerGain(powerGain>0?powerGain:POWER_GAIN);
			mReader.setActionTIDReadLength(length>0?length:6);
			mReader.setContinuousMode(continuous);
			if(keyStart){
				mReader.setOperationMode(type.equals("TID")?OperationMode.TID:OperationMode.Normal);
				keyActionChange(true);
			}else{
				mReader.readMemory(type.equals("TID")?MemoryBank.TID:MemoryBank.EPC,0,length>0?length:6);
			}
		} catch (ATRfidReaderException e) {
			ATLog.e(TAG, e.getMessage()+"");
		}
		
	}

	
	@Override
	public void keyActionChange(boolean open) {
		try {
			if(mReader==null){
				return;
			}
			
			/**
			 * 设置默认值
			 */
            if(!open){
                mReader.setOperationMode(OperationMode.TID);
                mReader.setActionTIDReadLength(6);
                mReader.setContinuousMode(false);
                mReader.setPowerGain(POWER_GAIN);
            }
            
            keepOpenKey=false;
            mReader.setUseKeyAction(open);
            //POWER_GAIN
        } catch (ATRfidReaderException e) {
        	ATLog.e(TAG, e.getMessage()+"");
        }
	}

	@Override
	public void stopRead() {
		mReader.stop();
		keyActionChange(false);
	}
}
