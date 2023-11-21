package com.mingzz__.h26x.projection;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.mingzz__.a2022h26x.R;
import com.mingzz__.h26x.code_play.PlayActivity;
import com.mingzz__.util.L;

public class ProjectionService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        L.i("ProjectionService onBind");
        createNotificationChannel();
        int resultCode = intent.getIntExtra("code", -1);
        Intent data = intent.getParcelableExtra("intent");
        L.i("resultCode-->" + resultCode + " data-->" + data);
        mediaProjection = ((MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE))
                .getMediaProjection(resultCode, data);
        L.i("ProjectionService get mediaProjection-->" + mediaProjection);
        return new ProjectionCallback() {
            @Override
            public MediaProjection getProjection() {
                return mediaProjection;
            }
        };
    }


    @Override
    public void onCreate() {
        L.i("ProjectionService onCreate");
        super.onCreate();
    }

    MediaProjection mediaProjection;


    @Override
    public void onDestroy() {
        mediaProjection = null;
        super.onDestroy();
    }

    private void createNotificationChannel() {
        Notification.Builder builder = new Notification.Builder(this.getApplicationContext()); //获取一个Notification构造器
        Intent nfIntent = new Intent(this, PlayActivity.class); //点击后跳转的界面，可以设置跳转数据

        builder.setContentIntent(PendingIntent.getActivity(this, 1991, nfIntent, PendingIntent.FLAG_IMMUTABLE)) // 设置PendingIntent
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launcher)) // 设置下拉列表中的图标(大图标)
                .setContentTitle("Projection Notification") // 设置下拉列表里的标题
                .setSmallIcon(R.mipmap.ic_launcher) // 设置状态栏内的小图标
                .setContentText("is running......") // 设置上下文内容
                .setWhen(System.currentTimeMillis()); // 设置该通知发生的时间

        /*以下是对Android 8.0的适配*/
        //普通notification适配
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId("projection request");
        }
        //前台服务notification适配
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel("projection request", "projection name",
                    NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }

        Notification notification = builder.build(); // 获取构建好的Notification
        notification.defaults = Notification.DEFAULT_SOUND; //设置为默认的声音
        startForeground(110, notification);
    }
}
