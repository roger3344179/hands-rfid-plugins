package com.jfxx.rfid.hands;

import com.atid.lib.rfid.event.ATRfidEventListener;

public interface IRFIDActivity extends ATRfidEventListener {

	public void setRFIDOprate(RFIDOprate rfidOprate);
	
	public void keyActionChange(boolean open);
	
	public boolean isDisconnect();
	
	public void connect(String addr);
	
	public void disconnect();
	
	/**
	 * 开始读卡
	 * @param powerGain 功率
	 * @param type 读卡类型 epc或tid
	 * @param length 长度
	 * @param jr 兼容长短卡
	 * @param continuous 边读模式
	 * @param keyStart 按键开始
	 */
	public void readCard(int powerGain,String type,int length,boolean jr,boolean continuous,boolean keyStart,boolean keepKey);
	
	public void stopRead();
	
	public void initATRfidReader();
}
