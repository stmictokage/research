package com.example.kimuratomoya.sensor_db;

import android.os.Debug;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.ActivityCompat;
import android.content.pm.PackageManager;
import android.Manifest;
// 測位に関係があるパッケージ
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.content.Context;
// 画面表示に関連するパッケージ
import android.view.WindowManager;

import java.sql.Time;
import java.util.List;
        import java.lang.System;
        import java.text.SimpleDateFormat;
        import java.util.Date;
        import android.hardware.Sensor;
        import android.hardware.SensorEvent;
        import android.hardware.SensorEventListener;
        import android.hardware.SensorManager;
        import android.content.ContentValues;
        import android.content.Intent;
        import android.database.sqlite.SQLiteDatabase;
        import android.view.View;
        import android.view.View.OnClickListener;
        import android.os.Bundle;
        import android.widget.TextView;
        import android.widget.Button;
        import android.util.Log;
import java.io.IOException;
// HTTP通信に関連するパッケージ
import java.net.HttpURLConnection;
import java.net.URL;
// サーバとの読み書きに関するパッケージ
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
//定期的に実行するためのパッケージ
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// スレッド
import android.os.Handler;

public class MainActivity extends AppCompatActivity implements SensorEventListener, LocationListener{
    private final static String BR = System.getProperty("line.separator");
    private LocationManager locationmanager; // ロケーションマネージャ
    private SensorManager sensormanager;
    private Context context = this;
    private Handler handler = new Handler();
    private TextView values;
    private TextView linevalues;
    private TextView gyrovalues;
    private TextView gps;
    private TextView time;
    //gpsのデータをサーバに送る＆画面表示用のテキスト
    private String sendgps;
    private String gpstext;
    //センサーのデータをサーバに送る用のテキスト
    private String sendsensor1[] = new String[dataNum];
    private String sendsensor2[] = new String[dataNum];
    public double[] sum = new double[10];
    public double[] oldsum = new double[4];
    public int i=0;//ループ変数
    public int first=0;
    private SQLiteDatabase db;
    //スケジュール用(定期的実行)
    //ScheduledExecutorService ex = Executors.newSingleThreadScheduledExecutor();
    long start = System.currentTimeMillis();
    //直前に送信したセンサーデータがセンサーデータ１かセンサーデータ２かを格納
    private int checksend = 0;
    //サーバへ一気に送るセンサーデータの数
    static final int dataNum = 20;

//    @Override
//    public void onStart() {
//        super.onStart();
//        ex.scheduleAtFixedRate(new Runnable() {
//            @Override
//            public void run() {
//
//            }
//        }, 2000, dataNum*60,TimeUnit.MILLISECONDS);
//    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // アプリ実行中はスリープしない
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        //データベースの定義
        MyOpenHelper helper = new MyOpenHelper(this);
        db = helper.getWritableDatabase();
        //明示的にonCreate()を実行することで、GPSテーブルを作成
        //helper.onCreate(db);

