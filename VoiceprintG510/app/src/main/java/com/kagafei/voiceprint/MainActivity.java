package com.kagafei.voiceprint;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int SAMPLE_RATE = 16000;
    private static final int RECORD_SECONDS = 4;
    private static final int FEATURE_SIZE = 34;
    private static final float DEFAULT_THRESHOLD = 0.86f;

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private TextView status, score, enrollmentCount;
    private ProgressBar progress;
    private Button enrollButton, verifyButton, resetButton;
    private final List<float[]> enrollmentSamples = new ArrayList<>();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
        loadTemplate();
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 10);
        }
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL); root.setGravity(Gravity.CENTER);
        root.setPadding(48, 28, 48, 28); root.setBackgroundColor(Color.rgb(7,21,46));

        TextView title = text("声紋認識デモ", 34, Color.WHITE); title.setGravity(Gravity.CENTER);
        TextView sub = text("G510 / Android オフライン話者照合", 17, Color.rgb(151,188,255)); sub.setGravity(Gravity.CENTER);
        status = text("マイク権限を確認しています", 22, Color.WHITE); status.setGravity(Gravity.CENTER); status.setPadding(0,28,0,16);
        score = text("一致度: --", 30, Color.rgb(80,220,160)); score.setGravity(Gravity.CENTER);
        enrollmentCount = text("登録サンプル: 0 / 3", 16, Color.LTGRAY); enrollmentCount.setGravity(Gravity.CENTER);
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100); progress.setProgress(0);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(-1, 22); pp.setMargins(80,20,80,24); progress.setLayoutParams(pp);

        LinearLayout buttons = new LinearLayout(this); buttons.setGravity(Gravity.CENTER);
        enrollButton = button("声を登録", Color.rgb(31,102,230));
        verifyButton = button("本人確認", Color.rgb(9,153,111));
        resetButton = button("登録消去", Color.rgb(110,120,140));
        buttons.addView(enrollButton); buttons.addView(verifyButton); buttons.addView(resetButton);
        enrollButton.setOnClickListener(v -> capture(true));
        verifyButton.setOnClickListener(v -> capture(false));
        resetButton.setOnClickListener(v -> resetTemplate());

        TextView guide = text("静かな場所で端末から30〜50cm離れ、自然な声で4秒間話してください。\n登録は3回行います。PoC用の話者照合であり、単独で重要な本人認証には使用しないでください。", 15, Color.rgb(190,200,218));
        guide.setGravity(Gravity.CENTER); guide.setPadding(0,24,0,0);
        root.addView(title); root.addView(sub); root.addView(status); root.addView(score); root.addView(enrollmentCount); root.addView(progress); root.addView(buttons); root.addView(guide);
        setContentView(root);
    }

    private TextView text(String s, int sp, int color) { TextView v=new TextView(this); v.setText(s); v.setTextSize(sp); v.setTextColor(color); return v; }
    private Button button(String s, int color) {
        Button b=new Button(this); b.setText(s); b.setTextSize(17); b.setTextColor(Color.WHITE); b.setBackgroundColor(color);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(240,82); p.setMargins(12,0,12,0); b.setLayoutParams(p); return b;
    }

    private void capture(boolean enroll) {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 10); return;
        }
        if (!enroll && readTemplate() == null) { toast("先に声を3回登録してください"); return; }
        setBusy(true); status.setText(enroll ? "登録音声を録音中…" : "照合音声を録音中…"); score.setText("一致度: --");
        worker.execute(() -> {
            try {
                short[] pcm = recordPcm();
                float[] feature = extractFeatures(pcm);
                ui.post(() -> finishCapture(enroll, feature));
            } catch (Exception e) {
                ui.post(() -> { setBusy(false); status.setText("録音エラー: " + e.getMessage()); });
            }
        });
    }

    private short[] recordPcm() throws Exception {
        int count = SAMPLE_RATE * RECORD_SECONDS;
        int min = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        AudioRecord rec = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(min, 4096));
        if (rec.getState() != AudioRecord.STATE_INITIALIZED) throw new Exception("AudioRecord初期化失敗");
        short[] out = new short[count]; int pos=0; rec.startRecording();
        while (pos < count) {
            int n=rec.read(out,pos,Math.min(2048,count-pos)); if(n<0){rec.stop();rec.release();throw new Exception("read="+n);} pos+=n;
            int value=(int)(100.0*pos/count); ui.post(() -> progress.setProgress(value));
        }
        rec.stop(); rec.release(); return out;
    }

    // 軽量PoC: RMS/ZCRと32帯域のGoertzel対数エネルギーを声紋ベクトル化。
    private float[] extractFeatures(short[] x) throws Exception {
        double rms=0; int zc=0;
        for(int i=1;i<x.length;i++){ double v=x[i]/32768.0; rms+=v*v; if((x[i]>=0)!=(x[i-1]>=0))zc++; }
        rms=Math.sqrt(rms/x.length); if(rms<0.012) throw new Exception("声が小さすぎます");
        float[] f=new float[FEATURE_SIZE]; f[0]=(float)Math.log(rms+1e-8); f[1]=(float)zc/x.length;
        for(int b=0;b<32;b++){
            double hz=120.0*Math.pow(3800.0/120.0,b/31.0); double w=2*Math.PI*hz/SAMPLE_RATE;
            double coeff=2*Math.cos(w), s0=0,s1=0,s2=0;
            int step=2; for(int i=0;i<x.length;i+=step){s0=x[i]/32768.0+coeff*s1-s2;s2=s1;s1=s0;}
            double p=s1*s1+s2*s2-coeff*s1*s2; f[b+2]=(float)Math.log(Math.max(p,1e-10));
        }
        normalize(f); return f;
    }

    private void normalize(float[] v){ float mean=0; for(float x:v)mean+=x; mean/=v.length; float n=0; for(int i=0;i<v.length;i++){v[i]-=mean;n+=v[i]*v[i];} n=(float)Math.sqrt(n); if(n>0)for(int i=0;i<v.length;i++)v[i]/=n; }
    private float cosine(float[] a,float[] b){float s=0;for(int i=0;i<a.length;i++)s+=a[i]*b[i];return s;}

    private void finishCapture(boolean enroll,float[] feature){
        setBusy(false); progress.setProgress(100);
        if(enroll){
            enrollmentSamples.add(feature); enrollmentCount.setText("登録サンプル: "+enrollmentSamples.size()+" / 3");
            if(enrollmentSamples.size()>=3){
                float[] avg=new float[FEATURE_SIZE]; for(float[] v:enrollmentSamples)for(int i=0;i<avg.length;i++)avg[i]+=v[i]/enrollmentSamples.size(); normalize(avg); saveTemplate(avg); enrollmentSamples.clear();
                status.setText("声紋登録が完了しました"); enrollmentCount.setText("登録済み");
            } else status.setText("登録しました。あと"+(3-enrollmentSamples.size())+"回話してください");
        } else {
            float sim=cosine(feature,readTemplate()); boolean ok=sim>=DEFAULT_THRESHOLD;
            score.setText(String.format(Locale.JAPAN,"一致度: %.1f%%",sim*100f)); score.setTextColor(ok?Color.rgb(80,220,160):Color.rgb(255,110,100));
            status.setText(ok?"本人と判定しました":"登録者と一致しません");
        }
    }

    private void saveTemplate(float[] v){StringBuilder s=new StringBuilder();for(int i=0;i<v.length;i++){if(i>0)s.append(',');s.append(v[i]);}getPreferences(MODE_PRIVATE).edit().putString("voiceprint",s.toString()).apply();}
    private float[] readTemplate(){String s=getPreferences(MODE_PRIVATE).getString("voiceprint",null);if(s==null)return null;String[] p=s.split(",");if(p.length!=FEATURE_SIZE)return null;float[] v=new float[p.length];try{for(int i=0;i<p.length;i++)v[i]=Float.parseFloat(p[i]);return v;}catch(Exception e){return null;}}
    private void loadTemplate(){boolean exists=readTemplate()!=null;status.setText(exists?"登録済みです。本人確認できます":"未登録です。声を3回登録してください");enrollmentCount.setText(exists?"登録済み":"登録サンプル: 0 / 3");}
    private void resetTemplate(){getPreferences(MODE_PRIVATE).edit().clear().apply();enrollmentSamples.clear();score.setText("一致度: --");loadTemplate();toast("登録データを消去しました");}
    private void setBusy(boolean b){enrollButton.setEnabled(!b);verifyButton.setEnabled(!b);resetButton.setEnabled(!b);if(b)progress.setProgress(0);}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    @Override protected void onDestroy(){worker.shutdownNow();super.onDestroy();}
}
