package com.zhang.testapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.alibaba.fastjson.JSONObject;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    public static final int FAIL = -1;
    public static final int CODESUCCESS = 1;
    public static final int FLOWSUCCESS = 2;
    public static final int REPEATCODE = 3;

    EditText phoneText;
    EditText codeText;
    String respCode;
    String respMsg;

    private Handler handler = new Handler(Looper.getMainLooper()){

        public void handleMessage(Message msg){
            switch(msg.what){
                case CODESUCCESS:
                    Toast.makeText(MainActivity.this,"验证码发送成功，请注意查收",Toast.LENGTH_LONG).show();
                break;
                case FLOWSUCCESS:
                    Toast.makeText(MainActivity.this,"30M流量领取成功，请稍后查看短信",Toast.LENGTH_LONG).show();
                    break;
                case REPEATCODE:
                    Toast.makeText(MainActivity.this,"一天最多只能发送三次短信",Toast.LENGTH_LONG).show();
                    break;
                default:
                    if (respMsg == null){
                        Toast.makeText(MainActivity.this,"操作失败",Toast.LENGTH_LONG).show();
                    }else {
                        Toast.makeText(MainActivity.this,respMsg,Toast.LENGTH_LONG).show();
                    }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button sendCode = (Button) findViewById(R.id.sendCode);
        sendCode.setOnClickListener(this);
        Button getFlow = (Button) findViewById(R.id.getFlow);
        getFlow.setOnClickListener(this);
        phoneText = (EditText) findViewById(R.id.editPhone);
        codeText = (EditText) findViewById(R.id.editCode);
        //还原SharedPreferences中储存的手机号，免得重复输入手机号
        SharedPreferences flowPhone = getSharedPreferences("flowPhone", MODE_PRIVATE);
        String phone = flowPhone.getString("phone","");
        phoneText.setText(phone);
        //把光标移到最后
        phoneText.setSelection(phone.length());
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.sendCode){
            sendCode();
        }
        if (v.getId() == R.id.getFlow){
            getFlow();
            codeText.setText("");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //储存手机号到SharedPreferences
        SharedPreferences.Editor editor = getSharedPreferences("flowPhone", MODE_PRIVATE).edit();
        editor.putString("phone",phoneText.getText().toString());
        editor.apply();
    }

    private void sendCode(){
        new Thread(() -> {
            try {
                String phone = phoneText.getText().toString();
                OkHttpClient client = new OkHttpClient();
                RequestBody requestBody = new FormBody.Builder()
                        .add("phoneVal", phone)
                        .add("type", String.valueOf(21))
                        .build();
                Request request = new Request.Builder()
                        .url("https://m.10010.com/god/AirCheckMessage/sendCaptcha")
                        .post(requestBody)
                        .build();
                Response response = client.newCall(request).execute();
                String responseBody = response.body().string();
                JSONObject jsonObject = JSONObject.parseObject(responseBody);
                respCode = (String) jsonObject.get("RespCode");
                respMsg = (String) jsonObject.get("RespMsg");
                Message message = new Message();
                if ("0000".equals(respCode)){
                    message.what = CODESUCCESS;
                }else if ("10001".equals(respCode)){
                    message.what = REPEATCODE;
                }else {
                    message.what = FAIL;
                }
                handler.sendMessage(message);
                Log.d("发送验证码的接口返回值", responseBody);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void getFlow(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String phone = phoneText.getText().toString();
                    String code = codeText.getText().toString();
                    OkHttpClient client = new OkHttpClient();
                    RequestBody requestBody = new FormBody.Builder()
                            .add("number", phone)
                            .add("type", String.valueOf(21))
                            .add("captcha", code)
                            .build();
                    Request request = new Request.Builder()
                            .url("https://m.10010.com/god/qingPiCard/flowExchange")
                            .post(requestBody)
                            .build();
                    Response response = client.newCall(request).execute();
                    String responseBody = response.body().string();
                    JSONObject jsonObject = JSONObject.parseObject(responseBody);
                    respCode = (String) jsonObject.get("respCode");
                    respMsg = (String) jsonObject.get("respDesc");
                    Message message = new Message();
                    if ("0000".equals(respCode)){
                        message.what = FLOWSUCCESS;
                    }else {
                        message.what = FAIL;
                    }
                    handler.sendMessage(message);
                    Log.d("领取流量的接口返回值", responseBody);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}