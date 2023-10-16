package com.espressif.esptouch.android.v2;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.icu.util.Calendar;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.DialogFragment;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.espressif.esptouch.android.EspTouchActivityAbs;
import com.espressif.esptouch.android.R;
import com.espressif.esptouch.android.databinding.ActivityEsptouch2Binding;
import com.espressif.iot.esptouch2.provision.IEspProvisioner;

import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Date;


public class EspTouch2Activity extends EspTouchActivityAbs {
    private ActivityEsptouch2Binding mBinding;
    private static TextView time_choose;
    private static String ipAddress;
    private static String device_ipAddress;
    private static int alarm_status,alarm_hour,alarm_minute,sleep_status,sleep_hour,sleep_minute;
    private static int set_hour,set_minute;

    public static class TimePickerFragment extends DialogFragment
            implements TimePickerDialog.OnTimeSetListener {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current time as the default values for the picker
            final Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);
            // Create a new instance of TimePickerDialog and return it
            return new TimePickerDialog(getActivity(), this, hour, minute,
                    DateFormat.is24HourFormat(getActivity()));
        }

        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            set_hour = hourOfDay;
            set_minute = minute;
            time_choose.setText(set_hour+":"+set_minute);
            Toast.makeText(getActivity().getBaseContext(),set_hour+":"+set_minute, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = ActivityEsptouch2Binding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        time_choose = findViewById(R.id.SettingTime);

        //点击设置时间回调函数
        findViewById(R.id.SettingTime).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Toast.makeText(EspTouch2Activity.this,"方法2：btn4", Toast.LENGTH_SHORT).show();
                DialogFragment newFragment = new TimePickerFragment();
                newFragment.show(getSupportFragmentManager(), "timePicker");
            }
        });

        //点击detect回调函数
        findViewById(R.id.DetectDevice).setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                try {
                    getLocalIPAddress(EspTouch2Activity.this);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            DetectDevice();

                        }
                    }).start();//启动线程
                    if(device_ipAddress != null){
                        Toast.makeText(EspTouch2Activity.this,"get device", Toast.LENGTH_SHORT).show();
                        mBinding.ipText.setText(device_ipAddress);
                        mBinding.AlarmSwitch.setChecked(alarm_status != 0);
                        mBinding.SleepSwitch.setChecked(sleep_status != 0);
                        mBinding.StateAlarmTime.setText(alarm_hour +":"+ alarm_minute);
                        mBinding.StateSleepTime.setText(sleep_hour +":"+sleep_minute);
                    }else{
                        Toast.makeText(EspTouch2Activity.this,"no device found", Toast.LENGTH_SHORT).show();
                    }

                } catch (NoSuchMethodException | InvocationTargetException |
                         IllegalAccessException | NoSuchFieldException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        findViewById(R.id.SettingTimemBtn).setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        long timecurrentTimeMillis = System.currentTimeMillis();
                        JSONObject jsonThree = new JSONObject();
                        jsonThree.put("time_sec", timecurrentTimeMillis);
                        //System.out.println(jsonThree.toString());
                        Log.d("test", "  当前时间戳1->:"+ jsonThree);
                        HttpURLConnection connection=null;
                        try {
                            URL url = new URL("http://"+device_ipAddress+"/time");
                            connection = (HttpURLConnection) url.openConnection();
                            connection.setConnectTimeout(3000);
                            connection.setReadTimeout(3000);
                            //设置请求方式 GET / POST 一定要大小
                            connection.setRequestMethod("POST");
                            connection.setDoOutput(true);
                            connection.connect();
                            DataOutputStream dos=new DataOutputStream(connection.getOutputStream());
                            dos.writeBytes(jsonThree.toString());
                            int responseCode = connection.getResponseCode();
                            if (responseCode != HttpURLConnection.HTTP_OK) {
                                throw new IOException("HTTP error code" + responseCode);
                            }
                        } catch (ProtocolException e) {
                            throw new RuntimeException(e);
                        } catch (MalformedURLException e) {
                            throw new RuntimeException(e);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                    }
                }).start();//启动线程

            }
        });

        findViewById(R.id.AlarmSwitch).setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        alarm_status = (alarm_status == 0)?1:0;
                        JSONObject jsonThree = new JSONObject();
                        jsonThree.put("alarm_status", alarm_status);
                        jsonThree.put("sleep_status", sleep_status);
                        HttpURLConnection connection=null;
                        try {
                            URL url = new URL("http://"+device_ipAddress+"/ctrl");
                            connection = (HttpURLConnection) url.openConnection();
                            connection.setConnectTimeout(3000);
                            connection.setReadTimeout(3000);
                            //设置请求方式 GET / POST 一定要大小
                            connection.setRequestMethod("POST");
                            connection.setDoOutput(true);
                            connection.connect();
                            DataOutputStream dos=new DataOutputStream(connection.getOutputStream());
                            dos.writeBytes(jsonThree.toString());
                            int responseCode = connection.getResponseCode();
                            if (responseCode != HttpURLConnection.HTTP_OK) {
                                throw new IOException("HTTP error code" + responseCode);
                            }
                        } catch (ProtocolException e) {
                            throw new RuntimeException(e);
                        } catch (MalformedURLException e) {
                            throw new RuntimeException(e);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                    }
                }).start();//启动线程

            }
        });

        findViewById(R.id.SleepSwitch).setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        sleep_status = (sleep_status == 0)?1:0;
                        JSONObject jsonThree = new JSONObject();
                        jsonThree.put("sleep_status", sleep_status);
                        jsonThree.put("alarm_status", alarm_status);
                        HttpURLConnection connection=null;
                        try {
                            URL url = new URL("http://"+device_ipAddress+"/ctrl");
                            connection = (HttpURLConnection) url.openConnection();
                            connection.setConnectTimeout(3000);
                            connection.setReadTimeout(3000);
                            //设置请求方式 GET / POST 一定要大小
                            connection.setRequestMethod("POST");
                            connection.setDoOutput(true);
                            connection.connect();
                            DataOutputStream dos=new DataOutputStream(connection.getOutputStream());
                            dos.writeBytes(jsonThree.toString());
                            int responseCode = connection.getResponseCode();
                            if (responseCode != HttpURLConnection.HTTP_OK) {
                                throw new IOException("HTTP error code" + responseCode);
                            }
                        } catch (ProtocolException e) {
                            throw new RuntimeException(e);
                        } catch (MalformedURLException e) {
                            throw new RuntimeException(e);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                    }
                }).start();//启动线程

            }
        });

        findViewById(R.id.SettingButton).setOnClickListener(new View.OnClickListener() {
            @SuppressLint({"SetTextI18n", "ResourceType"})
            @Override
            public void onClick(View v) {
                int bt_id = mBinding.SettingRadioGroup.getCheckedRadioButtonId();
                switch(bt_id){
                    case R.id.AlarmRadioButton:
                        alarm_hour = set_hour;
                        alarm_minute = set_minute;
                        mBinding.StateAlarmTime.setText(alarm_hour+":"+alarm_minute);
                        break;
                    case R.id.SleepRadioButton:
                        sleep_hour = set_hour;
                        sleep_minute = set_minute;
                        mBinding.StateSleepTime.setText(sleep_hour+":"+sleep_minute);

                        break;
                }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject jsonThree = new JSONObject();
                        URL url = null;
                        HttpURLConnection connection=null;
                        try {
                            int bt_id = mBinding.SettingRadioGroup.getCheckedRadioButtonId();
                            switch(bt_id){
                                case R.id.AlarmRadioButton:
                                    jsonThree.put("alarm_hour", set_hour);
                                    jsonThree.put("alarm_min", set_minute);
                                    url = new URL("http://"+device_ipAddress+"/alarm");
                                    break;
                                case R.id.SleepRadioButton:
                                    jsonThree.put("sleep_hour", set_hour);
                                    jsonThree.put("sleep_min", set_minute);
                                    url = new URL("http://"+device_ipAddress+"/sleep");
                                    break;
                            }
                            connection = (HttpURLConnection) url.openConnection();
                            connection.setConnectTimeout(3000);
                            connection.setReadTimeout(3000);
                            //设置请求方式 GET / POST 一定要大小
                            connection.setRequestMethod("POST");
                            connection.setDoOutput(true);
                            connection.connect();
                            DataOutputStream dos=new DataOutputStream(connection.getOutputStream());
                            dos.writeBytes(jsonThree.toString());
                            int responseCode = connection.getResponseCode();
                            if (responseCode != HttpURLConnection.HTTP_OK) {
                                throw new IOException("HTTP error code" + responseCode);
                            }
                        } catch (ProtocolException e) {
                            throw new RuntimeException(e);
                        } catch (MalformedURLException e) {
                            throw new RuntimeException(e);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                    }
                }).start();//启动线程

            }
        });

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected String getEspTouchVersion() {
        return getString(R.string.esptouch2_about_version, IEspProvisioner.ESPTOUCH_VERSION);
    }

    public static void DetectDevice() {
        String message = "123";
        int server_port = 3333;
        DatagramSocket s =null;
        try {
            s = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        InetAddress local = null;
        try {
            // 换成服务器端IP
            local = InetAddress.getByName(ipAddress);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        byte[] messageByte = message.getBytes();
        DatagramPacket p = new DatagramPacket(messageByte, messageByte.length, local, server_port);
        try {
            s.send(p);
            byte[] buf = new byte[1024];
            DatagramPacket serverMsgPacket = new DatagramPacket(buf, buf.length);
            s.receive(serverMsgPacket);
            device_ipAddress = String.valueOf(serverMsgPacket.getAddress()).substring(1);

            char[] tChars = new char[1024];
            for(int i = 0; i<serverMsgPacket.getLength(); i++)
                tChars[i]=(char)buf[i];
            JSONObject jsonObject = JSON.parseObject(String.valueOf(tChars), Feature.OrderedField);
            alarm_status  = jsonObject.getIntValue("alarm_status");
            alarm_hour = jsonObject.getIntValue("alarm_hour");
            alarm_minute = jsonObject.getIntValue("alarm_minute");

            sleep_status = jsonObject.getIntValue("sleep_status");
            sleep_hour = jsonObject.getIntValue("sleep_hour");
            sleep_minute = jsonObject.getIntValue("sleep_minute");

            s.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // wifi下获取本地网络IP地址（局域网地址）
    public static CharSequence getLocalIPAddress(Context context) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        Field field = wifiManager.getClass().getDeclaredField("WIFI_AP_STATE_ENABLED");
        int value = (int) field.get(wifiManager);
        Method method = wifiManager.getClass().getDeclaredMethod("getWifiApState");
        int state = (int) method.invoke(wifiManager);
        if ((wifiManager != null) && (state == value)) {
            int i = (255<<24)|(43<<16)|(168<<8)|192;
            ipAddress = (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF) + "." + (i >> 24 & 0xFF);
        } else if(wifiManager !=null) {
            @SuppressLint("MissingPermission") WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int i = wifiInfo.getIpAddress();
            ipAddress = (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF) + "." + (255 & 0xFF);
        }
        return null;
    }

}
