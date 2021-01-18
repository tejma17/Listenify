package com.tejma.listenify;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements MyAdapter.onNoteListener {

    private String filepath;
    private MyAdapter recyclerViewAdapter;
    private List<File> fileArrayList;
    private List<Bitmap> thumbList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PackageManager.PERMISSION_GRANTED);

        FloatingActionButton add = findViewById(R.id.add);
        TextView title = findViewById(R.id.title);
        RecyclerView list = findViewById(R.id.list);

        ImageButton add_button = findViewById(R.id.addButton);
        add_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BottomSheetDialog builder = new BottomSheetDialog(MainActivity.this);
                builder.setContentView(R.layout.search_layout);
                Button search = builder.findViewById(R.id.search);
                EditText query = builder.findViewById(R.id.query);
                builder.show();

                search.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        searchQuery(query.getText().toString());
                        builder.dismiss();
                    }
                });

            }
        });

        fileArrayList = new ArrayList<>();
        thumbList = new ArrayList<>();
        recyclerViewAdapter = new MyAdapter(MainActivity.this, R.layout.recycler_parent, fileArrayList, thumbList, this);
        list.setAdapter(recyclerViewAdapter);
        StaggeredGridLayoutManager staggeredGridLayoutManager = new StaggeredGridLayoutManager(3, LinearLayoutManager.VERTICAL);
        list.setLayoutManager(staggeredGridLayoutManager);
        chooseFile();

        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chooseFile();
            }
        });


    }


    public void searchQuery(String queryText){
        String escapedQuery = null;
        try {
            escapedQuery = URLEncoder.encode(queryText, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Toast.makeText(MainActivity.this, "Search failed", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
        Uri uri = Uri.parse("https://www.pdfdrive.com/search?q=" + escapedQuery);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if ( v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                ((EditText) v).getText().clear();
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent( event );
    }


    @Override
    public void onNoteClick(int position, View view) {
        Intent intent = new Intent(getApplicationContext(), PdfView.class);
        intent.putExtra("Book", fileArrayList.get(position).getName());
        startActivity(intent);
    }


    @Override
    public void onNoteLongClick(int position) {

    }

    void getThumbs(){
        File path = new File(Environment.getExternalStorageDirectory()+"/Listenify/");
        File[] files = path.listFiles();
        for(int i=0; i<files.length; i++){
            Uri uri = Uri.fromFile(files[i]);
            thumbList.add(generateImageFromPdf(uri));
        }
    }

    private void chooseFile() {
        fileArrayList.clear();
        List<File> tempList;
        LinearLayout nothing = findViewById(R.id.nothing);
        File path = new File(Environment.getExternalStorageDirectory()+"/Listenify/");

        if(!path.exists()){
            nothing.animate().alpha(1.0f).setDuration(500);
        }else{
            nothing.setAlpha(0f);
            File[] files = path.listFiles();

            tempList = Arrays.asList(files);
            fileArrayList.addAll(tempList);
            getThumbs();
            recyclerViewAdapter.notifyDataSetChanged();

            if(fileArrayList.size()==0)
                nothing.animate().alpha(1.0f).setDuration(500);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_navigation, menu);

        //searchView
        MenuItem item = menu.findItem(R.id.speed);
        item.setVisible(false);
        return true;
    }


    Bitmap generateImageFromPdf(Uri pdfUri) {
        int pageNumber = 0;
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.pdf);;
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
        return bmp;
    }
}