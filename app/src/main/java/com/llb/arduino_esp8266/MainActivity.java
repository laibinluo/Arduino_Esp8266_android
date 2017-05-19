package com.llb.arduino_esp8266;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.menu.ExpandedMenuView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    Socket socket = null;
    String buffer = "";
    Button button;
    Button led;
    Button led2;
    boolean  isConnet = true;
    MyThread myThread;
    int open = 1;

    public Handler myHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0x11) {
                Bundle bundle = msg.getData();
                Toast.makeText(getApplicationContext(), bundle.getString("msg"), Toast.LENGTH_LONG).show();
            }else if (msg.what == 2){
                Toast.makeText(getApplicationContext(), "连接成功", Toast.LENGTH_LONG).show();
                button.setText("断开");
            }
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);

        button = (Button) findViewById(R.id.connect);
        led  = (Button) findViewById(R.id.led) ;
        led.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                open = 1;
            }
        });

        led2 = (Button) findViewById(R.id.led2);
        led2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                open = 0;
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if("断开".equals(button.getText().toString())){
                    isConnet = false;
                    button.setText("连接");
                    return;
                }
                EditText ip = (EditText) findViewById(R.id.ip);
                EditText port = (EditText) findViewById(R.id.port);
                String ipStr = ip.getText().toString();
                int portInt = Integer.parseInt(port.getText().toString());
                Log.d(TAG, "IP : " + ipStr + ", PORT : " + portInt);
                myThread  = new MyThread(ipStr, portInt);
                myThread.start();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isConnet = false;
    }

    class MyThread extends Thread {

        public String ip;
        public int port;
        public int data;

        public MyThread(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }

        @Override
        public void run() {
            //定义消息
            Message msg = new Message();
            msg.what = 0x11;
            Bundle bundle = new Bundle();
            bundle.clear();
            try {
                //连接服务器 并设置连接超时为5秒
                socket = new Socket();
                socket.connect(new InetSocketAddress(ip, port), 5000);
                //获取输入输出流
                OutputStream ou = socket.getOutputStream();
                BufferedReader bff = new BufferedReader(new InputStreamReader(
                        socket.getInputStream()));

                if (socket.isConnected()){
                    Log.d(TAG, "SOCKET connect successed");
                    myHandler.sendEmptyMessage(2);
                }else {
                    Log.d(TAG, "SOCKET connect faild");
                }

                //向服务器发送信息
                ou.write("android connect ".getBytes("gbk"));
                ou.flush();
                while (isConnet){
                    if (socket.isConnected()){
                        Log.d(TAG, "SOCKET connect successed");
                        myHandler.sendEmptyMessage(2);
                    }else {
                        Log.d(TAG, "SOCKET connect faild");
                    }

                    if (data != open ){
                        data = open;
                        Log.d(TAG, "data : " + data);
                        ou.write(String.valueOf(data).getBytes("gbk"));
                        ou.flush();
                    }else {
                        Log.d(TAG, "data = OPEN: " + data);
                        try {
                            Thread.sleep(1000);
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }
                //发送消息 修改UI线程中的组件
                myHandler.sendMessage(msg);
                //关闭各种输入输出流
                bff.close();
                ou.close();
                socket.close();
            } catch (SocketTimeoutException aa) {
                //连接超时 在UI界面显示消息
                bundle.putString("msg", "服务器连接失败！请检查网络是否打开");
                msg.setData(bundle);
                //发送消息 修改UI线程中的组件
                myHandler.sendMessage(msg);
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "e : " + e.toString());
            }
        }
    }
}
