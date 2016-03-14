package org.robolectric.shadows;

import android.net.wifi.WifiInfo;

import org.robolectric.annotation.HiddenApi;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

/**
 * Shadow for {@link android.net.wifi.WifiInfo}.
 */
@Implements(WifiInfo.class)
public class ShadowWifiInfo {
  public static void __staticInitializer__() {
  }

  private String macAddress = "02:00:00:00:00:00"; // WifiInfo.DEFAULT_MAC_ADDRESS (@hide)
  private String ssid = "<unknown ssid>"; // WifiSsid.NONE (@hide)
  private String bssid;
  private int rssi = -127; // WifiInfo.INVALID_RSSI (@hide)

  @Implementation
  public String getMacAddress() {
    return macAddress;
  }

  @Implementation
  public String getSSID() {
    return ssid;
  }

  @Implementation
  public String getBSSID() {
    return bssid;
  }

  @Implementation
  public int getRssi() {
    return rssi;
  }

  @HiddenApi @Implementation
  public void setMacAddress(String newMacAddress) {
    macAddress = newMacAddress;
  }

  @HiddenApi @Implementation
  public void setSSID(String ssid) {
    this.ssid = ssid;
  }

  @HiddenApi @Implementation
  public void setBSSID(String bssid) {
    this.bssid = bssid;
  }

  @HiddenApi @Implementation
  public void setRssi(int rssi) {
    this.rssi = rssi;
  }
}
