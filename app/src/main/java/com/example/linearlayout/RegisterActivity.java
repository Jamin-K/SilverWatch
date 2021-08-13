package com.example.linearlayout;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class RegisterActivity extends AppCompatActivity {

    private String ip = "192.168.35.47";
    private String TAG = "phptest";
    private AppCompatButton btn_admit;
    private EditText et_age;
    private EditText et_phone;
    private EditText et_name;
    private RadioGroup rg_sex;
    private int age;
    private String wearName;
    private String phone;
    private String msg;
    private String wearPhone;
    private int sex=2; //남자 = 0 , 여자 = 1
    private char state;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN); //상태바 제거
        setContentView(R.layout.activity_register);

        ActionBar actionBar = getSupportActionBar(); //타이틀 바 제거
        actionBar.hide();

        Intent getIntent = getIntent();
        wearPhone = getIntent.getExtras().getString("wearPhone");

        btn_admit = findViewById(R.id.btn_admit);
        et_age = findViewById(R.id.et_age);
        et_phone = findViewById(R.id.et_phone);
        et_name = findViewById(R.id.et_name);
        rg_sex = findViewById(R.id.rg_sex);

        rg_sex.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if(checkedId == R.id.rbtn_men){
                    sex= 0;
                    System.out.println("남자선택");
                }
                else if(checkedId == R.id.rbtn_women){
                    sex = 1;
                    System.out.println("여자선택");
                }
            }
        });

        btn_admit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(sex == 2){
                    Toast.makeText(getApplicationContext(), "성별을 체크해주세요", Toast.LENGTH_LONG).show();
                }
                else if(et_age.getText().toString().equals("") || et_age.getText().toString() == null){
                    Toast.makeText(getApplicationContext(), "나이를 입력해주세요", Toast.LENGTH_LONG).show();
                }
                else if(et_name.getText().toString().equals("") || et_name.getText().toString() == null){
                    Toast.makeText(getApplicationContext(), "이름을 입력해주세요", Toast.LENGTH_LONG).show();
                }
                else if(et_phone.getText().toString().equals("") || et_phone.getText().toString() == null){
                    Toast.makeText(getApplicationContext(), "연락처를 입력해주세요", Toast.LENGTH_LONG).show();
                }
                else {
                    age = Integer.parseInt(et_age.getText().toString()); //getText() 시 Editable 형식 반환
                    phone = et_phone.getText().toString();
                    wearName = et_name.getText().toString();

                    InsertData task = new InsertData();
                    task.execute("http://" + ip + "/insert.php", wearName, Integer.toString(sex), Integer.toString(age), wearPhone, phone); //이름, 성별, 나이, 착용자 번호, 보호자번호
                   /*
                   DB와의 통신 기술
                    */
                }
            }
        });
    }


    class InsertData extends AsyncTask<String, Void, String> {
        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog = ProgressDialog.show(RegisterActivity.this,
                    "Please Wait", null, true, true);
        }


        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            progressDialog.dismiss();
            state = result.charAt(0);
            if(result.charAt(0) == '1') {
                Toast.makeText(getApplicationContext(), "가입성공", Toast.LENGTH_LONG).show();
                InsertDataHeart heart_task = new InsertDataHeart();
                heart_task.execute("http://" + ip + "/insert_heart.php", wearPhone);
                //finish();

            }
            else
                Toast.makeText(getApplicationContext(), "가입실패", Toast.LENGTH_LONG).show();
            Log.d(TAG, "POST response  - " + result);
        }

        @Override
        protected String doInBackground(String... params) {

            String wearName = (String)params[1];
            String sex = (String)params[2];
            String age = (String)params[3];
            String wearPhone = (String)params[4];
            String phone = (String)params[5];

            String serverURL = (String)params[0];
            String postParameters = "name=" + wearName + "&sex=" + sex + "&age=" + age + "&wear_ph=" + wearPhone + "&master_ph=" + phone;

            try {

                URL url = new URL(serverURL);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();


                httpURLConnection.setReadTimeout(5000);
                httpURLConnection.setConnectTimeout(5000);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.connect();


                OutputStream outputStream = httpURLConnection.getOutputStream();
                outputStream.write(postParameters.getBytes("UTF-8"));
                outputStream.flush();
                outputStream.close();


                int responseStatusCode = httpURLConnection.getResponseCode();
                Log.d(TAG, "POST response code - " + responseStatusCode);

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
                String line = null;

                while((line = bufferedReader.readLine()) != null){
                    sb.append(line);
                }


                bufferedReader.close();

                return sb.toString();



            } catch (Exception e) {

                Log.d(TAG, "InsertData: Error ", e);

                return new String("Error: " + e.getMessage());
            }

        }
    }


    class InsertDataHeart extends AsyncTask<String, Void, String> {
        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            //progressDialog = ProgressDialog.show(RegisterActivity.this,"Please Wait", null, true, true);
        }


        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            //progressDialog.dismiss();
            state = result.charAt(0);
            if(result.charAt(0) == '1') {
                Toast.makeText(getApplicationContext(), "데이터 입력 성공", Toast.LENGTH_LONG).show();
                finish();

            }
            else
                Toast.makeText(getApplicationContext(), "데이터 입력 실패", Toast.LENGTH_LONG).show();
            Log.d(TAG, "POST response  - " + result);
        }

        @Override
        protected String doInBackground(String... params) {

            String wearPhone = (String)params[1];

            String serverURL = (String)params[0];
            String postParameters = "wear_ph=" + wearPhone;
            try {

                URL url = new URL(serverURL);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();


                httpURLConnection.setReadTimeout(5000);
                httpURLConnection.setConnectTimeout(5000);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.connect();


                OutputStream outputStream = httpURLConnection.getOutputStream();
                outputStream.write(postParameters.getBytes("UTF-8"));
                outputStream.flush();
                outputStream.close();


                int responseStatusCode = httpURLConnection.getResponseCode();
                Log.d(TAG, "POST response code - " + responseStatusCode);

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
                String line = null;

                while((line = bufferedReader.readLine()) != null){
                    sb.append(line);
                }


                bufferedReader.close();

                return sb.toString();



            } catch (Exception e) {

                Log.d(TAG, "InsertDataHeart: Error ", e);

                return new String("Error: " + e.getMessage());
            }

        }
    }



}
//https://webnautes.tistory.com/828