package com.example.kimuratomoya.sensor_db;

/**
 * Created by kimuratomoya on 2017/04/28.
 */
import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class ShowSensorDataBase extends Activity {
    private int MATCH_PARENT = ViewGroup.LayoutParams.MATCH_PARENT;
    private int WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MyOpenHelper helper = new MyOpenHelper(this);
        SQLiteDatabase db = helper.getReadableDatabase();
//        setContentView(R.layout.show_database);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new ScrollView.LayoutParams(MATCH_PARENT, MATCH_PARENT));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT,MATCH_PARENT));

        scrollView.addView(layout);
        setContentView(scrollView);


        // 検索で全てのカラムを表示する文。テーブル名の次にカラム名を指定するとそのカラムのみ表示される
        //例えばdb.query("sensor",new String[]{"created_at","acceleration_x"},null,null,null,null,null);
        //Cursor c = db.query("sensor", new String[] { "created_at", "acceleration_x" }, null, null, null, null, null);
        Cursor c = db.query("sensor", null, null, null, null, null, null);

        //moveToFirstを実行。実行内容はカーソルを取得したレコードの先頭に移動させるという意味。
        //成功したらtureが返される。取得したレコードが0件ならfalseが返される。
        boolean mov = c.moveToFirst();
        Log.d("sensor","8");
        //moveToFirstが成功していたらwhile文に入る
        while (mov) {
            TextView record = new TextView(this);
            record.setLayoutParams(new LinearLayout.LayoutParams(WRAP_CONTENT,WRAP_CONTENT));
            Log.d("sensor","8.1");
            Log.d("sensor",c.getString(0));
            Log.d("sensor","" + c.getDouble(1));
            Log.d("sensor","" + c.getDouble(2));
            Log.d("sensor","" + c.getDouble(3));
            Log.d("sensor","" + c.getDouble(4));
            Log.d("sensor","" + c.getDouble(5));
            Log.d("sensor","" + c.getDouble(6));
            Log.d("sensor","" + c.getDouble(7));
            Log.d("sensor","" + c.getDouble(8));
            Log.d("sensor","" + c.getDouble(9));
            //c.get系の()の中はカラムの順番(インデックス)。
            record.setText(String.format("%s\n加速度: %f | %f | %f\n直線加速度: %f | %f | %f\nジャイロ: %f | %f | %f",
                    c.getString(0),
                    c.getDouble(1),
                    c.getDouble(2),
                    c.getDouble(3),
                    c.getDouble(4),
                    c.getDouble(5),
                    c.getDouble(6),
                    c.getDouble(7),
                    c.getDouble(8),
                    c.getDouble(9)));
            //record.setText(String.format("%s : %f",c.getString(0),c.getDouble(1)));
            Log.d("sensor","8.2");
            //カーソルを次のレコードに移動する。成功ならtrue,次のレコードが無いならfalse。
            mov = c.moveToNext();
            Log.d("sensor","8.3");
            layout.addView(record);
            Log.d("sensor","8.4");
        }
        Log.d("sensor","9");
        c.close();
        db.close();
    }
}
