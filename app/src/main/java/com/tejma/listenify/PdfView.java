package com.tejma.listenify;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.listener.OnTapListener;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;
import com.tejma.listenify.Service.OnClearFromRecentService;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class PdfView extends AppCompatActivity implements OnPageChangeListener, OnLoadCompleteListener, OnTapListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private PDFView pdfView;
    private TextView PN, action_name;
    private MaterialToolbar toolbar;
    private MaterialButton back_button;
    private boolean visible = true, paused, firstTime;
    private TextToSpeech textToSpeech;
    private Integer pageNumber = 0, counter = 0;
    private int shortAnimationDuration;
    private float playBackSpeed = 1.0f;
    private String name;
    Bundle params = new Bundle();
    private Bitmap bmp;
    private String[] sents;
    private NotificationManager notificationManager;
    private FloatingActionButton play, start;
    private int runningPage, totalPages;
    private String values;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_view);

        pdfView = findViewById(R.id.pdfView);
        play = findViewById(R.id.play);
        start = findViewById(R.id.start);
        PN = findViewById(R.id.count);
        toolbar = findViewById(R.id.toolbar);
        action_name = toolbar.findViewById(R.id.name);
        back_button = toolbar.findViewById(R.id.back);
        MaterialButton item = findViewById(R.id.speed);


        back_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });


        item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dialog builder = new Dialog(PdfView.this);
                builder.setContentView(R.layout.speed_layout);
                SeekBar seekBar = builder.findViewById(R.id.seek);
                Button button = builder.findViewById(R.id.set);
                TextView currSpeed = builder.findViewById(R.id.currspeed);

                int curr = (int) (playBackSpeed*10);
                String text = playBackSpeed+" X";
                seekBar.setProgress(curr);
                currSpeed.setText(text);


                seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        String spd = ((float)progress)/10+" X";
                        currSpeed.setText(spd);
                        Log.i(TAG, playBackSpeed+"");
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        Log.i(TAG, playBackSpeed+"");
                    }
                });


                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        playBackSpeed = ((float)seekBar.getProgress())/10;
                        textToSpeech.setSpeechRate(playBackSpeed);
                        builder.dismiss();
                    }
                });
                builder.show();
            }
        });

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O) {
            createChannel();
            registerReceiver(broadcastReceiver, new IntentFilter("tracks"));
            startService(new Intent(getBaseContext(), OnClearFromRecentService.class));
        }else{
            Toast.makeText(this, "Requires Android Oreo+", Toast.LENGTH_SHORT).show();
        }

        shortAnimationDuration = getResources().getInteger(
                android.R.integer.config_shortAnimTime);
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "IDS");

        name = getIntent().getStringExtra("Book");
        action_name.setText(name.replace(".pdf",""));

        Uri uri = Uri.fromFile(new File(Environment.getExternalStorageDirectory()+"/Listenify/"+name));
        new Thread(new Runnable() {
            @Override
            public void run() {
                generateImageFromPdf(uri);
            }
        }).start();


        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                textToSpeech.setLanguage(Locale.US);
                Set<String> a=new HashSet<>();
                a.add("networkTimeoutMs");
                a.add("networkRetriesCount");
                Voice v=new Voice("en-in-x-ene-network",new Locale("en","IN"),400,200,true,  a);
                textToSpeech.setVoice(v);
                textToSpeech.setSpeechRate(1.0f);
                textToSpeech.setPitch(0f);
            }
        });

        if(textToSpeech.isSpeaking()){
            firstTime = false;
            start.setImageResource(R.drawable.ic_round_stop_24);
            play.setImageResource(R.drawable.ic_round_pause_24);
            play.animate().alpha(1f).setDuration(1000);
        } else{
            start.setImageResource(R.drawable.ic_round_not_started_24);
            play.setImageResource(R.drawable.ic_round_play_arrow_24);
            play.animate().alpha(0f).setDuration(1000);
            firstTime = true;
        }

        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {

            }

            @Override
            public void onDone(String utteranceId) {
                counter++;
                String progress = runningPage+"TTT"+counter;
                sharedPreferences.edit().putString(name, progress).apply();
                Log.i(TAG, "Sents: "+sents.length+" Counter: "+counter+" running: "+runningPage+" total: "+totalPages);
                if(counter<sents.length)
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            textToSpeech.speak(sents[counter], TextToSpeech.QUEUE_FLUSH, params, "IDS");
                        }
                    }).start();
                else if(runningPage<totalPages){
                    getThingsReady(++runningPage, 0);
                }
                else {
                    onStopped();
                }
            }

            @Override
            public void onError(String utteranceId) {

            }
        });

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(firstTime){
                    if(values.equals(0+"TTT"+0)) {
                        runningPage = pageNumber;
                        getThingsReady(runningPage, 0);
                    }else{
                        String info[] = values.split("TTT");
                        getThingsReady(Integer.parseInt(info[0]), Integer.parseInt(info[1]));
                    }
                }else{
                    onStopped();
                }
            }
        });


        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (paused) {
                    getThingsReady(runningPage, counter);
                } else {
                    afterPaused();
                }
            }
        });

    }

    @Override
    public void onUserInteraction(){
        if(visible){
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    PN.animate()
                            .alpha(0f)
                            .setDuration(shortAnimationDuration)
                            .setListener(null);
                    visible = false;
                }
            }, 5000);
        }
    }

    private void displayFromAsset(String fileName) {
        File file = new File(Environment.getExternalStorageDirectory()+"/Listenify/"+fileName);
        pdfView.fromFile(file)
                .defaultPage(runningPage)
                .enableSwipe(true)
                .swipeHorizontal(false)
                .onPageChange(this)
                .spacing(5)
                .onTap(this)
                .enableDoubletap(true)
                .enableAnnotationRendering(true)
                .onLoad(this)
                .scrollHandle(new DefaultScrollHandle(this))
                .load();
        totalPages = pdfView.getPageCount();
    }

    @Override
    public void onPageChanged(int page, int pageCount) {
        pageNumber = page;
        PN.setText(String.format("%s / %s", page + 1, pageCount));
    }

    @Override
    public void loadComplete(int nbPages) {
        PdfDocument.Meta meta = pdfView.getDocumentMeta();
        printBookmarksTree(pdfView.getTableOfContents(), "-");

    }

    public void printBookmarksTree(List<PdfDocument.Bookmark> tree, String sep) {
        for (PdfDocument.Bookmark b : tree) {

            Log.e(TAG, String.format("%s %s, p %d", sep, b.getTitle(), b.getPageIdx()));

            if (b.hasChildren()) {
                printBookmarksTree(b.getChildren(), sep + "-");
            }
        }
    }

    @Override
    public boolean onTap(MotionEvent e) {
        if(visible){
            PN.animate()
                    .alpha(0f)
                    .setDuration(shortAnimationDuration)
                    .setListener(null);
            visible = false;
        }else{
            PN.animate()
                    .alpha(1f)
                    .setDuration(shortAnimationDuration)
                    .setListener(null);
            visible = true;
        }

        return false;
    }


    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getExtras().getString("actionName");
            if(paused) {
                afterResumed();
            }else{
               afterPaused();
            }
        }
    };


    @Override
    public void onBackPressed() {
        String progress = runningPage+"TTT"+counter;
        sharedPreferences.edit().putString(name, progress).apply();
        Toast.makeText(this, "Progress Saved for "+name.replace(".pdf",""), Toast.LENGTH_SHORT).show();
        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        sharedPreferences = getSharedPreferences("BOOKS", MODE_PRIVATE);
        values = sharedPreferences.getString(name, 0+"TTT"+0);
        if(!values.equals(0+"TTT"+0)){
            start.setImageResource(R.drawable.ic_round_stop_24);
            play.animate().alpha(1f).setDuration(500);
            firstTime = false;
            paused = true;
        }
        counter = Integer.parseInt(values.split("TTT")[1]);
        runningPage = Integer.parseInt(values.split("TTT")[0]);
        displayFromAsset(name);
        //Toast.makeText(this, sharedPreferences.getString(name, "AAA"), Toast.LENGTH_SHORT).show();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        textToSpeech.stop();
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O) {
            notificationManager.cancelAll();
            unregisterReceiver(broadcastReceiver);
        }
    }


    public void onStarted(){
        start.setImageResource(R.drawable.ic_round_stop_24);
        play.animate().alpha(1f).setDuration(500);
        firstTime = false;
        afterResumed();
    }

    public void onStopped(){
        start.setImageResource(R.drawable.ic_round_not_started_24);
        play.setImageResource(R.drawable.ic_round_play_arrow_24);
        textToSpeech.stop();
        paused = true;
        play.animate().alpha(0f).setDuration(500);
        firstTime = true;
        counter = 0;
        runningPage = 0;
        sharedPreferences.edit().putString(name, runningPage+"TTT"+counter).apply();
        notificationManager.cancelAll();
    }

    public void afterResumed(){
        CreateNoti.createNotification(PdfView.this, name, true, R.drawable.ic_round_pause_24, bmp);
        play.setImageResource(R.drawable.ic_round_pause_24);
        textToSpeech.speak(sents[counter], TextToSpeech.QUEUE_FLUSH, params, "IDS");
        paused = false;
    }



    public void afterPaused(){
        CreateNoti.createNotification(PdfView.this, name, false, R.drawable.ic_round_play_arrow_24, bmp);
        play.setImageResource(R.drawable.ic_round_play_arrow_24);
        paused = true;
        textToSpeech.stop();
    }



    public void getThingsReady(int pageN, int count){
        try {
            counter = count;
            PdfReader pdfReader = new PdfReader(Environment.getExternalStorageDirectory()+"/Listenify/"+name);
            totalPages = pdfReader.getNumberOfPages();
            String text = PdfTextExtractor.getTextFromPage(pdfReader, pageN+1);
            sents = null;
            sents = text.split("\\.");
            if(counter>=sents.length){
               getThingsReady(++runningPage, 0);
            }
            pdfReader.close();
            onStarted();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createChannel(){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            NotificationChannel notificationChannel = new NotificationChannel(CreateNoti.CHANNEL_ID, "TEJMA",
                    NotificationManager.IMPORTANCE_LOW);
            notificationManager = getSystemService(NotificationManager.class);
            if(notificationManager!=null){
                notificationManager.createNotificationChannel(notificationChannel);

            }
        }
    }

    void generateImageFromPdf(Uri pdfUri) {
        int pageNumber = 0;
        PdfiumCore pdfiumCore = new PdfiumCore(this);
        try {
            ParcelFileDescriptor fd = getContentResolver().openFileDescriptor(pdfUri, "r");
            PdfDocument pdfDocument = pdfiumCore.newDocument(fd);
            pdfiumCore.openPage(pdfDocument, pageNumber);
            int width = pdfiumCore.getPageWidthPoint(pdfDocument, pageNumber);
            int height = pdfiumCore.getPageHeightPoint(pdfDocument, pageNumber);
            bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            pdfiumCore.renderPageBitmap(pdfDocument, bmp, pageNumber, 0, 0, width, height);
            pdfiumCore.closeDocument(pdfDocument); // important!
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_navigation, menu);

        //searchView
        MenuItem item = menu.findItem(R.id.speed);

        return true;
    }
}

