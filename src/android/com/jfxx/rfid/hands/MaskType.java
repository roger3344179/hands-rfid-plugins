package com.jfxx.rfid.hands;

public enum MaskType {
	SelectionMask(0, "Selection Mask"),
	EPCMask(1, "EPC Mask");
	
	private int code;
	private String name;
	
	MaskType(int code, String name) {
		this.code = code;
		this.name = name;
	}
	
	public int getCode() {
		return this.code;
	}

	@Override
	public String toString() {
		return this.name;
	}
	
	public static MaskType valueOf(int code) {
		for (MaskType item : values()) {
			if (item.getCode() == code) {
				return item;
			}
		}
		return MaskType.SelectionMask;
	}
}
