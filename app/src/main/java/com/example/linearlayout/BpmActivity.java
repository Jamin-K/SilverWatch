package com.example.linearlayout;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.linearlayout.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BpmActivity extends AppCompatActivity {

    private AppCompatButton btn_state;
    public ProgressDialog asyncDialog;
    private TextView et_bpm;
    private int state = 0; // 0 = 휴식 중, 1 = 운동 중
    private int btnCount = 0;
    private int bpm = 0; //측정 BPM
    private int[] abpm= new int[5]; //측정 BPM
    private int cnt=0;
    private double longitude;
    private double latitude;
    private String url = "http://maps.google.com/maps?q=";
    private String ip = "192.168.35.47";
    private String TAG = "UpdateQuery";

    BluetoothAdapter mBluetoothAdapter;
    Set<BluetoothDevice> mPairedDevices;
    List<String> mListPairedDevices;

    Handler mBluetoothHandler;
    ConnectedBluetoothThread mThreadConnectedBluetooth;
    BluetoothDevice mBluetoothDevice;
    BluetoothSocket mBluetoothSocket;

    final static int BT_REQUEST_ENABLE = 1;
    final static int BT_MESSAGE_READ = 2;
    final static int BT_CONNECTING_STATUS = 3;
    final static UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private int age;
    private String phone ;
    private int sex;
    private String name;
    private String wearPhone;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN); //상태바 제거
        setContentView(R.layout.activity_bpm);
        Intent intent = getIntent();
        phone = intent.getStringExtra("phone");
        age = Integer.parseInt(intent.getStringExtra("age"));
        sex = Integer.parseInt(intent.getStringExtra("sex"));
        name = intent.getStringExtra("name");
        wearPhone = intent.getStringExtra("wear_ph");

        btn_state = findViewById(R.id.btn_state);
        et_bpm = findViewById(R.id.et_bpm);
        ActionBar actionBar = getSupportActionBar(); //타이틀 바 제거
        actionBar.hide();

        /*
        위치기록 테스트
         */
        checkSelfPermission();
        final LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        String provider = location.getProvider();
        longitude = location.getLongitude();
        latitude = location.getLatitude();
        System.out.println(longitude);
        System.out.println(latitude);

        System.out.println("초기호출");

        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                1000,
                1,
                gpsLocationListener);
        lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                1000,
                1,
                gpsLocationListener);


        /*
        블루투스 연결 심박수 가져오기
         */

        /*
        블루투스
            https://ddangeun.tistory.com/59 참조
        https://hyoin1223.tistory.com/entry/%EC%95%88%EB%93%9C%EB%A1%9C%EC%9D%B4%EB%93%9C-%EB%B8%94%EB%A3%A8%ED%88%AC%EC%8A%A4-%ED%94%84%EB%A1%9C%EA%B7%B8%EB%9E%98%EB%B0%8D
         */
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if(mBluetoothAdapter == null){ //장치가 블루투스를 지원하지 않을 경우
            Toast.makeText(getApplicationContext(), "블루투스를 지원하지 않는 기기입니다.", Toast.LENGTH_SHORT).show();
        }
        else{//블루투스를 지원 할 경우
            if(!mBluetoothAdapter.isEnabled()){ //지원을 기능하지만 켜져있지 않은 경우
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, BT_REQUEST_ENABLE);
            }
            else{
                //블루투스를 지원하며 기능이 켜져있을 경우 페어링 된 기기 묵록을 보여주고 장치 선택
                Set<BluetoothDevice> pariedDevice = mBluetoothAdapter.getBondedDevices();
                if(pariedDevice.size() > 0){ //페어링 장치가 있을 경우
                    listPairedDevices();
                }
                else{ //페어링 장치가 없을 경우
                    Toast.makeText(getApplicationContext(), "페어링을 진행해주세요.", Toast.LENGTH_SHORT).show();
                }
            }
        }

        btn_state.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnCount++;
                if(btnCount%2 == 1) {
                    btn_state.setText("운동중");
                    state = 1;
                }
                else {
                    btn_state.setText("휴식중");
                    state = 0;
                }
            }
        });
        mBluetoothHandler = new Handler(){
            @SuppressLint("HandlerLeak")
            public void handleMessage(android.os.Message msg){
                if(msg.what == BT_MESSAGE_READ){
                    String readMessage = null;
                    try {
                        readMessage = new String((byte[]) msg.obj, "UTF-8");
                        readMessage = readMessage.trim();
                        bpm = Integer.parseInt(readMessage);
                        abpm[cnt]=bpm;
                        cnt++;
                        if(cnt>4) {
                            int avg = 0;
                            for (int i = 0; i < abpm.length; i++) {
                                avg += abpm[i];
                            }
                            avg = avg / 5;
                            if (avg != 0) { //심박수 값을 가져와서 그 값이 0이 아닐때 화면 상 bpm 표기
                                if (avg >= 100)
                                    et_bpm.setText(Integer.toString(avg));
                                else
                                    et_bpm.setText("0" + Integer.toString(avg));
                            }
                            if(checkEmergency(age, sex, avg,state)==1) {
                                sendSMS(phone,  name+" : 심박수 이상 발생 확인요망\n심박수 :"+avg+"\n");
                                sendSMS(phone,  url + Double.toString(latitude) + "," + Double.toString(longitude));

                                UpdateData task = new UpdateData();
                                task.execute("http://" + ip + "/insert_heart.php", wearPhone, Integer.toString(avg));
                                System.out.println("심박수 이상, 문자전송");
                            }
                            cnt = 0;
                        }
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                }
            }
        };
    }

    void listPairedDevices() {
        if (mBluetoothAdapter.isEnabled()) {
            mPairedDevices = mBluetoothAdapter.getBondedDevices();

            if (mPairedDevices.size() > 0) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("장치 선택");

                mListPairedDevices = new ArrayList<String>();
                for (BluetoothDevice device : mPairedDevices) {
                    mListPairedDevices.add(device.getName());
                }
                final CharSequence[] items = mListPairedDevices.toArray(new CharSequence[mListPairedDevices.size()]);
                mListPairedDevices.toArray(new CharSequence[mListPairedDevices.size()]);

                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        connectSelectedDevice(items[item].toString());
                    }
                });
                AlertDialog alert = builder.create();
                alert.show();
            } else {
                Toast.makeText(getApplicationContext(), "페어링된 장치가 없습니다.", Toast.LENGTH_LONG).show();
            }
        }
        else {
            Toast.makeText(getApplicationContext(), "블루투스가 비활성화 되어 있습니다.", Toast.LENGTH_SHORT).show();
        }
    }
    void connectSelectedDevice(String selectedDeviceName) {
        for(BluetoothDevice tempDevice : mPairedDevices) {
            if (selectedDeviceName.equals(tempDevice.getName())) {
                mBluetoothDevice = tempDevice;
                break;
            }
        }
        try {
            mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(BT_UUID);
            mBluetoothSocket.connect();
            mThreadConnectedBluetooth = new ConnectedBluetoothThread(mBluetoothSocket); //데이터를 언제 수신 받을 지 모르기때문에 수신을 위한 별도의 쓰레드 생성
            mThreadConnectedBluetooth.start();
            mBluetoothHandler.obtainMessage(BT_CONNECTING_STATUS, 1, -1).sendToTarget();
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "블루투스 연결 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
        }
    }

    private class ConnectedBluetoothThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedBluetoothThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "소켓 연결 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
        public void run() { //수신 받은 데이터가 있는지 확인하는 과정. 데이터 수신을 위함
            byte[] buffer = new byte[1024];
            int bytes=0;

            while (true) {
                try {
                    bytes = mmInStream.available();
                    if (bytes != 0) {
                        SystemClock.sleep(100);
                        bytes = mmInStream.available(); //읽을 수 있는 바이트 수 리턴
                        bytes = mmInStream.read(buffer, 0, bytes); //현재 스트림에서 byte 만큼을 읽어와서 buffer에 저장하고 읽은 개수를 bytes에 저장
                        mBluetoothHandler.obtainMessage(BT_MESSAGE_READ, bytes, -1, buffer).sendToTarget();//메세지를 핸들러에게 전달
                        //if(buffer[])
                        /*for(int i=0 ; i<bytes; i++) {
                            byte b = packetBytes[i];
                            if(b == mDelimiter) {
                                byte[] encodedBytes = new byte[readBufferPosition];
                                System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                final String data = new String(encodedBytes, "US-ASCII");
                                readBufferPosition = 0;

                                handler.post(new Runnable() {
                                    public void run() {
                                        // 수신된 문자열 데이터에 대한 처리 작업
                                    }
                                });
                            }
                            else {
                                readBuffer[readBufferPosition++] = b;
                            }
                        }


                    }
                }


                출처: https://hyoin1223.tistory.com/entry/안드로이드-블루투스-프로그래밍 [lionhead]*/
                    }
                } catch (IOException e) {
                    break;
                }
            }

        }
        public void write(String str) { //데이터 전송을 위함
            byte[] bytes = str.getBytes();
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "데이터 전송 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
            }
        }
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "소켓 해제 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
            }
        }
    }



    private void sendSMS(String phone, String textSMS) {
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phone, null, textSMS, null, null);
    }

    private void checkSelfPermission() {
        { //위험권한만 확인
            String temp = "";
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                temp = temp + Manifest.permission.ACCESS_FINE_LOCATION + " ";
            }
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED){
                temp = temp + Manifest.permission.SEND_SMS + " ";
            }
            else
                Toast.makeText(this, "권한 허용 완료", Toast.LENGTH_SHORT).show();
        }
    }

    final LocationListener gpsLocationListener = new LocationListener() {
        public void onLocationChanged(Location location) {

            String provider = location.getProvider();
            double longitude = location.getLongitude();
            double latitude = location.getLatitude();
            double altitude = location.getAltitude();


            System.out.println("위치 변환에 따른 갱신");

        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onProviderDisabled(String provider) {
        }
    };

    private int checkEmergency(int age, int sex, int bpm, int state){
        int emergency = 0; // 0 = 평상시, 1 = 위급

        if(sex == 0){//남자
            if(state == 0) {
                /*
                안정된 상태
                 */
                if (age >= 40 && age <= 59) {
                    if (bpm < 52 || bpm > 90) {
                        emergency = 1;
                    }
                } else if (age >= 60 && age <= 79) {
                    if (bpm < 50 || bpm > 91) {
                        emergency = 1;
                    }
                } else if (age >= 80) {
                    if (bpm < 51 || bpm > 94) {
                        emergency = 1;
                    }
                }
            }
            else{ //최대 심박수 = 220-나이
             /*
             운동 중 상태
              */
                if(220 - age < bpm){
                    emergency = 1;
                }

            }
        }
        else{//여자
            if(state == 0){
                /*
                안정된 상태
                 */
                if(age >= 40 && age <= 59){
                    if (bpm < 56 || bpm > 92){
                        emergency = 1;
                    }
                }
                else if(age >= 60 && age <= 79){
                    if(bpm< 56 || bpm > 92){
                        emergency = 1;
                    }
                }
                else if(age >= 80){
                    if(bpm < 56 || bpm > 93){
                        emergency = 1;
                    }
                }
            }
            else{
                /*
                운동 중 상태
                 */
                if(220 - age < bpm){
                    emergency = 1;
                }
            }
        }

        return emergency;
    }

    class UpdateData extends AsyncTask<String, Void, String> {
        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

           //progressDialog = ProgressDialog.show(BpmActivity.this, "Please Wait", null, true, true);
        }


        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            //progressDialog.dismiss();
            state = result.charAt(0);
            if(result.charAt(0) == '1') {
                Toast.makeText(getApplicationContext(), "업데이트성공", Toast.LENGTH_LONG).show();
                //finish();

            }
            else
                Toast.makeText(getApplicationContext(), "업데이트실패", Toast.LENGTH_LONG).show();
            Log.d(TAG, "POST response  - " + result);
        }

        @Override
        protected String doInBackground(String... params) {

            String wearPhone = (String)params[1];
            String bpm = (String)params[2];

            String serverURL = (String)params[0];
            String postParameters = "wear_ph=" + wearPhone + "&bpm=" + bpm;

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

                Log.d(TAG, "UpdateQuery: Error ", e);

                return new String("Error: " + e.getMessage());
            }

        }
    }
}


/*
http://hqcenter.snu.ac.kr/archives/jiphyunjeon/%EB%82%98%EC%97%90%EA%B2%8C-%EB%A7%9E%EB%8A%94-%EC%9A%B4%EB%8F%99%EA%B0%95%EB%8F%84-%EC%84%A4%EC%A0%95-2
국민건강지식센터, 최대심박수
 */