        values = (TextView)findViewById(R.id.value_id);
        linevalues = (TextView)findViewById(R.id.linevalue_id);
        gyrovalues = (TextView)findViewById(R.id.gyrovalue_id);
        gps = (TextView)findViewById(R.id.gps_id);
        time = (TextView)findViewById(R.id.time_id);
        //センサーマネージャの取得
        sensormanager = (SensorManager)getSystemService(SENSOR_SERVICE);
        // ロケーションマネージャの取得
        locationmanager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        //センサーのデータベース閲覧＆データ削除
        Button sensorDetaBaseButton = (Button)findViewById(R.id.sensorDataBase);
        sensorDetaBaseButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent sensorDbIntent = new Intent(MainActivity.this,
                        ShowSensorDataBase.class);
                startActivity(sensorDbIntent);
            }
        });
        Button sensordeleteButton = (Button)findViewById(R.id.sensorDelete);
        sensordeleteButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                db.delete("sensor",null, null);
            }
        });
        //GPSのデータベース閲覧＆データ削除
        Button gpsDetaBaseButton = (Button)findViewById(R.id.gpsDataBase);
        gpsDetaBaseButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent gpsDbIntent = new Intent(MainActivity.this,
                        ShowGPSDataBase.class);
                startActivity(gpsDbIntent);
            }
        });
        Button gpsDeleteButton = (Button)findViewById(R.id.gpsDelete);
        gpsDeleteButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                db.delete("gps",null, null);
            }
        });
    }

    @Override
    protected void onStop() {

        super.onStop();
        // Listenerの登録解除
        sensormanager.unregisterListener(this);

        //ロケーションマネージャーの設定 - 測位を停止
        if (locationmanager != null) {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationmanager.removeUpdates(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Listenerの登録
        List<Sensor> sensors = sensormanager.getSensorList(Sensor.TYPE_ACCELEROMETER);
        List<Sensor> sensors1 = sensormanager.getSensorList(Sensor.TYPE_LINEAR_ACCELERATION);
        List<Sensor> sensors2 = sensormanager.getSensorList(Sensor.TYPE_GYROSCOPE);
        if(sensors.size() > 0) {
            Sensor s = sensors.get(0);
            sensormanager.registerListener(this, s, SensorManager.SENSOR_DELAY_UI);
        }
        if(sensors1.size() > 0) {
            Sensor s = sensors1.get(0);
            sensormanager.registerListener(this, s, SensorManager.SENSOR_DELAY_UI);
        }
        if(sensors2.size() > 0) {
            Sensor s = sensors2.get(0);
            sensormanager.registerListener(this, s, SensorManager.SENSOR_DELAY_UI);
        }

        // GPSの利用許可を利用者に求める
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]
                    {Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }
        // ロケーションマネージャの設定 - 測位方法を指定して測位を開始
        locationmanager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        String text = "位置情報を取得しています...";
        gps.setText(text);

        start = System.currentTimeMillis();
//        Log.d("sensor", "Start Time" + start);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    //位置情報が変更されたときに呼ばれるコールバック関数
    public void onLocationChanged(Location location) {
        Long time;
        Double latitude;
        Double longitude;
        String timestamp;

        time = location.getTime();
        latitude = location.getLatitude();
        longitude = location.getLongitude();

        Date date = new Date(time);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        timestamp = simpleDateFormat.format(date);

        ContentValues insertValues = new ContentValues();
        insertValues.put("created_at", timestamp);
        insertValues.put("latitude", latitude);
        insertValues.put("longitude", longitude);
        long id = db.insert("gps", (String.valueOf(100)), insertValues);
        //緯度と経度の取得
        sendgps = "gps" + "," + timestamp + "," +latitude + "," + longitude;
        gpstext = "GPS Data" + BR +
                "時刻: " + time + BR +
                "緯度: " + latitude + BR +
                "経度: " + longitude;
        gps.setText(gpstext); // テキストビューの更新
        // 通信用のスレッドを起動
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 通信先のURLを指定 (http://IPアドレス:ポート番号)
                String path = "http://133.19.62.7:80";
                HttpURLConnection conn = null;
                try {
                    // サーバとのHTTPコネクションを確立
                    URL url = new URL(path);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST"); // 通信方式はPOSTを指定
                    conn.setDoOutput(true);
                    conn.setDoInput(true);
                    // サーバへメッセージを送信
                    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
                    bw.write(sendgps);
                    bw.close();
                    // サーバからメッセージを受信
                    final String str = InputStreamToString(conn.getInputStream());
//                    // サーバから受信したメッセージをトーストで表示
//                    handler.post(new Runnable(){
//                        @Override
//                        public void run(){
//                            Toast.makeText(context, str, Toast.LENGTH_LONG).show();
//                        }
//                    });
                }catch(Exception e){
                    try {
                        if(conn != null) conn.disconnect();
                    }catch (Exception e2){
                    }
                }
            }
        }).start();
    }

    // サーバから受信したデータを文字列に変換
    static String InputStreamToString(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();
        return sb.toString();
    }
    //位置情報取得有効化が通知されたときに呼ばれるコールバック関数
    public void onProviderEnabled(String provider) {
        String text = "位置情報の取得に成功しました。";
        gps.setText(text);
    }

    //位置情報取得無効化が通知されたときに呼ばれるコールバック関数
    public void onProviderDisabled(String provider) {
        String text = "位置情報の取得に失敗しました。";
        gps.setText(text);
    }

    //位置情報状態が変更されたときに呼ばれるコールバック関数
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            String str = "加速度センサー値:"
                    + "\nX軸:" + event.values[0]
                    + "\nY軸:" + event.values[1]
                    + "\nZ軸:" + event.values[2];
            values.setText(str);
            sum[0] = (double)event.values[0];
            sum[1] = (double)event.values[1];
            sum[2] = (double)event.values[2];
        }
        if(event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            String str = "直線加速度センサー値:"
                    + "\nX軸:" + event.values[0]
                    + "\nY軸:" + event.values[1]
                    + "\nZ軸:" + event.values[2];
            linevalues.setText(str);
            sum[3] = (double)event.values[0];
            sum[4] = (double)event.values[1];
            sum[5] = (double)event.values[2];
        }

        if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            String str = "ジャイロセンサー値:"
                    + "\nX軸:" + event.values[0]
                    + "\nY軸:" + event.values[1]
                    + "\nZ軸:" + event.values[2];
            gyrovalues.setText(str);
            sum[6] = (double)event.values[0];
            sum[7] = (double)event.values[1];
            sum[8] = (double)event.values[2];
        }
        if((first==0)||(sum[0]!=oldsum[0]&&sum[3]!=oldsum[1]&&sum[6]!=oldsum[2])) {
            String timestamp;
            //UNIX時間の取得
            long currentTimeMillis = System.currentTimeMillis();
            Date date = new Date(currentTimeMillis);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            timestamp = simpleDateFormat.format(date);
            time.setText(timestamp);
            //time.setText(simpleDateFormat.format(date));
            //データがアップデートされていた場合、データベースへ格納
            //データベースへデータを挿入する前の一時的な入れ物の用意
            ContentValues insertValues = new ContentValues();


            //tssize[0] = timestamp.getBytes(Charset.forName("UTF-8"));
            //決められたカラム(nameとかage)の入れ物にデータを挿入
            insertValues.put("created_at", simpleDateFormat.format(date));
            insertValues.put("acceleration_x", sum[0]);
            insertValues.put("acceleration_y", sum[1]);
            insertValues.put("acceleration_z", sum[2]);
            insertValues.put("linear_acceleration_x", sum[3]);
            insertValues.put("linear_acceleration_y", sum[4]);
            insertValues.put("linear_acceleration_z", sum[5]);
            insertValues.put("gyroscope_x", sum[6]);
            insertValues.put("gyroscope_y", sum[7]);
            insertValues.put("gyroscope_z", sum[8]);

            //実際にデータベースへデータを挿入
            //insert(テーブル名、nullが許されないカラムに値が指定されていない場合に代わりに挿入される値,入れ物の値)
            long id = db.insert("sensor", (String.valueOf(100)), insertValues);
            //サーバへ送る用のテキストにデータを挿入
            if(sendsensor1[dataNum-1]==null) {
                if(i==0) {
                    sendsensor1[i] = "sensor" + "," + simpleDateFormat.format(date) + "," + sum[0] + "," + sum[1] + "," + sum[2] + ","
                            + sum[3] + "," + sum[4] + "," + sum[5] + "," + sum[6] + "," + sum[7] + "," + sum[8];
                    Log.d("check", "Store 1:" + i);
                    //Log.d("check", "sendsensor1:" + sendsensor1[i]);
                }else if(i>0){
                    sendsensor1[i] = "," + simpleDateFormat.format(date) + "," + sum[0] + "," + sum[1] + "," + sum[2] + ","
                            + sum[3] + "," + sum[4] + "," + sum[5] + "," + sum[6] + "," + sum[7] + "," + sum[8];
                    Log.d("check", "Store 1:" + i);
                    //Log.d("check", "sendsensor1:" + sendsensor1[i]);
                }
                i++;
                if(i==dataNum){
                    Log.d("sensor", "センサー１格納完了");
                }
            }else if(sendsensor1[dataNum-1]!=null){
                if(i==0) {
                    sendsensor2[i] = "sensor" + "," + simpleDateFormat.format(date) + "," + sum[0] + "," + sum[1] + "," + sum[2] + ","
                            + sum[3] + "," + sum[4] + "," + sum[5] + "," + sum[6] + "," + sum[7] + "," + sum[8];
                    Log.d("check", "Store 2:" + i);
                    //Log.d("check", "sendsensor2:" + sendsensor2[i]);
                }else if(i>0){
                    sendsensor2[i] = "," + simpleDateFormat.format(date) + "," + sum[0] + "," + sum[1] + "," + sum[2] + ","
                            + sum[3] + "," + sum[4] + "," + sum[5] + "," + sum[6] + "," + sum[7] + "," + sum[8];
                    Log.d("check", "Store 2:" + i);
                    //Log.d("check", "sendsensor2:" + sendsensor2[i]);
                }
                i++;
                if(i==dataNum){
                    sendsensor1[dataNum-1]=null;
                    Log.d("sensor", "センサー2格納完了");
                }
            }
            if(i==dataNum){
                i=0;
                Log.d("sensor", "スレッド立ち上げ");
                // 通信用のスレッドを起動
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // 通信先のURLを指定 (http://IPアドレス:ポート番号)
                        String path = "http://192.168.100.56:3000";
                        HttpURLConnection conn = null;
                        try {
                            // サーバとのHTTPコネクションを確立
                            URL url = new URL(path);
                            conn = (HttpURLConnection) url.openConnection();
                            conn.setRequestMethod("POST"); // 通信方式はPOSTを指定
                            conn.setDoOutput(true);
                            conn.setDoInput(true);
                            // サーバへメッセージを送信
                            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
                            if (sendsensor1[dataNum-1] != null && (checksend == 0 || checksend == 2)) {
                                for (int j = 0; j < dataNum; j++) {
                                    bw.write(sendsensor1[j]);
                                    //Log.d("check", "Send 1:" + j);
                                }
                                Log.d("sensor", "Send 1:" + sendsensor1[dataNum-1]);
                                Log.d("check", "Send 1:");
                                checksend = 1;
                            } else if (sendsensor2[dataNum-1] != null && checksend == 1) {
                                for (int j = 0; j < dataNum; j++) {
                                    bw.write(sendsensor2[j]);
                                    //Log.d("check", "Send 2:" + j);
                                }
                                checksend = 2;
                                Log.d("check", "Send 2:");
                                Log.d("sensor", "Send 2:" + sendsensor2[dataNum-1]);
                            }
                            bw.close();
                            // サーバからメッセージを受信
                            final String str = InputStreamToString(conn.getInputStream());
//                    // サーバから受信したメッセージをトーストで表示
//                    handler.post(new Runnable(){
//                        @Override
//                        public void run(){
//                            Toast.makeText(context, str, Toast.LENGTH_LONG).show();
//                        }
//                    });
                        } catch (Exception e) {
                            try {
                                if (conn != null) conn.disconnect();
                            } catch (Exception e2) {
                            }
                        }
                    }
                }).start();
            }
            //一個前のデータを保存(データの更新を検知するため)
            oldsum[0] = sum[0];
            oldsum[1] = sum[3];
            oldsum[2] = sum[6];
            first = 1;
        }
    }
}
