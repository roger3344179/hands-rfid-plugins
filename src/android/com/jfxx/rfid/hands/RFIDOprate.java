package com.jfxx.rfid.hands;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.atid.lib.ATRfidManager;
import com.atid.lib.diagnostics.ATLog;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

public class RFIDOprate extends CordovaPlugin {
	
	private static final String TAG = RFIDOprate.class.getSimpleName();

	private CallbackContext callbackContext;
	
	private CallbackContext connectionCallbackContext;
	
	private CallbackContext readCardCallbackContext;
	
	
	//private boolean connected=false;
	
	private JSONObject rfidState=null;
	
	private static final String READ_CARD="readCard";
	private static final String STOP_READ="getConnection";
	private static final String CONNECT="connect";
	private static final String DISCONNECT="disconnect";
	
	private static final String GET_CONNECTION="getConnection";
	
	private IRFIDActivity rfidActivity;
	
	

	@Override
	public void initialize(CordovaInterface cordova, CordovaWebView webView) {
		super.initialize(cordova, webView);
		if(this.cordova.getActivity() instanceof IRFIDActivity){
			rfidActivity=(IRFIDActivity)this.cordova.getActivity();
			rfidActivity.setRFIDOprate(this);
		}
		
		try {
			rfidState=new JSONObject();
			rfidState.put("betty", 0);
			rfidState.put("connected", false);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
	}

	@Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        this.callbackContext = callbackContext;
        if(action.equals(READ_CARD)){
        	readCard(args,callbackContext);
        }else if(action.equals(CONNECT)){
        	connectionCallbackContext=callbackContext;
        	connect(args);
        }else if(action.equals(DISCONNECT)){
        	disconnect(args);
        }else if(action.equals(GET_CONNECTION)){
        	getConnection(callbackContext);
        }else if(action.equals(STOP_READ)){
        	stopRead();
        }else{
        	return false;
        }
        return true;
    }
	
	public void getConnection(CallbackContext callbackContext){
		connectionCallbackContext=callbackContext;
		try{
			if(rfidState!=null){
				PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, rfidState);
	            pluginResult.setKeepCallback(true);
	            connectionCallbackContext.sendPluginResult(pluginResult);
			}
		}catch (Exception e) {
			connectionCallbackContext.error("获取连接出现异常 "+e.getMessage());
		}
	}
	
	public void stopRead(){
		try{
			if(rfidActivity!=null){
				rfidActivity.stopRead();
			}else{
				this.callbackContext.error("activity不支持rfid");
			}
		}catch (Exception e) {
			this.callbackContext.error("停止读卡异常"+e.getMessage());
		}
	}
	
	public void connect(JSONArray args){
		try{
			//Toast.makeText(this.cordova.getActivity(), "收到请求："+args.toString(), Toast.LENGTH_SHORT).show();
			//callbackContext.success("ok");
			String addr=null;
			if(args!=null&&args.length()>0&&args.get(0)!=null){
				addr=args.getString(0);
			}
			if(rfidActivity!=null){
				rfidActivity.connect(addr);
			}else{
				this.callbackContext.error("activity不支持rfid");
			}
		}catch (Exception e) {
			this.callbackContext.error("连接出现异常"+e.getMessage());
		}
	}
	
	public void disconnect(JSONArray args){
		try{
			//Toast.makeText(this.cordova.getActivity(), "收到请求："+args.toString(), Toast.LENGTH_SHORT).show();
			//callbackContext.success("ok");
			readCardCallbackContext=null;
			if(rfidActivity!=null){
				rfidActivity.disconnect();
			}else{
				this.callbackContext.error("activity不支持rfid");
			}
		}catch (Exception e) {
			this.callbackContext.error("断开连接出现异常"+e.getMessage());
		}
	}
	
	/**
	 * 开始读卡
	 * @param args
	 */
	public void readCard(JSONArray args,CallbackContext callbackContext){
		try{
			//Toast.makeText(this.cordova.getActivity(), "收到请求："+args.toString(), Toast.LENGTH_SHORT).show();
			this.readCardCallbackContext=callbackContext;
			//callbackContext.success("ok");
			JSONObject obj=null;
			if(args!=null&&args.length()>0){
				obj=args.getJSONObject(0);
			}
			if(rfidActivity!=null){
				rfidActivity.readCard(obj.getInt("POWER_GAIN"), obj.getString("TYPE"), obj.getInt("LENGTH"), obj.getBoolean("COMPATIBLE"), obj.getBoolean("CONTINUOUS"), obj.getBoolean("KEYSTART"), obj.getBoolean("KEEPKEY"));
			}else{
				this.callbackContext.error("activity不支持rfid");
			}
		}catch (Exception e) {
			callbackContext.error("触发读卡异常"+e.getMessage());
		}
	}
	
	/**
	 * 读卡回调
	 * @param cardNum
	 */
	public void readCardCallback(String cardNum){
		if(readCardCallbackContext!=null){
			/*PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, cardNum);
            pluginResult.setKeepCallback(true);*/
            readCardCallbackContext.success(cardNum);
		}
	}
	
	/**
	 * 更新状态
	 * @param connected
	 * @param betty
	 */
	public void updateState(boolean connected,int betty,String deviceName,String msg){
		if(connectionCallbackContext!=null){
			if(rfidState==null){
				rfidState=new JSONObject();
			}
			try {
				rfidState.put("betty", betty);
				rfidState.put("deviceName", deviceName);
				rfidState.put("connected", connected);
				rfidState.put("msg", msg);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, rfidState);
            pluginResult.setKeepCallback(true);
			connectionCallbackContext.sendPluginResult(pluginResult);
		}
	}
	
	
}
