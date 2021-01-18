package com.tejma.listenify;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder>{

    String TAG = "ADAPTER";
    List<File> objects, query_list;
    List<Bitmap> thumbs;
    Context context;
    int res;
    private final onNoteListener mOnNoteListener;
    LayoutInflater inflater;

    public MyAdapter(@NonNull Context context, int resource, List<File> objects, List<Bitmap> thumbs, onNoteListener mOnNoteListener) {
        this.context = context;
        this.objects = new ArrayList<>();
        this.objects = objects;
        this.thumbs = new ArrayList<>();
        this.thumbs = thumbs;
        res = resource;
        inflater = LayoutInflater.from(context);
        this.mOnNoteListener = mOnNoteListener;
        query_list = new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(res, parent, false);
        query_list.clear();
        query_list.addAll(objects);
        return new ViewHolder(view, mOnNoteListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        holder.title.setText(objects.get(position).getName().replace(".pdf", ""));
        holder.thumbnail.setImageBitmap(thumbs.get(position));
    }

    @Override
    public int getItemCount() {
        return objects.size();
    }

    public void filter(String query){
        query = query.toLowerCase().trim();
        objects.clear();
        if(query.length() == 0)
        {
            objects.addAll(query_list);
        }
        else
        {
            for(File chapter : query_list){
                if(chapter.getName().toLowerCase().contains(query)){
                    objects.add(chapter);
                }
            }
        }
        notifyDataSetChanged();
    }



    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        TextView title;
        ImageView thumbnail;
        LinearLayout parent;
        onNoteListener noteListener;

         public ViewHolder(View itemView, onNoteListener onNoteListener){
             super(itemView);
             this.title = itemView.findViewById(R.id.list_text);
             this.thumbnail = itemView.findViewById(R.id.thumbnail);
             this.noteListener = onNoteListener;
             itemView.setOnClickListener(this);
             itemView.setOnLongClickListener(this);
             this.parent = (LinearLayout) itemView.findViewById(R.id.parent_layout);
         }

        @Override
        public void onClick(View v) {
            noteListener.onNoteClick(getLayoutPosition(), v);
        }

        @Override
        public boolean onLongClick(View view) {
            noteListener.onNoteLongClick(getLayoutPosition());
            return true;
        }
    }

    public interface onNoteListener{
        void onNoteClick(int position, View v);
        void onNoteLongClick(int position);
    }
}
