// 通信・MySQLに必要なライブラリを読み込み
var http = require('http');
var mysql = require('mysql');
//Androidから送られているデータの数
const dataNum = 20;
var sensorData;
//センサーデータを格納する配列。10個のデータ×２０＋識別子＋\n
var entry = [];
//送られてきたセンサーデータ。
var text;
//データが途中で途切れてしまった時の、最初のデータ
var oldtext;
var checkflag = 0;
var temp;
// MySQLサーバへの接続
var connection = mysql.createConnection({
    user: 'tomoya', // ユーザ名
    password: 'basuapuri', // パスワード
    database: 'serversensordb' // データベース名
});
// HTTPサーバの実行
var server = http.createServer();
// サーバにHTTPリクエストが届いたときのコールバック関数
//requestはイベント名。発生するイベントはhttp.Serverオブジェクトに元々用意されているもの。
//右側の引数は自分で設定可能。今回はあらかじめ用意されているrequestオブジェクト,responseオブジェクトを使用
server.on('request', function (req, res) {
    // スマートフォンからのアクセスがPOSTであった場合
    if (req.method == 'POST') {
        // スマートフォンへ返信するデータのヘッダを作成
        res.writeHead(200, {
            'Content-Type': req.headers['content-type']
        });

        // スマートフォンからデータを受信したときの処理 (コールバック関数)
        req.on('data', function (data) {
            // スマートフォンから受信したデータ(文字列)を、文字列に変換
            text = data.toString('utf8');
            // 文字列を、データベースに入力する各データに分割
            entry = text.split(",");
            if(entry[200]==null && checkflag == 0){
                oldtext = text;
                checkflag = 1;
            }else if(entry[200]==null && checkflag == 1){
                temp = text;
                text = oldtext;
                text = text.concat(temp);
                entry = text.split(",");
                checkflag=0;
            }
            //console.log('entry[0]:'+entry[0]);
//            for(var i = 0; i < dataNum; i++){
//                console.log('entry[1]:'+entry[1+i*10]+' '+'entry[2]:'+entry[2+i*10]+' '+'entry[3]:'+entry[3+i*10]+' '+'entry[4]:'+entry[4+i*10]+' '+'entry[5]:'+entry[5+i*10]+' '+'entry[6]:'+entry[6+i*10]+' '+'entry[7]:'+entry[7+i*10]+' '+'entry[8]:'+entry[8+i*10]+' '+'entry[9]:'+entry[9+i*10]+' '+'entry[10]:'+entry[10+i*10]);
//            }
            // 受信ログを表示
            console.log(req.method + ' "' + req.headers['user-agent'] + '"');
            //console.log(text);

            // データの入力履歴をスマートフォンに返信
            //res.write('Input > ID = ' + entry[0] + ', Data = ' + entry[1]);
            res.write('Complete Receive');
        });
        // スマートフォンから全てのデータを受信したときの処理（コールバック関数）
        req.on('end', function () {
            // スマートフォンへのデータの返信を終了
            res.end();
        });
        console.log('受信');
        //console.log('entry[0]=',entry[0]);
        //データベースに格納
        if (entry[0] == "gps") {
            //データベースのテーブル(gps)にデータ入力
            connection.query('INSERT INTO gps SET time = ?, latitude = ?, longitude = ?', [entry[1], entry[2], entry[3]]);
        } else if (entry[0] == "sensor") {
            for (var i = 0; i < dataNum; i++) {
                // データベースのテーブル(sensornode)にデータを入力
                connection.query('INSERT INTO sensornode SET time = ?, acceleration_x = ?, acceleration_y = ?, acceleration_z = ?, linear_acceleration_x = ?, linear_acceleration_y = ?, linear_acceleration_z = ?, gyroscope_x = ?, gyroscope_y = ?, gyroscope_z = ?',
                        [entry[1 + i * 10], entry[2 + i * 10], entry[3 + i * 10], entry[4 + i * 10], entry[5 + i * 10], entry[6 + i * 10], entry[7 + i * 10], entry[8 + i * 10], entry[9 + i * 10], entry[10 + i * 10]]);
                console.log('Client Request > ' + entry[1 + i * 10] + ',' + entry[2 + i * 10] + ',' + entry[3 + i * 10] + ',' + entry[4 + i * 10] + ',' + entry[5 + i * 10] + ',' + entry[6 + i * 10] + ',' + entry[7 + i * 10] + ',' + entry[8 + i * 10] + ',' + entry[9 + i * 10] + ',' + entry[10 + i * 10]);
            }
        }
        entry.fill(0);
        //console.log('初期化後entry[0]=',entry[0]);
    }
    // スマートフォンからのアクセスがPOST以外の場合
    else {
        res.writeHead(200, {
            'Content-Type': 'text/plain'
        });
        res.write('Hello World\n');
        res.end();
    }
});

// スマートフォンからの接続を待ち受けるポート番号の設定(3000番)
//server.listen(80,'133.19.62.7');
server.listen(80,'133.19.62.7', function(){process.setuid(80)});