package com.tejma.listenify.Service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotificationService extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        context.sendBroadcast(new Intent("tracks")
        .putExtra("actionName", intent.getAction()));
    }
}
