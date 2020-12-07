package com.example.bluetoothtemperaturecontrol;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bluetoothtemperaturecontrol.view.PasswordInputView;
import com.github.mikephil.charting.charts.LineChart;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    /**
     * View 和 表格 相关
     */
    private DynamicLineChartManager dynamicLineChartManager1;
    private LineChart mChart1;
    private List<Integer> list = new ArrayList<>(); //数据集合
    private List<String> names = new ArrayList<>(); //折线名字集合
    private List<Integer> colour = new ArrayList<>();//折线颜色集合
    private EditText et_popup_item_order;
    private AutoCompleteTextView et_bluetooth_ip;
    private PasswordInputView et_set_temp, et_set_p_para, et_set_i_para, et_set_d_para, et_set_pianzhi;
    private TextView tv_power;

    /**
     * 蓝牙相关
     */
    private Button btn_send_order, btn_connect_bluetooth, btn_set_zheng_temp, btn_set_fu_temp,
            btn_set_p_para, btn_set_i_para, btn_set_d_para, btn_set_zheng_pianzhi, btn_set_fu_pianzhi,
            btn_get_temp, btn_get_pianzhi, btn_get_pid, btn_reset_pid;
    private BluetoothAdapter _bluetoothAdapter;
    private BluetoothManager mBluetoothManager;
    static BluetoothDevice _device = null;     //蓝牙设备

    //Dell: 34-23-87-40-73-04
    private String HC05SAddress = "14:A3:2F:63:EB:FA";
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        findViews();
        initChart();

        initBlueTooth();

        registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));
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
        names.add("温度");
        names.add("压强");
        names.add("其他");
        //折线颜色
        colour.add(Color.CYAN);
        colour.add(Color.GREEN);
        colour.add(Color.BLUE);

        dynamicLineChartManager1 = new DynamicLineChartManager(mChart1, names.get(0), colour.get(0));

        dynamicLineChartManager1.setYAxis(100, 0, 10);

        //死循环添加数据
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            list.add((int) (Math.random() * 50) + 10);
                            list.add((int) (Math.random() * 80) + 10);
                            list.add((int) (Math.random() * 100));
                            list.clear();
                        }
                    });
                }
            }
        }).start();
    }

    String[] autoStrings = new String[]{"联合国", "联合国安理会", "联合国五个常任理事国",
            "Google", "Google Map"};

    private void findViews() {
        setContentView(R.layout.activity_main);

        tv_power = findViewById(R.id.tv_power);
        et_bluetooth_ip = (AutoCompleteTextView) findViewById(R.id.et_bluetooth_ip);
        et_set_temp = findViewById(R.id.et_set_temp);
        et_set_p_para = findViewById(R.id.et_set_p_para);
        et_set_i_para = findViewById(R.id.et_set_i_para);
        et_set_d_para = findViewById(R.id.et_set_d_para);
        et_set_pianzhi = findViewById(R.id.et_set_pianzhi);
        String[] autoStrings = new String[]{"联合国", "联合国安理会", "联合国五个常任理事国",
                "Google", "Google Map"};
        // 第二个参数表示适配器的下拉风格
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this,
                android.R.layout.simple_dropdown_item_1line, autoStrings);
        et_bluetooth_ip.setAdapter(adapter);

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
    }

    //按钮点击添加数据
    public void addEntry(View view) {
        dynamicLineChartManager1.addEntry((int) (Math.random() * 100));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_connect_bluetooth:
                if (et_bluetooth_ip.getText().toString().length() != 16) break;

                HC05SAddress = formatAddress(et_bluetooth_ip.getText().toString());

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
                is = null;
                isStop = false;
                dTask = new DownloadTask();
                dTask.execute(100);
                break;
            case R.id.btn_send_order:
                if (os == null) {
                    Toast.makeText(this, "蓝牙没连接", 1000).show();
                    return;
                }
                //TODO:发送命令
                String order = et_popup_item_order.getText().toString();
                try {
                    if (null == _socket) return;
                    os.write(order.getBytes());
                    os.flush();
                } catch (IOException e) {
                    Log.i(TAG, "onClick: 报错：" + e.getMessage());
                }
                Log.i(TAG, "onClick: _socket = " + _socket);
                break;
            case R.id.btn_set_zheng_temp:
                //TODO:设定正温度
                if (os == null) {
                    Toast.makeText(this, "蓝牙没连接", 1000).show();
                    return;
                }
                String zhengTemp = et_set_temp.getText().toString();
                try {
                    if (null == _socket) return;
                    os.write(("S" + zhengTemp).getBytes());
                    os.flush();
                } catch (IOException e) {
                    Log.i(TAG, "onClick: 报错：" + e.getMessage());
                }

                StringBuilder sb = new StringBuilder(zhengTemp);
                sb.insert(2, ".");
                String marStrNew = sb.toString();
                Toast.makeText(MainActivity.this, "设定了正温度：" + marStrNew, Toast.LENGTH_SHORT).show();
                break;
            case R.id.btn_set_fu_temp:
                //TODO:设定负温度
                if (os == null) {
                    Toast.makeText(this, "蓝牙没连接", 1000).show();
                    return;
                }
                String fuTemp = et_set_temp.getText().toString();
                if (Integer.parseInt(fuTemp) > 6000) {
                    Toast.makeText(MainActivity.this, "请输入 0000 ~ 6000 之间的整数！", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    if (null == _socket) return;
                    os.write(("S-" + fuTemp).getBytes());
                    os.flush();
                } catch (IOException e) {
                    Log.i(TAG, "onClick: 报错：" + e.getMessage());
                }

                StringBuilder sbFuTemp = new StringBuilder(fuTemp);
                sbFuTemp.insert(2, ".");
                String newFuTemp = sbFuTemp.toString();
                Toast.makeText(MainActivity.this, "设定了负温度：- " + newFuTemp, Toast.LENGTH_SHORT).show();
                break;
            case R.id.btn_set_p_para:
                //TODO:设定 P 参数
                if (os == null) {
                    Toast.makeText(this, "蓝牙没连接", 1000).show();
                    return;
                }
                String pPara = et_set_p_para.getText().toString();
                try {
                    if (null == _socket) return;
                    os.write(("SP" + pPara).getBytes());
                    os.flush();
                } catch (IOException e) {
                    Log.i(TAG, "onClick: 报错：" + e.getMessage());
                }

                StringBuilder sbPPara = new StringBuilder(pPara);
                sbPPara.insert(2, ".");
                String newPPara = sbPPara.toString();
                Toast.makeText(MainActivity.this, "设定了 P 参数：" + newPPara, Toast.LENGTH_SHORT).show();
                break;
            case R.id.btn_set_i_para:
                //TODO:设定 I 参数
                if (os == null) {
                    Toast.makeText(this, "蓝牙没连接", 1000).show();
                    return;
                }
                String iPara = et_set_i_para.getText().toString();
                try {
                    if (null == _socket) return;
                    os.write(("SI" + iPara).getBytes());
                    os.flush();
                } catch (IOException e) {
                    Log.i(TAG, "onClick: 报错：" + e.getMessage());
                }

                StringBuilder sbIPara = new StringBuilder(iPara);
                sbIPara.insert(2, ".");
                String newIPara = sbIPara.toString();
                Toast.makeText(MainActivity.this, "设定了 I 参数：" + newIPara, Toast.LENGTH_SHORT).show();
                break;
            case R.id.btn_set_d_para:
                //TODO:设定 D 参数
                if (os == null) {
                    Toast.makeText(this, "蓝牙没连接", 1000).show();
                    return;
                }
                String dPara = et_set_d_para.getText().toString();
                try {
                    if (null == _socket) return;
                    os.write(("SD" + dPara).getBytes());
                    os.flush();
                } catch (IOException e) {
                    Log.i(TAG, "onClick: 报错：" + e.getMessage());
                }

                StringBuilder sbDPara = new StringBuilder(dPara);
                sbDPara.insert(2, ".");
                String newDPara = sbDPara.toString();
                Toast.makeText(MainActivity.this, "设定了 D 参数：" + newDPara, Toast.LENGTH_SHORT).show();
                break;
            case R.id.btn_set_zheng_pianzhi:
                //TODO:设定了正偏置
                if (os == null) {
                    Toast.makeText(this, "蓝牙没连接", 1000).show();
                    return;
                }
                String zhengPianzhi = et_set_pianzhi.getText().toString();
                if (Integer.parseInt(zhengPianzhi) > 1000) {
                    Toast.makeText(MainActivity.this, "请输入 0 ~ 1000 内的整数", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    if (null == _socket) return;
                    os.write(("SA" + zhengPianzhi).getBytes());
                    os.flush();
                } catch (IOException e) {
                    Log.i(TAG, "onClick: 报错：" + e.getMessage());
                }

                StringBuilder sbZhengPianzhi = new StringBuilder(zhengPianzhi);
                sbZhengPianzhi.insert(2, ".");
                String newZhengPianzhi = sbZhengPianzhi.toString();
                Toast.makeText(MainActivity.this, "设定了正偏置：" + newZhengPianzhi, Toast.LENGTH_SHORT).show();
                break;
            case R.id.btn_set_fu_pianzhi:
                //TODO:设定了负偏置
                if (os == null) {
                    Toast.makeText(this, "蓝牙没连接", 1000).show();
                    return;
                }
                String fuPianzhi = et_set_pianzhi.getText().toString();
                if (Integer.parseInt(fuPianzhi) > 1000) {
                    Toast.makeText(MainActivity.this, "请输入 0 ~ 1000 内的整数", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    if (null == _socket) return;
                    os.write(("SA-" + fuPianzhi).getBytes());
                    os.flush();
                } catch (IOException e) {
                    Log.i(TAG, "onClick: 报错：" + e.getMessage());
                }

                StringBuilder sbFuPianzhi = new StringBuilder(fuPianzhi);
                sbFuPianzhi.insert(2, ".");
                String newFuPianzhi = sbFuPianzhi.toString();
                Toast.makeText(MainActivity.this, "设定了负偏置：- " + newFuPianzhi, Toast.LENGTH_SHORT).show();
                break;
            case R.id.btn_get_temp:
                //TODO:获取环境温度
                Toast.makeText(MainActivity.this, "获取环境温度", Toast.LENGTH_SHORT).show();
                break;
            case R.id.btn_get_pianzhi:
                //TODO:获取温度偏置量
                Toast.makeText(MainActivity.this, "获取温度偏置量：", Toast.LENGTH_SHORT).show();
                break;
            case R.id.btn_get_pid:
                //TODO:获取 PID 参数
                Toast.makeText(MainActivity.this, "获取 PID 参数：", Toast.LENGTH_SHORT).show();
                break;
            case R.id.btn_reset_pid:
                //TODO:重置 PID 参数
                Toast.makeText(MainActivity.this, "重置了 PID 参数：", Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
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
        while (!_bluetoothAdapter.isEnabled()) {
        }

        if (_bluetoothAdapter.isDiscovering()) _bluetoothAdapter.cancelDiscovery();
        //打包log.i(TAG, "connectHC05: 搜索？" + (_bluetoothAdapter.isDiscovering()));
        if (_device == null) {
            _device = _bluetoothAdapter.getRemoteDevice(HC05SAddress);
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
                } catch (IOException e) {
                    Log.i(TAG, "doWork: _socket.getOutputStream() 报错：" + e.getMessage());
                }

                buffer = new byte[16];

                try {
                    is = _socket.getInputStream();
                } catch (IOException e) {
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
                        String receivedMsg = new String(buffer);


                        //TODO:绘图
                        if (receivedMsg.contains("TEMP")) {
                            receivedMsg = receivedMsg.replaceAll("TEMP:", "");
                            float f = Float.parseFloat(receivedMsg);
                            dynamicLineChartManager1.addEntry(f);
                        }
                        if (receivedMsg.contains("POWER")) {
                            Message msg = new Message();
                            msg.what = POWER;
                            msg.obj = receivedMsg.replaceAll("POWER:", "");
                            handler.sendMessage(msg);
                        }

                        if (receivedMsg.contains("CPV:")) {
                            Message msg = new Message();
                            msg.what = RC;
                            msg.obj = receivedMsg.replaceAll("CPV:", "");
                            handler.sendMessage(msg);
                        }

                        if (receivedMsg.contains("Offset Temp:")) {
                            Message msg = new Message();
                            msg.what = RA;
                            msg.obj = receivedMsg.replaceAll("Offset Temp:", "");
                            handler.sendMessage(msg);
                        }

                        if (receivedMsg.contains("P:")&&receivedMsg.contains("I:")&&receivedMsg.contains("D:")) {
                            Message msg = new Message();
                            msg.what = RF;
                            msg.obj = receivedMsg.replace("P:","").replace("I","").replace("D","");
                            handler.sendMessage(msg);
                        }

                    }
                } catch (IOException e) {
                    break;
                }
            }
        }
    };

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
                    et_set_temp.setText(msg.obj.toString());
                    /*sb = new StringBuilder(msg.obj.toString());
                    sb.insert(2, ".");
                    marStrNew = sb.toString();
                    et_popup_item_order.setText(marStrNew + "℃");*/
                    break;
                case RA:
                    et_set_pianzhi.setText(msg.obj.toString());
                    break;
                case RF:
                    String[] strs = msg.obj.toString().split(":");
                    et_set_p_para.setText(strs[0]);
                    et_set_i_para.setText(strs[1]);
                    et_set_d_para.setText(strs[2]);
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

    public String formatAddress(String newAddress) {
        if (TextUtils.isEmpty(newAddress)) return null;
        String result = "";
        for (int i = 0; i < newAddress.length(); i++) {
            result += newAddress.charAt(i);
            if (i % 2 != 0 && i < newAddress.length() - 1) {//偶！数！位！最后一个位置不补 :
                result += ":";
            }
        }
        Log.i(TAG, "格式化后的地址格式：" + result);
        return result;
    }
}