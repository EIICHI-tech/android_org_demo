package com.example.voiceclimate;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.os.Bundle;
import android.speech.*;
import android.speech.tts.TextToSpeech;
import android.view.*;
import java.util.*;

public class MainActivity extends Activity implements RecognitionListener, TextToSpeech.OnInitListener {
    private static final int REQUEST_AUDIO = 10;
    private ClimateView view;
    private SpeechRecognizer recognizer;
    private Intent speechIntent;
    private TextToSpeech tts;
    private boolean listening = false;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(5894);
        view = new ClimateView(); setContentView(view);
        tts = new TextToSpeech(this, this);
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO);
        else initRecognizer();
    }

    private void initRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) { view.status = "音声認識サービスがありません"; view.invalidate(); return; }
        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(this);
        speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja-JP");
        speechIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        speechIntent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false);
    }

    private void toggleListening() {
        if (recognizer == null) { initRecognizer(); if (recognizer == null) return; }
        if (listening) { recognizer.stopListening(); listening = false; view.status = "停止しました"; }
        else { listening = true; view.status = "お話しください…"; recognizer.startListening(speechIntent); }
        view.invalidate();
    }

    private void processCommand(String raw) {
        String s = raw.replace("　", " ").toLowerCase(Locale.JAPAN);
        view.lastCommand = raw;
        List<String> replies = new ArrayList<>();
        boolean handled = false;
        if (containsAny(s,"エアコンつけ","エアコンをつけ","空調つけ","空調をつけ","オンにして")) { view.power=true; replies.add("エアコンをオンにしました"); handled=true; }
        if (containsAny(s,"エアコン消して","エアコンを消して","空調消して","オフにして")) { view.power=false; replies.add("エアコンをオフにしました"); handled=true; }
        Integer temp = extractTemperature(s);
        if (temp != null) { int t=Math.max(16,Math.min(30,temp)); view.driverTemp=t; if(view.sync)view.passengerTemp=t; replies.add("温度を"+t+"度に設定しました"); handled=true; }
        if (containsAny(s,"温度を上げ","暖かく","暑くして")) { changeTemp(1); replies.add("温度を1度上げました"); handled=true; }
        if (containsAny(s,"温度を下げ","涼しく","寒くして","暑い")) { changeTemp(-1); replies.add("温度を1度下げました"); handled=true; }
        if (containsAny(s,"風量を上げ","風を強く","最大風量","風量最大")) { view.fan = containsAny(s,"最大") ? 7 : Math.min(7,view.fan+1); replies.add("風量を"+(view.fan==7?"最大に":"上げ")+"しました"); handled=true; }
        if (containsAny(s,"風量を下げ","風を弱く")) { view.fan=Math.max(1,view.fan-1); replies.add("風量を下げました"); handled=true; }
        if (containsAny(s,"オート","自動")) { view.auto=!containsAny(s,"解除","切って","オフ"); replies.add("オート運転を"+(view.auto?"開始":"解除")+"しました"); handled=true; }
        if (containsAny(s,"内気","循環")) { view.recirc=true; replies.add("内気循環にしました"); handled=true; }
        if (containsAny(s,"外気")) { view.recirc=false; replies.add("外気導入にしました"); handled=true; }
        if (containsAny(s,"フロントデフロスター","フロントガラス","曇りを取")) { view.front=true; replies.add("フロントデフロスターをオンにしました"); handled=true; }
        if (containsAny(s,"リアデフォッガー","リアガラス")) { view.rear=true; replies.add("リアデフォッガーをオンにしました"); handled=true; }
        if (containsAny(s,"同期","シンク")) { view.sync=true; view.passengerTemp=view.driverTemp; replies.add("左右の温度を同期しました"); handled=true; }
        if (!handled) replies.add("すみません。エアコンの操作として理解できませんでした");
        String response = joinJapanese(replies); view.status=response; view.invalidate(); speak(response);
    }

    private void changeTemp(int d) { view.driverTemp=Math.max(16,Math.min(30,view.driverTemp+d)); if(view.sync)view.passengerTemp=view.driverTemp; }
    private boolean containsAny(String s,String... keys){ for(String k:keys) if(s.contains(k)) return true; return false; }
    private Integer extractTemperature(String s) {
        java.util.regex.Matcher m=java.util.regex.Pattern.compile("(1[6-9]|2[0-9]|30)(?:\\.0)?\\s*(?:度|℃)").matcher(s);
        return m.find()?Integer.parseInt(m.group(1)):null;
    }
    private String joinJapanese(List<String> xs){ StringBuilder b=new StringBuilder(); for(String x:xs){ if(b.length()>0)b.append("。 "); b.append(x); } return b.toString(); }
    private void speak(String text){ if(tts!=null) tts.speak(text,TextToSpeech.QUEUE_FLUSH,null,"climate"); }

    @Override public void onInit(int status){ if(status==TextToSpeech.SUCCESS){ tts.setLanguage(Locale.JAPAN); tts.setSpeechRate(1.05f); } }
    @Override public void onReadyForSpeech(Bundle b){ view.status="認識中…"; view.invalidate(); }
    @Override public void onBeginningOfSpeech(){}
    @Override public void onRmsChanged(float rms){ view.micLevel=Math.max(0,Math.min(1,(rms+2)/12)); view.invalidate(); }
    @Override public void onBufferReceived(byte[] b){}
    @Override public void onEndOfSpeech(){ listening=false; view.micLevel=0; view.invalidate(); }
    @Override public void onError(int e){ listening=false; view.micLevel=0; view.status=(e==SpeechRecognizer.ERROR_NO_MATCH||e==SpeechRecognizer.ERROR_SPEECH_TIMEOUT)?"聞き取れませんでした。もう一度お話しください":"音声認識エラー: "+e; view.invalidate(); }
    @Override public void onResults(Bundle b){ listening=false; ArrayList<String> r=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION); if(r!=null&&!r.isEmpty())processCommand(r.get(0)); }
    @Override public void onPartialResults(Bundle b){ ArrayList<String> r=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION); if(r!=null&&!r.isEmpty()){ view.status=r.get(0); view.invalidate(); } }
    @Override public void onEvent(int t,Bundle b){}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){ super.onRequestPermissionsResult(r,p,g); if(r==REQUEST_AUDIO&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)initRecognizer(); else {view.status="マイク権限が必要です";view.invalidate();} }
    @Override protected void onDestroy(){ if(recognizer!=null)recognizer.destroy(); if(tts!=null){tts.stop();tts.shutdown();} super.onDestroy(); }

    private class ClimateView extends View {
        Paint p=new Paint(3); int driverTemp=25,passengerTemp=25,fan=4; boolean power=true,auto=true,sync=true,recirc=true,front=false,rear=false; float micLevel=0; String status="マイクを押して話してください"; String lastCommand="例: 温度を24度にして、風量を上げて";
        String[] labs={"A/C","SYNC","↻ 内気","ECO","FRONT","REAR"};
        RectF micRect=new RectF();
        ClimateView(){ super(MainActivity.this); p.setTypeface(Typeface.create("sans",Typeface.NORMAL)); setBackgroundColor(Color.rgb(4,8,14)); }
        void text(Canvas c,String s,float x,float y,float size,int color,Paint.Align a){p.setTextSize(size);p.setColor(color);p.setTextAlign(a);p.setStyle(Paint.Style.FILL);c.drawText(s,x,y,p);}
        void panel(Canvas c,float l,float t,float r,float b,int color){p.setShader(new LinearGradient(l,t,r,b,color,Color.rgb(8,14,24),Shader.TileMode.CLAMP));p.setStyle(Paint.Style.FILL);c.drawRoundRect(l,t,r,b,12,12,p);p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1);p.setColor(Color.rgb(35,72,105));c.drawRoundRect(l,t,r,b,12,12,p);p.setStyle(Paint.Style.FILL);}
        void button(Canvas c,String label,float l,float t,float r,float b,boolean active){panel(c,l,t,r,b,active?Color.rgb(12,102,181):Color.rgb(25,31,42)); text(c,label,(l+r)/2,(t+b)/2+8,22,Color.WHITE,Paint.Align.CENTER); if(active){p.setColor(Color.rgb(20,150,255));c.drawRoundRect(l+8,t+5,r-8,t+9,4,4,p);}}
        @Override protected void onDraw(Canvas c){ super.onDraw(c); float w=getWidth(),h=getHeight(),sx=w/1280f,sy=h/720f;c.save();c.scale(sx,sy);
            p.setColor(Color.rgb(6,10,17));c.drawRect(0,0,1280,720,p); p.setColor(Color.rgb(20,101,180));c.drawRect(0,68,1280,71,p);
            text(c,"10:30   ◇  ▮▮▮",32,47,27,Color.LTGRAY,Paint.Align.LEFT);text(c,"エアコン",640,48,31,Color.WHITE,Paint.Align.CENTER);text(c,"↶ 戻る",1210,48,25,Color.LTGRAY,Paint.Align.RIGHT);
            p.setColor(Color.rgb(8,15,25));c.drawRect(0,71,175,650,p);String[] nav={"⌂\nホーム","▲\nナビ","♫\nオーディオ","✤\nエアコン","⚙\n設定"};
            for(int i=0;i<5;i++){float y=75+i*112;if(i==3){p.setShader(new LinearGradient(0,y,175,y+108,Color.rgb(0,103,195),Color.rgb(8,37,75),Shader.TileMode.CLAMP));c.drawRect(0,y,175,y+108,p);p.setShader(null);}String[] q=nav[i].split("\n");text(c,q[0],87,y+46,36,Color.WHITE,Paint.Align.CENTER);text(c,q[1],87,y+80,21,Color.WHITE,Paint.Align.CENTER);}
            text(c,"運転席",245,145,22,Color.WHITE,Paint.Align.LEFT);text(c,String.format(Locale.JAPAN,"%d.0℃",driverTemp),245,215,51,Color.WHITE,Paint.Align.LEFT);
            text(c,"助手席",1110,145,22,Color.WHITE,Paint.Align.CENTER);text(c,String.format(Locale.JAPAN,"%d.0℃",passengerTemp),1110,215,51,Color.WHITE,Paint.Align.CENTER);
            button(c,"▲",242,240,342,315,false);button(c,"▼",242,325,342,400,false);button(c,"▲",1065,240,1165,315,false);button(c,"▼",1065,325,1165,400,false);
            panel(c,390,130,1012,410,Color.rgb(12,24,38)); text(c,"❄",700,225,100,Color.rgb(30,132,235),Paint.Align.CENTER);text(c,power?"CLIMATE ON":"CLIMATE OFF",700,285,32,Color.WHITE,Paint.Align.CENTER);text(c,"運転席と助手席へ快適な風を送っています",700,330,19,Color.LTGRAY,Paint.Align.CENTER);
            text(c,"風量",320,455,22,Color.WHITE,Paint.Align.LEFT);button(c,"−",390,420,470,480,false);panel(c,476,420,825,480,Color.rgb(20,27,37));for(int i=0;i<7;i++){p.setColor(i<fan?Color.rgb(222,235,246):Color.rgb(55,63,73));c.drawRect(493+i*44,440,527+i*44,460,p);}button(c,"＋",832,420,912,480,false);button(c,"AUTO",932,420,1060,480,auto);
            boolean[] states={power,sync,recirc,false,front,rear};for(int i=0;i<6;i++)button(c,labs[i],260+i*145,500,395+i*145,575,states[i]);
            panel(c,205,595,1135,675,Color.rgb(8,19,30));text(c,"認識結果: "+lastCommand,230,622,17,Color.rgb(135,176,210),Paint.Align.LEFT);text(c,status,230,654,21,Color.WHITE,Paint.Align.LEFT);
            micRect.set(1045,588,1130,673);p.setColor(listening?Color.rgb(230,55,55):Color.rgb(0,112,209));c.drawCircle(1087,631,38+micLevel*8,p);text(c,listening?"■":"●",1087,640,27,Color.WHITE,Paint.Align.CENTER);
            text(c,"外気温   28℃",70,704,20,Color.LTGRAY,Paint.Align.LEFT);text(c,"デモ表示",1195,704,17,Color.GRAY,Paint.Align.RIGHT);c.restore(); }
        @Override public boolean onTouchEvent(android.view.MotionEvent e){ if(e.getAction()!=MotionEvent.ACTION_UP)return true;float x=e.getX()*1280/getWidth(),y=e.getY()*720/getHeight();
            if(x>1035&&x<1140&&y>580&&y<685){toggleListening();return true;}
            if(x>242&&x<342&&y>240&&y<315){changeTemp(1);respondManual("運転席の温度を上げました");}
            else if(x>242&&x<342&&y>325&&y<400){changeTemp(-1);respondManual("運転席の温度を下げました");}
            else if(x>1065&&x<1165&&y>240&&y<315){passengerTemp=Math.min(30,passengerTemp+1);respondManual("助手席の温度を上げました");}
            else if(x>1065&&x<1165&&y>325&&y<400){passengerTemp=Math.max(16,passengerTemp-1);respondManual("助手席の温度を下げました");}
            else if(x>390&&x<470&&y>420&&y<480){fan=Math.max(1,fan-1);respondManual("風量を下げました");}
            else if(x>832&&x<912&&y>420&&y<480){fan=Math.min(7,fan+1);respondManual("風量を上げました");}
            else if(x>932&&x<1060&&y>420&&y<480){auto=!auto;respondManual("オート運転を"+(auto?"開始":"解除")+"しました");}
            else if(y>500&&y<575){int i=(int)((x-260)/145);if(i>=0&&i<6){switch(i){case 0:power=!power;break;case 1:sync=!sync;if(sync)passengerTemp=driverTemp;break;case 2:recirc=!recirc;break;case 3:break;case 4:front=!front;break;case 5:rear=!rear;}respondManual(labs[i]+"を切り替えました");}}
            invalidate();return true; }
        void respondManual(String s){status=s;lastCommand="タッチ操作";invalidate();speak(s);}
    }
}
