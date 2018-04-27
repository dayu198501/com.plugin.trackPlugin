package com.plugin.trackPlugin;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.model.LatLng;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.kit.cordova.AMapLocation.LocationPlugin;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import io.ionic.starter.Constant.Constant;


/**
 * This class echoes a string called from JavaScript.
 */
public class Track extends CordovaPlugin {

  private static final int PERMISSON_REQUESTCODE = 0;
  /**
   * 需要进行检测的权限数组
   */
  protected String[] needPermissions = {
    Manifest.permission.ACCESS_COARSE_LOCATION,
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.WRITE_EXTERNAL_STORAGE,
    Manifest.permission.READ_EXTERNAL_STORAGE,
    Manifest.permission.READ_PHONE_STATE
  };
  private CallbackContext trackCallbackContext = null;
  private CallbackContext locationCallbackContext = null;
  private CallbackContext geocallbackContext = null;
  //private CallbackContext callbackContext = null;
  private Track.SaveLocation saveLocation;
  private Track.NativeLocation nativeLocation;

  /**
   * 判断是否需要检测，防止不停的弹框
   */
  private boolean isNeedCheck = true;

  private Activity activity;

  //uiHandler在主线程中创建，所以自动绑定主线程
  private Handler uiHandler = new Handler() {
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case 1:
          win(geocallbackContext, (JSONObject) msg.obj);
          break;
      }
      super.handleMessage(msg);
    }
  };

  @Override
  public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
    this.activity = this.cordova.getActivity();
    //this.callbackContext = callbackContext;
    if (action.equals("coolMethod")) {
      String message = args.getString(0);
      this.coolMethod(message, callbackContext);
      return true;
    } else if (action.equals("startTrack")) {
      this.trackCallbackContext = callbackContext;
      long tid = args.getLong(0);
      int uid = args.getInt(1);
      String dbname = args.getString(2);
      if (saveLocation == null) {
        saveLocation = new Track.SaveLocation();
      }
      saveLocation.stopTrack();
      saveLocation.startTrack(tid, uid, this.cordova.getActivity(), dbname);
      PluginResult r = new PluginResult(PluginResult.Status.NO_RESULT);
      r.setKeepCallback(true);
      callbackContext.sendPluginResult(r);
      return true;
    } else if (action.equals("stopTrack")) {
      this.trackCallbackContext = callbackContext;
      if (saveLocation != null) {
        saveLocation.stopTrack();
      }
      PluginResult r = new PluginResult(PluginResult.Status.NO_RESULT);
      r.setKeepCallback(true);
      callbackContext.sendPluginResult(r);
    } else if (action.equals("startLocation")) {
      this.locationCallbackContext = callbackContext;
      if (nativeLocation == null) {
        nativeLocation = new Track.NativeLocation();
      }
      nativeLocation.stopLocation();
      nativeLocation.startLocation();
      PluginResult r = new PluginResult(PluginResult.Status.NO_RESULT);
      r.setKeepCallback(true);
      callbackContext.sendPluginResult(r);
      return true;
    } else if (action.equals("stopLocation")) {
      this.locationCallbackContext = callbackContext;
      if (nativeLocation != null) {
        nativeLocation.stopLocation();
      }
      PluginResult r = new PluginResult(PluginResult.Status.NO_RESULT);
      r.setKeepCallback(true);
      callbackContext.sendPluginResult(r);
      return true;
    } else if (action.equals("geocodeSeach")) {
      this.geocallbackContext = callbackContext;
      final double lng = args.getDouble(0);
      final double lat = args.getDouble(1);

      final GeocodeSearch geocoderSearch = new GeocodeSearch(this.cordova.getActivity());
      geocoderSearch.setOnGeocodeSearchListener(new GeocodeSearch.OnGeocodeSearchListener() {
        @Override
        public void onRegeocodeSearched(RegeocodeResult result, int rCode) {
          JSONObject jo = new JSONObject();
          try {
            jo.put("latitude", lat);
            jo.put("longitude", lng);
            jo.put("code", rCode);

            if (rCode == 1000) {
              if (result != null && result.getRegeocodeAddress() != null
                && result.getRegeocodeAddress().getFormatAddress() != null) {
                jo.put("formatAddress", result.getRegeocodeAddress().getFormatAddress());
                jo.put("city", result.getRegeocodeAddress().getCity());
                jo.put("building", result.getRegeocodeAddress().getBuilding());
                jo.put("province", result.getRegeocodeAddress().getProvince());
                jo.put("district", result.getRegeocodeAddress().getDistrict());
                jo.put("town", result.getRegeocodeAddress().getTownship());
              }
            }
            Thread.sleep(10000);
            win(locationCallbackContext, jo);
          } catch (JSONException e) {
            jo = null;
            e.printStackTrace();
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }

        @Override
        public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {

        }
      });

      LatLonPoint latLonPoint = new LatLonPoint(lat, lng);
      RegeocodeQuery query = new RegeocodeQuery(latLonPoint, 200, GeocodeSearch.GPS);
      geocoderSearch.getFromLocationAsyn(query);

      PluginResult r = new PluginResult(PluginResult.Status.OK);
      r.setKeepCallback(true);
      callbackContext.sendPluginResult(r);
    } else if (action.equals("saveUser")) {
      Constant.URL = args.getString(0);
      Constant.USER_ID = args.getInt(1);
      PluginResult r = new PluginResult(PluginResult.Status.NO_RESULT);
      r.setKeepCallback(true);
      callbackContext.sendPluginResult(r);
    }
    return false;
  }

  private void coolMethod(String message, CallbackContext callbackContext) {
    if (message != null && message.length() > 0) {
      callbackContext.success(message);
    } else {
      callbackContext.error("Expected one non-empty string argument.");
    }
  }

  /**
   * 获取权限集中需要申请权限的列表
   *
   * @param permissions
   * @return
   * @since 2.5.0
   */
  private List<String> findDeniedPermissions(String[] permissions) {
    List<String> needRequestPermissonList = new ArrayList<String>();
    if (Build.VERSION.SDK_INT >= 23
      && this.cordova.getActivity().getApplicationInfo().targetSdkVersion >= 23) {
      try {
        for (String perm : permissions) {
          Method checkSelfMethod = getClass().getMethod("checkSelfPermission", String.class);
          Method shouldShowRequestPermissionRationaleMethod = getClass().getMethod("shouldShowRequestPermissionRationale",
            String.class);
          if ((Integer) checkSelfMethod.invoke(this, perm) != PackageManager.PERMISSION_GRANTED
            || (Boolean) shouldShowRequestPermissionRationaleMethod.invoke(this, perm)) {
            needRequestPermissonList.add(perm);
          }
        }
      } catch (Throwable e) {

      }
    }
    return needRequestPermissonList;
  }

  /**
   * @param permissions
   * @since 2.5.0
   */
  private void checkPermissions(String... permissions) {
    try {
      if (Build.VERSION.SDK_INT >= 23
        && this.cordova.getActivity().getApplicationInfo().targetSdkVersion >= 23) {
        List<String> needRequestPermissonList = findDeniedPermissions(permissions);
        if (null != needRequestPermissonList
          && needRequestPermissonList.size() > 0) {
          String[] array = needRequestPermissonList.toArray(new String[needRequestPermissonList.size()]);
          Method method = getClass().getMethod("requestPermissions", new Class[]{String[].class,
            int.class});

          method.invoke(this, array, PERMISSON_REQUESTCODE);
        }
      }
    } catch (Throwable e) {
    }
  }

  /**
   * 检测是否所有的权限都已经授权
   *
   * @param grantResults
   * @return
   * @since 2.5.0
   */
  private boolean verifyPermissions(int[] grantResults) {
    for (int result : grantResults) {
      if (result != PackageManager.PERMISSION_GRANTED) {
        return false;
      }
    }
    return true;
  }

  @TargetApi(23)
  public void onRequestPermissionsResult(int requestCode,
                                         String[] permissions, int[] paramArrayOfInt) {
    if (requestCode == PERMISSON_REQUESTCODE) {
      if (!verifyPermissions(paramArrayOfInt)) {
        showMissingPermissionDialog();
        isNeedCheck = false;
      }
    }
  }

  /**
   * 显示提示信息
   *
   * @since 2.5.0
   */
  private void showMissingPermissionDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this.cordova.getActivity());
    builder.setTitle("权限");
    builder.setMessage("缺少权限");

    // 拒绝, 退出应用
    builder.setNegativeButton("取消",
      new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          //context.finish();
        }
      });

    builder.setPositiveButton("设置",
      new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          startAppSettings();
        }
      });

    builder.setCancelable(false);

    builder.show();
  }

  /**
   * 启动应用的设置
   *
   * @since 2.5.0
   */
  private void startAppSettings() {
    Intent intent = new Intent(
      Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
    intent.setData(Uri.parse("package:" + this.cordova.getActivity().getPackageName()));
    this.cordova.getActivity().startActivity(intent);
  }

  private void win(CallbackContext callbackContext, JSONObject message) {
    // Success return object
    PluginResult result;
    if (message != null)
      result = new PluginResult(PluginResult.Status.OK, message);
    else
      result = new PluginResult(PluginResult.Status.OK);

    result.setKeepCallback(true);
    callbackContext.sendPluginResult(result);
  }

  private void win(CallbackContext callbackContext, boolean success) {
    // Success return object
    PluginResult result;
    result = new PluginResult(PluginResult.Status.OK, success);

    result.setKeepCallback(true);
    callbackContext.sendPluginResult(result);
  }

  public class SaveLocation implements AMapLocationListener {
    private AMapLocationClient locationClient = null;
    private AMapLocationClientOption locationOption = null;

    private AMapLocation perLocation;
    private double distance;
    private long tid;
    private int uid;
    SQLiteDatabase db;
    private Activity activity;

    @Override
    public void onLocationChanged(AMapLocation amapLocation) {

      if (amapLocation != null && amapLocation.getErrorCode() == 0) {

        if (perLocation != null) {
          double d = AMapUtils.calculateLineDistance(new LatLng(perLocation.getLatitude(), perLocation.getLongitude()), new LatLng(amapLocation.getLatitude(), amapLocation.getLongitude()));
          if (d > 100 || d < 1) {
            return;
          } else {
            distance += d;
          }
        }

        double[] latlng = GPSUtil.gcj02_To_Gps84(amapLocation.getLatitude(), amapLocation.getLongitude());
        perLocation = amapLocation;

        ContentValues cv = new ContentValues();
        cv.put("tid", this.tid);
        cv.put("gps_time", amapLocation.getTime());
        cv.put("height", amapLocation.getAltitude());
        cv.put("speed", amapLocation.getSpeed());
        cv.put("direction", amapLocation.getBearing());
        cv.put("uploader", this.uid);
        cv.put("x", latlng[1]);
        cv.put("y", latlng[0]);
        this.db.insert("sport_track_point", null, cv);
      }
    }

    public void startTrack(long tid, int uid, Activity activity, String dbname) {

      if (isNeedCheck) {
        checkPermissions(needPermissions);
      }

      perLocation = null;
      distance = 0;
      this.tid = tid;
      this.uid = uid;

      File dbfile = activity.getDatabasePath(dbname);

      if (!dbfile.exists()) {
        trackCallbackContext.error("db is not exits.");
        return;
      }
      db = SQLiteDatabase.openOrCreateDatabase(dbfile, null);

      locationClient = new AMapLocationClient(activity);
      locationOption = new AMapLocationClientOption();
      /*
      低功耗   Battery_Saving
			高精度   Hight_Accuracy
			GPS    Device_Sensors
			*/
      // 设置定位模式为高精度模式
      locationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
      // 设置定位监听
      locationClient.setLocationListener(this);
      //设置定位间隔,单位毫秒,默认为2000ms
      locationOption.setInterval(5000);
      // 设置是否使用设备传感器
      locationOption.setSensorEnable(true);
      // 设置定位参数
      locationClient.setLocationOption(locationOption);
      // 启动定位
      locationClient.startLocation();

    }

    public void stopTrack() {
      if (locationClient != null) {
        locationClient.stopLocation();
      }
      if (db != null) {
        db.close();
      }
    }

  }

  public class NativeLocation implements AMapLocationListener {

    private AMapLocationClient locationClient = null;
    private AMapLocationClientOption locationOption = null;

    @Override
    public void onLocationChanged(AMapLocation amapLocation) {

      if (amapLocation != null && amapLocation.getErrorCode() == 0) {
        // 获取位置信息
        double latitude = amapLocation.getLatitude();
        double longitude = amapLocation.getLongitude();
        double height = amapLocation.getAltitude();
        boolean hasAccuracy = amapLocation.hasAccuracy();
        float accuracy = amapLocation.getAccuracy();
        String address = amapLocation.getAddress();
        String province = amapLocation.getProvince();
        String road = amapLocation.getRoad();
        // 速度
        float speed = amapLocation.getSpeed();
        // 角度
        float bearing = amapLocation.getBearing();
        // 星数
        int satellites = amapLocation.getExtras().getInt("satellites", 0);
        // 时间
        long time = amapLocation.getTime();

        double[] latlng = GPSUtil.gcj02_To_Gps84(latitude, longitude);
        JSONObject jo = new JSONObject();
        try {
          jo.put("latitude", latlng[0]);
          jo.put("longitude", latlng[1]);
          jo.put("height", height);
          jo.put("hasAccuracy", hasAccuracy);
          jo.put("accuracy", accuracy);
          jo.put("address", address);
          jo.put("province", province);
          jo.put("road", road);
          jo.put("speed", speed);
          jo.put("bearing", bearing);
          jo.put("satellites", satellites);
          jo.put("time", time);
          win(locationCallbackContext, jo);
        } catch (JSONException e) {
          jo = null;
          e.printStackTrace();
        }
      }
    }

    public void startLocation() {

      if (isNeedCheck) {
        checkPermissions(needPermissions);
      }

      locationClient = new AMapLocationClient(activity);
      locationOption = new AMapLocationClientOption();
      // 设置定位模式为高精度模式
      locationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
      // 设置定位监听
      locationClient.setLocationListener(this);
      //设置定位间隔,单位毫秒,默认为2000ms
      locationOption.setInterval(2000);
      // 设置是否使用设备传感器
      locationOption.setSensorEnable(true);
      // 设置定位参数
      locationClient.setLocationOption(locationOption);
      // 启动定位
      locationClient.startLocation();

    }

    public void stopLocation() {
      if (locationClient != null) {
        locationClient.stopLocation();
      }
    }
  }


}
