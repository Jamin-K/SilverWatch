package com.example.linearlayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;


public class MainActivity extends AppCompatActivity {


    private AppCompatButton btn_login;
    private AppCompatButton btn_register;
    private EditText et_phone;
    private String age;
    private String master_ph;
    private String name;
    private String sex;
    private String wearPhone;
    private String ip = "192.168.35.47";
    private String TAG = "phpquery";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        /*
        퍼미션 요청
        https://www.bsidesoft.com/6824 참고
         */

        checkSelfPermission();
        et_phone = findViewById(R.id.et_phone);
        btn_login = findViewById(R.id.btn_login);
        btn_register = findViewById(R.id.btn_register);
        ActionBar actionBar = getSupportActionBar(); //타이틀 바 제거
        actionBar.hide();

        TelephonyManager telManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE); //어플 사용자 휴대전화번호 호출
        wearPhone = telManager.getLine1Number();
        if(wearPhone.startsWith("+82")){
            wearPhone = wearPhone.replace("+82", "0");
        }

        btn_register.setOnClickListener(new View.OnClickListener() { //회원가입
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
                intent.putExtra("wearPhone", wearPhone);
                startActivity(intent);
            }
        });


        btn_login.setOnClickListener(new View.OnClickListener() { //로그인
            @Override
            public void onClick(View v) {

                GetData task = new GetData();
                task.execute(et_phone.getText().toString(), wearPhone);

            }
        });

    }

    private class GetData extends AsyncTask<String, Void, String> {

        ProgressDialog progressDialog;
        String errorString = null;

        @Override
        protected void onPreExecute() { //작업 시작 전 초기 호출
            super.onPreExecute();

            progressDialog = ProgressDialog.show(MainActivity.this,
                    "Please Wait", null, true, true);
        }

        @Override
        protected void onPostExecute(String result) { //AsynceTask의 모든 작업이 완료된 후 가장 마지막에 한번 호출. doinBackgroind() 함수의 값을 받음, 여기서 JSON값 추출할 것
            super.onPostExecute(result);
            progressDialog.dismiss();
            Log.d(TAG, "response - " + result);
            if (result == null){
            }
            else {
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    String jaemin = jsonObject.getString("jaemin");
                    JSONArray jsonArray = new JSONArray(jaemin);
                    for(int i = 0 ; i<jsonArray.length() ; i++){
                        JSONObject subObject = jsonArray.getJSONObject(i);
                        name = subObject.getString("name");
                        age = subObject.getString("age");
                        sex = subObject.getString("sex");
                        master_ph = subObject.getString("master_ph");

                        Intent intent = new Intent(MainActivity.this, BpmActivity.class); //BpmActivity로 변환
                        intent.putExtra("age",age);
                        intent.putExtra("phone",master_ph);
                        intent.putExtra("name", name);
                        intent.putExtra("sex",sex);
                        intent.putExtra("wear_ph",wearPhone);
                        startActivity(intent);
                        //System.out.println(name+age+sex);

                    }

                } catch (JSONException e) {
                    Toast.makeText(getApplicationContext(), "보호자 전화번호를 확인해주세요", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }



            }
        }


        @Override
        protected String doInBackground(String... params) { //스레드에 의해 처리될 내용들

            String searchKeyword1 = params[0];
            String searchKeyword2 = params[1];

            String serverURL = "http://"+ip+"/query.php";
            String postParameters = "master_ph=" + searchKeyword1 + "&wear_ph=" + searchKeyword2;
            //String postParameters = "master_ph=" + "011" + "&wear_ph=" + "010";

            try {

                URL url = new URL(serverURL);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setReadTimeout(5000);
                httpURLConnection.setConnectTimeout(5000);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setDoInput(true);
                httpURLConnection.connect();

                OutputStream outputStream = httpURLConnection.getOutputStream();
                outputStream.write(postParameters.getBytes("UTF-8"));
                outputStream.flush();
                outputStream.close();

                int responseStatusCode = httpURLConnection.getResponseCode();
                Log.d(TAG, "response code - " + responseStatusCode);
                InputStream inputStream;
                if(responseStatusCode == HttpURLConnection.HTTP_OK) {
                    inputStream = httpURLConnection.getInputStream();
                }
                else{
                    inputStream = httpURLConnection.getErrorStream();
                }

                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                StringBuilder sb = new StringBuilder();
                String line;

                while((line = bufferedReader.readLine()) != null){
                    sb.append(line);
                }


                bufferedReader.close();


                return sb.toString().trim();


            } catch (Exception e) {

                Log.d(TAG, "InsertData: Error ", e);
                errorString = e.toString();

                return null;
            }

        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == 1){ // 권한을 허용했을 경우
            int length  = permissions.length;
            for(int i = 0 ; i<length ; i++){
                if(grantResults[i] == PackageManager.PERMISSION_GRANTED){
                    //권한 허용
                    Log.d("MainActivity", "권한허용 : " + permissions[i]);
                }
            }
        }
    }

    private void checkSelfPermission() { //위험권한만 확인
        String temp = "";
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            temp = temp + Manifest.permission.ACCESS_FINE_LOCATION + " ";
        }

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            temp = temp + Manifest.permission.ACCESS_COARSE_LOCATION + " ";
        }

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED){
            temp = temp + Manifest.permission.SEND_SMS + " ";
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED){
            temp = temp + Manifest.permission.READ_PHONE_NUMBERS + " ";
        }

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED){
            temp = temp + Manifest.permission.READ_PHONE_STATE + " ";
        }

        if(TextUtils.isEmpty(temp) == false){ // 권한 없을 시 요청
            ActivityCompat.requestPermissions(this, temp.trim().split(" "), 1);
        }
        else
            Toast.makeText(this, "권한 허용 완료", Toast.LENGTH_SHORT).show();
    }




}