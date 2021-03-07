package com.example.bluetoothtemperaturecontrol;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.LoginFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bluetoothtemperaturecontrol.DynamicLineChartManager;
import com.example.bluetoothtemperaturecontrol.view.PasswordInputView;
import com.github.mikephil.charting.charts.LineChart;

import org.w3c.dom.ls.LSException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity implements View.OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    /**
     * View 和 表格 相关
     */
    private DynamicLineChartManager dynamicLineChartManager1;
    private LineChart mChart1;
    private List<Float> list = new ArrayList<>(); //数据集合
    private List<String> names = new ArrayList<>(); //折线名字集合
    private List<Integer> colour = new ArrayList<>();//折线颜色集合
    private EditText et_popup_item_order, et_bluetooth_ip, et_set_temp_buchang, et_set_temp_max, et_set_temp_min;
    private PasswordInputView et_set_temp, et_set_p_para, et_set_i_para, et_set_d_para, et_set_pianzhi;
    private TextView tv_power;
    private ImageView iv_setting;
    private LinearLayout ll_set_temp;

    /**
     * 蓝牙相关
     */
    private Button btn_send_order, btn_connect_bluetooth, btn_set_temp_commit,
            btn_set_zheng_temp, btn_set_fu_temp,
            btn_set_p_para, btn_set_i_para, btn_set_d_para, btn_set_zheng_pianzhi, btn_set_fu_pianzhi,
            btn_get_temp, btn_get_pianzhi, btn_get_pid, btn_reset_pid;
    private BluetoothAdapter _bluetoothAdapter;
    private BluetoothManager mBluetoothManager;
    static BluetoothDevice _device = null;     //蓝牙设备

    //Dell: 34-23-87-40-73-04
    private String HC05SAddress = "14:A3:2F:63:EB:FA";
    private float tempMax = 100F, tempMin = 0F;
    private int tempBuchang = 10;
    private BluetoothSocket _socket = null;      //蓝牙通信socket
    private static int connectSuccessful;//判断蓝牙接口
    private final static String MY_UUID = "00001101-0000-1000-8000-00805F9B34FB";   //SPP服务UUID号
    private DownloadTask dTask;
    private boolean bRun = true;
    private InputStream is;    //输入流，用来接收蓝牙数据
    private static byte[] buffer;
    private static int streamSuccessful = 1;//判断输入流
    private String date, time;
    private boolean bThread = false;
    private OutputStream os = null;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        findViews();
        initChart();

        initBlueTooth();

        registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));

        //调整到 点击连接后
        dTask = new DownloadTask();
        dTask.execute(100);

        et_set_temp.setInputListener(new PasswordInputView.InputListener() {
            @Override
            public void onInputCompleted(String text) {
                //TODO:对输入框的监听
            }
        });
        et_set_p_para.setInputListener(new PasswordInputView.InputListener() {
            @Override
            public void onInputCompleted(String text) {
                //TODO:对输入框的监听
            }
        });
        et_set_i_para.setInputListener(new PasswordInputView.InputListener() {
            @Override
            public void onInputCompleted(String text) {
                //TODO:对输入框的监听
            }
        });
        et_set_d_para.setInputListener(new PasswordInputView.InputListener() {
            @Override
            public void onInputCompleted(String text) {
                //TODO:对输入框的监听
            }
        });
        et_set_pianzhi.setInputListener(new PasswordInputView.InputListener() {
            @Override
            public void onInputCompleted(String text) {
                //TODO:对输入框的监听
            }
        });
    }

    private void initChart() {
        //折线名字
        names.add("当前温度");
        names.add("设定温度");
        //names.add("其他");
        //折线颜色
        colour.add(Color.BLUE);
        colour.add(Color.GRAY);
        //colour.add(Color.RED);

        dynamicLineChartManager1 = new DynamicLineChartManager(mChart1, names, colour);

        tempMax = sharedPreferences.getFloat("tempMax", 100F);
        tempMin = sharedPreferences.getFloat("tempMin", 0F);
        tempBuchang = sharedPreferences.getInt("tempBuchang", 10);

        dynamicLineChartManager1.setYAxis(tempMax, tempMin, tempBuchang);
    }

    private void findViews() {
        setContentView(R.layout.activity_main);

        tv_power = findViewById(R.id.tv_power);
        et_bluetooth_ip = findViewById(R.id.et_bluetooth_ip);
        iv_setting = findViewById(R.id.iv_setting);
        iv_setting.setOnClickListener(this);

        et_set_temp = findViewById(R.id.et_set_temp);
        et_set_p_para = findViewById(R.id.et_set_p_para);
        et_set_i_para = findViewById(R.id.et_set_i_para);
        et_set_d_para = findViewById(R.id.et_set_d_para);
        et_set_pianzhi = findViewById(R.id.et_set_pianzhi);

        btn_connect_bluetooth = findViewById(R.id.btn_connect_bluetooth);
        btn_set_zheng_temp = findViewById(R.id.btn_set_zheng_temp);
        btn_set_fu_temp = findViewById(R.id.btn_set_fu_temp);
        btn_set_p_para = findViewById(R.id.btn_set_p_para);
        btn_set_i_para = findViewById(R.id.btn_set_i_para);
        btn_set_d_para = findViewById(R.id.btn_set_d_para);
        btn_set_zheng_pianzhi = findViewById(R.id.btn_set_zheng_pianzhi);
        btn_set_fu_pianzhi = findViewById(R.id.btn_set_fu_pianzhi);
        btn_get_temp = findViewById(R.id.btn_get_temp);
        btn_get_pianzhi = findViewById(R.id.btn_get_pianzhi);
        btn_get_pid = findViewById(R.id.btn_get_pid);
        btn_reset_pid = findViewById(R.id.btn_reset_pid);

        btn_connect_bluetooth.setOnClickListener(this);
        btn_set_zheng_temp.setOnClickListener(this);
        btn_set_fu_temp.setOnClickListener(this);
        btn_set_p_para.setOnClickListener(this);
        btn_set_i_para.setOnClickListener(this);
        btn_set_d_para.setOnClickListener(this);
        btn_set_zheng_pianzhi.setOnClickListener(this);
        btn_get_temp.setOnClickListener(this);
        btn_get_pianzhi.setOnClickListener(this);
        btn_get_pid.setOnClickListener(this);
        btn_reset_pid.setOnClickListener(this);
        btn_set_fu_pianzhi.setOnClickListener(this);

        mChart1 = (LineChart) findViewById(R.id.dynamic_chart1);
        et_popup_item_order = findViewById(R.id.et_popup_item_order);
        btn_send_order = findViewById(R.id.btn_send_order);
        btn_send_order.setOnClickListener(this);

        et_set_temp_buchang = findViewById(R.id.et_set_temp_buchang);
        et_set_temp_max = findViewById(R.id.et_set_temp_max);
        et_set_temp_min = findViewById(R.id.et_set_temp_min);
        ll_set_temp = findViewById(R.id.ll_set_temp);

        btn_set_temp_commit = findViewById(R.id.btn_set_temp_commit);

        btn_set_temp_commit.setOnClickListener(this);


        sharedPreferences = getSharedPreferences("bluetooth", MODE_PRIVATE);
        editor = sharedPreferences.edit();
        HC05SAddress = sharedPreferences.getString("address", "14:A3:2F:63:EB:FA");
        et_bluetooth_ip.setText(HC05SAddress);
    }


    //按钮点击添加 随机 数据
    public void addEntry(View view) {
        dynamicLineChartManager1.addEntry((int) (Math.random() * 100));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_set_temp_commit:
                try {
                    tempMax = Float.parseFloat(et_set_temp_max.getText().toString());
                    tempMin = Float.parseFloat(et_set_temp_min.getText().toString());
                    tempBuchang = (int) ((tempMax - tempMin) / Integer.parseInt(et_set_temp_buchang.getText().toString()));
                    //tempBuchang = Integer.parseInt(et_set_temp_buchang.getText().toString());

                    editor.putFloat("tempMax", tempMax);
                    editor.putFloat("tempMin", tempMin);
                    editor.putInt("tempBuchang", tempBuchang);
                    editor.apply();
                    editor.commit();
                    if (ll_set_temp.getVisibility() != View.GONE)
                        ll_set_temp.setVisibility(View.GONE);
                } catch (Exception e) {
                    Toast.makeText(this, "非法输入将导致手机爆炸！\n请谨慎操作！", Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.iv_setting:
                if (ll_set_temp.getVisibility() == View.VISIBLE) {
                    ll_set_temp.setVisibility(View.GONE);
                } else {
                    ll_set_temp.setVisibility(View.VISIBLE);
                }
                break;
            case R.id.btn_connect_bluetooth:
                String temp = et_bluetooth_ip.getText().toString().replaceAll("：", ":");
                if (temp.length() < HC05SAddress.length()) {
                    Toast.makeText(MainActivity.this, "蓝牙物理地址输入有误！", Toast.LENGTH_SHORT).show();
                    break;
                }
                Message.obtain(handler, CONNECT, "请等待").sendToTarget();

                HC05SAddress = temp;
                et_bluetooth_ip.setText(HC05SAddress);
                et_bluetooth_ip.setSelection(HC05SAddress.length());
                is = null;
                isStop = true;
                if (dTask != null) {
                    dTask.mCancle();
                }

                if (_socket != null) { //关闭连接socket
                    try {
                        _socket.close();
                    } catch (IOException e) {
                    }
                }

                if (_device != null) _device = null;
                isStop = false;
                dTask = new DownloadTask();
                dTask.execute(100);
                break;

            //TODO:发送命令
            case R.id.btn_send_order:
                String order = et_popup_item_order.getText().toString();
                sendOrder(order);
                break;

            //TODO:设定正温度
            case R.id.btn_set_zheng_temp:
                String zhengTemp = fillString(et_set_temp.getText().toString(), 4);
                sendOrder("S" + zhengTemp);
                break;

            //TODO:设定负温度
            case R.id.btn_set_fu_temp:
                String fuTemp = fillString(et_set_temp.getText().toString(), 4);
                if (Integer.parseInt(fuTemp) > 6000) {
                    Toast.makeText(MainActivity.this, "请输入 0000 ~ 6000 之间的整数！", Toast.LENGTH_SHORT).show();
                    return;
                }
                sendOrder("S-" + fuTemp);

                /*StringBuilder sbFuTemp = new StringBuilder(fuTemp);
                sbFuTemp.insert(2, ".");
                String newFuTemp = sbFuTemp.toString();*/
                break;

            //TODO:设定 P 参数
            case R.id.btn_set_p_para:
                String pPara = fillString(et_set_p_para.getText().toString(), 3);
                sendOrder("SP" + pPara);
                break;

            //TODO:设定 I 参数
            case R.id.btn_set_i_para:
                String iPara = fillString(et_set_i_para.getText().toString(), 3);
                sendOrder("SI" + iPara);
                break;

            //TODO:设定 D 参数
            case R.id.btn_set_d_para:
                String dPara = fillString(et_set_d_para.getText().toString(), 3);
                sendOrder("SD" + dPara);
                break;

            //TODO:设定了正偏置
            case R.id.btn_set_zheng_pianzhi:
                String zhengPianzhi = fillString(et_set_pianzhi.getText().toString(), 4);
                if (Integer.parseInt(zhengPianzhi) > 1000) {
                    Toast.makeText(MainActivity.this, "请输入 0 ~ 1000 内的整数", Toast.LENGTH_SHORT).show();
                    return;
                }
                sendOrder("SA" + zhengPianzhi);
                break;

            //TODO:设定了负偏置
            case R.id.btn_set_fu_pianzhi:
                String fuPianzhi = fillString(et_set_pianzhi.getText().toString(), 4);
                if (Integer.parseInt(fuPianzhi) > 1000) {
                    Toast.makeText(MainActivity.this, "请输入 0 ~ 1000 内的整数", Toast.LENGTH_SHORT).show();
                    return;
                }
                sendOrder("SA-" + fuPianzhi);
                break;


            //TODO:重置 PID 参数
            case R.id.btn_reset_pid:
                sendOrder("SF");
                Message.obtain(handler,TOAST,"已发送重置PID命令").sendToTarget();
                break;

            //TODO:获取 PID 参数
            case R.id.btn_get_pid:
                sendOrder("RF");
                Message.obtain(handler,TOAST,"已发送获取PID请求").sendToTarget();
                break;

            //TODO:获取温度偏置量
            case R.id.btn_get_pianzhi:
                sendOrder("RA");
                Message.obtain(handler,TOAST,"已发送当前温度偏置量请求").sendToTarget();
                break;

            //TODO:获取环境温度
            case R.id.btn_get_temp:
                sendOrder("RC");
                Message.obtain(handler,TOAST,"已发送当前环境温度请求").sendToTarget();
                break;
            default:
                break;
        }
    }


    /**
     * 发送指定命令
     *
     * @param msg 指定的命令
     */
    public void sendOrder(String msg) {
        if (null == os || null == _socket) {
            Toast.makeText(this, "蓝牙没连接", 1000).show();
            return;
        }
        try {
            os.write(msg.getBytes());
            os.flush();
        } catch (IOException e) {
            Log.i(TAG, "sendOrder:" + msg + " 报错：" + e.getMessage());
        }
    }

    public void initBlueTooth() {
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (mBluetoothManager == null) {
            Toast.makeText(this, "设备不支持蓝牙，别玩了", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        _bluetoothAdapter = mBluetoothManager.getAdapter();

        //如果不能得到蓝牙，可以给APP定位权限并在代码中动态获取；
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //判断是否具有权限
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                //判断是否需要向用户解释为什么需要申请该权限
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION)) {
                }
                //请求权限
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 666);
            }
        }
        // 设置设备可以被搜索
        new Thread() {
            @Override
            public void run() {
                if (_bluetoothAdapter.isEnabled() == false) {
                    _bluetoothAdapter.enable();
                }
            }
        }.start();
    }

    public void connectHC05() {
        Message.obtain(handler, CONNECT, "连接中").sendToTarget();
        while (!_bluetoothAdapter.isEnabled()) {
        }

        if (_bluetoothAdapter.isDiscovering()) _bluetoothAdapter.cancelDiscovery();
        //打包log.i(TAG, "connectHC05: 搜索？" + (_bluetoothAdapter.isDiscovering()));
        if (_device == null) {
            try {

                _device = _bluetoothAdapter.getRemoteDevice(HC05SAddress);
            } catch (Exception e) {
                Message msg = new Message();
                msg.what = TOAST;
                msg.obj = "蓝牙地址输入有误！";
                handler.sendMessage(msg);
                isStop = true;
                return;
            }
        }
        try {
            if (_socket == null)
                _socket = _device.createInsecureRfcommSocketToServiceRecord(UUID.fromString(MY_UUID));
            //_socket = (BluetoothSocket) _device.getClass().getDeclaredMethod("createRfcommSocket", new Class[]{int.class}).invoke(_device, 1);
        } catch (Exception e) {
            connectSuccessful = 2;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
            }
            //打包log.i(TAG, "connectHC05: connectSuccessful = " + connectSuccessful + e.getMessage());
        }

        try {
            _socket.connect();
            connectSuccessful = 1;
            Log.i(TAG, "1 连接成功");
            Message.obtain(handler, TOAST, "蓝牙连接成功!").sendToTarget();
            Message.obtain(handler, CONNECT, "连接").sendToTarget();

            editor.putString("address", HC05SAddress);
            editor.apply();
            editor.commit();

        } catch (IOException connectException) {
            connectSuccessful = 3;
            try {
                Method m = _device.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
                _socket = (BluetoothSocket) m.invoke(_device, 1);
                _socket.connect();
                connectSuccessful = 1;
                Log.i(TAG, "2 连接成功");
            } catch (Exception e) {
                connectSuccessful = 4;
                try {
                    _socket.close();
                    _socket = null;
                } catch (IOException ie) {
                }
            }
            return;
        }

        try {
            if (connectSuccessful != 1) Thread.sleep(2000);
        } catch (InterruptedException e) {
        }
    }


    private boolean isStop = false;

    class DownloadTask extends AsyncTask<Integer, Integer, String> {

        public void mCancle() {
            this.cancel(true);
        }

        @Override
        protected String doInBackground(Integer... params) {
            while (connectSuccessful != 1) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                connectHC05();
                if (isStop) break;
            }
            return "执行完毕";
        }

        @Override
        protected void onPostExecute(String result) {
            if (isStop) return;
            if (connectSuccessful == 1) {
                Log.i(TAG, "onPostExecute: connectSuccessful = " + connectSuccessful);
                doWork();
            } else {
                _socket = null;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        connectHC05();
                    }
                }).start();
            }
            super.onPostExecute(result);
        }

        private void doWork() {
            Log.i(TAG, "进入 doWork: ");
            if (_socket != null) {
                try {
                    os = _socket.getOutputStream();
                    sendOrder("Test");
                } catch (
                        IOException e) {
                    Log.i(TAG, "doWork: _socket.getOutputStream() 报错：" + e.getMessage());
                }

                buffer = new byte[16];

                try {
                    is = _socket.getInputStream();
                } catch (
                        IOException e) {
                    streamSuccessful = 0;
                    return;
                }

                if (!bThread) {
                    ReadThread.start();
                    bThread = true;
                } else {
                    bRun = true;
                }
            }
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        //打包log.i(TAG, "onDestroy: ");
        if (_socket != null) { //关闭连接socket
            try {
                _socket.close();
//	    		is.close()
            } catch (IOException e) {
            }
        }
        if (dTask != null) {
            dTask.mCancle();
        }
        dTask = null;
        is = null;
        if (_device != null) _device = null;

        //	_bluetooth.disable();  //关闭蓝牙服务
    }

    //接收数据线程
    Thread ReadThread = new Thread() {
        @Override
        public void run() {
            Log.i(TAG, "run: 进入 ReadThread 线程");
            bRun = true;
            while (true) {
                try {
                    while (is != null && is.available() == 0) {
                        while (!bRun) {
                        }
                    }
                    while (is != null) {
                        int time = is.available();
                        if (time == 0) break;
                        buffer = new byte[time];
                        if (is != null) is.read(buffer);
                        String receivedMsg = new String(buffer, "UTF-8").trim();
                        Log.i(TAG, "run: 接收到： " + receivedMsg);
                        //TODO:绘图
                        if (receivedMsg.contains("PV:") && receivedMsg.contains("SV:")) {
                            receivedMsg = receivedMsg.replace(" ", "").replaceAll("PV:", "")
                                    .replaceAll("SV:", "");//PV:3536SV:3500-657
                            try {
                                byte[] be = receivedMsg.getBytes("gbk");
                                String pv = new String(be, 0, getStrIndex(be, 4), "gbk");
                                receivedMsg = receivedMsg.replace(pv, "");
                                be = receivedMsg.getBytes("gbk");
                                float pvFloat = Float.parseFloat(insertPoint(pv, 2));

                                String sv = new String(be, 0, getStrIndex(be, 4), "gbk");
                                float svFloat = Float.parseFloat(insertPoint(sv, 2));

                                list.add(pvFloat);
                                list.add(svFloat);
                                dynamicLineChartManager1.addEntry(list);
                                list.clear();

                                receivedMsg = receivedMsg.replace(sv, "");
                                String pw = receivedMsg.replaceAll(sv, "");
                                Message msg = new Message();
                                msg.what = POWER;
                                msg.obj = insertPoint(pw, 1);
                                handler.sendMessage(msg);
                                Log.i(TAG, "run: pv = " + pvFloat + " ; sv = " + svFloat + " ; pw = " + msg.obj);
                            } catch (UnsupportedEncodingException e) {
                                System.out.println("报错:" + e.getMessage());
                            }
                            continue;
                        }

                        //一定放在上面！
                        if (receivedMsg.contains("P:") && receivedMsg.contains("I:") && receivedMsg.contains("D:")) {
                            Message msg = new Message();
                            msg.what = RF;
                            msg.obj = receivedMsg.replace("P:", "").replace("I", "").replace("D", "");
                            handler.sendMessage(msg);
                            continue;
                        }
                        if (receivedMsg.contains("P:")) {
                            receivedMsg = receivedMsg.replace(" ", "").replaceAll("P:", "");

                            receivedMsg = insertPoint(receivedMsg, 1);

                            Message msg = new Message();
                            msg.what = TOAST;
                            msg.obj = "P 参数设置为：" + receivedMsg;
                            handler.sendMessage(msg);
                            continue;
                        }
                        if (receivedMsg.contains("I:")) {
                            receivedMsg = receivedMsg.replace(" ", "").replaceAll("I:", "");

                            receivedMsg = insertPoint(receivedMsg, 1);

                            Message msg = new Message();
                            msg.what = TOAST;
                            msg.obj = "I 参数设置为：" + receivedMsg;
                            handler.sendMessage(msg);
                            continue;
                        }
                        if (receivedMsg.contains("D:")) {
                            receivedMsg = receivedMsg.replace(" ", "").replaceAll("D:", "");

                            receivedMsg = insertPoint(receivedMsg, 1);

                            Message msg = new Message();
                            msg.what = TOAST;
                            msg.obj = "D 参数设置为：" + receivedMsg;
                            handler.sendMessage(msg);
                            continue;
                        }
                        if (receivedMsg.contains("Temp-Offset Set")) {
                            Message msg = new Message();
                            msg.what = TOAST;
                            msg.obj = "温度偏量设置成功";
                            handler.sendMessage(msg);
                            continue;
                        }
                        if (receivedMsg.contains("PID reset")) {
                            Message msg = new Message();
                            msg.what = TOAST;
                            msg.obj = "PID 参数设置为出厂值";
                            handler.sendMessage(msg);
                            continue;
                        }

                        if (receivedMsg.contains("Offset Temp:")) {
                            Message msg = new Message();
                            msg.what = RA;
                            msg.obj = receivedMsg.replaceAll("Offset Temp:", "");
                            handler.sendMessage(msg);
                            continue;
                        }
                        if (receivedMsg.contains("CPV:")) {
                            Message msg = new Message();
                            msg.what = RC;
                            msg.obj = receivedMsg.replaceAll("CPV:", "");
                            handler.sendMessage(msg);
                            continue;
                        }
                    }
                } catch (IOException e) {
                    Log.i(TAG, "run: 循环接受报错：" + e.getMessage());
                    break;
                }
            }
        }
    };

    /**
     * 根据长度截取 String
     *
     * @param be String 的数组
     * @param i  指定的长度
     * @return
     */
    public int getStrIndex(byte[] be, int i) {
        //截取字节长度必须小于字符串字节长度
        if (i < be.length) {
            //asi小于0 则递归
            if (be[i] < 0) {
                i--;
                if (i > 0) {
                    getStrIndex(be, i);
                }
            }
        }
        return i;
    }

    StringBuilder sb = new StringBuilder("temp");
    String marStrNew;
    Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case POWER:
                    tv_power.setText(msg.obj.toString());
                    break;
                case RC:
                    et_set_temp.setText(fillString(msg.obj.toString(), 4));
                    break;
                case RA:
                    et_set_pianzhi.setText(fillString(msg.obj.toString(), 4));
                    break;
                case RF:
                    String[] strs = msg.obj.toString().split(":");
                    et_set_p_para.setText(fillString(strs[0], 3));
                    et_set_i_para.setText(fillString(strs[1], 3));
                    et_set_d_para.setText(fillString(strs[2], 3));
                    break;
                case TOAST:
                    Toast.makeText(MainActivity.this, msg.obj.toString(), Toast.LENGTH_SHORT).show();
                    break;
                case CONNECT:
                    btn_connect_bluetooth.setText(msg.obj.toString());
                    break;
                default:
                    break;
            }
        }
    };

    private static final int POWER = 4;
    private static final int RC = 5;
    private static final int RA = 6;
    private static final int RF = 7;
    private static final int TOAST = 8;
    private static final int CONNECT = 9;

    private BlueToothReceiver mReceiver = new BlueToothReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (connectSuccessful != 1) {
                return;
            }
            connectSuccessful = -1;
            if (dTask != null) {
                dTask.cancel(true);
            }
            dTask = null;
            dTask = new DownloadTask();
            dTask.execute(100);
        }
    };

    /**
     * 插入小数点
     *
     * @param msg        原数据 如 3665
     * @param xiaoshuwei 小数位数：如果为 2 ；则 36.65；如果为 1 ；则 366.5
     */
    public String insertPoint(String msg, int xiaoshuwei) {
        StringBuilder sb = new StringBuilder(msg);
        sb.insert(msg.length() - xiaoshuwei, ".");
        marStrNew = sb.toString();
        return marStrNew;
    }

    /**
     * 根据指定长度填充 String
     *
     * @param res          eg:12
     * @param targetLength eg:4
     * @return eg:0012
     */
    public static String fillString(String res, int targetLength) {
        if (res.length() >= targetLength) return res;
        int resLen = res.length();
        for (int i = 0; i < targetLength - resLen; i++) {
            res = "0" + res;
        }
        return res;
    }

    public String getString(InputStream inputStream) {
        InputStreamReader inputStreamReader = null;
        try {
            inputStreamReader = new InputStreamReader(inputStream, "gbk");
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }
        BufferedReader reader = new BufferedReader(inputStreamReader);
        StringBuffer sb = new StringBuffer("");
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    /**
     * 数组元素去重
     *
     * @param strs 待去重元素
     * @return
     */
    public String[] oneClear(String[] strs) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < strs.length; i++) {
            if (!list.contains(strs[i])) {
                list.add(strs[i]);
            }
        }
        String[] newStrs = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            newStrs[i] = list.get(i);
        }
        return newStrs;
    }
}