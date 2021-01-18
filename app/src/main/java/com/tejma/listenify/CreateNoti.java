package com.tejma.listenify;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.tejma.listenify.Service.NotificationService;

public class CreateNoti {

    public static final String CHANNEL_ID = "channel1";
    public  static final String ACTION_PLAY = "actionPlay";
    public  static final String CHANNEL_PAUSE = "actionpause";

    public static  Notification notification;

    public static void createNotification(Context context, String file, boolean onGoing, int playButton, Bitmap icon){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
            MediaSessionCompat mediaSessionCompat = new MediaSessionCompat(context, "tag");
            file = file.replace(".pdf", "");

            PendingIntent pendingIntent;
                Intent intentPrevious = new Intent(context, NotificationService.class)
                        .setAction(ACTION_PLAY);
                pendingIntent = PendingIntent.getBroadcast(context, 0,
                        intentPrevious, PendingIntent.FLAG_UPDATE_CURRENT);

            Intent resultIntent = new Intent(context, PdfView.class);
            resultIntent.putExtra("Book", file+".pdf");
            // Create the TaskStackBuilder and add the intent, which inflates the back stack
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
            stackBuilder.addNextIntentWithParentStack(resultIntent);
            // Get the PendingIntent containing the entire back stack
            PendingIntent resultPendingIntent =
                    stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);


            notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.logo_trans)
                    .setContentTitle(file)
                    .setLargeIcon(icon)
                    .setOnlyAlertOnce(true)
                    .setAutoCancel(true)
                    .setOngoing(onGoing)
                    .setShowWhen(false)
                    .setContentIntent(resultPendingIntent)
                    .addAction(playButton, ACTION_PLAY, pendingIntent)
                    .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0)
                        .setMediaSession(mediaSessionCompat.getSessionToken()))
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .build();


            notificationManagerCompat.notify(1, notification);

        }
    }


}
