package com.lpg.ciarnurnotimer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.ValueCallback;

import java.util.Timer;
import java.util.TimerTask;

import im.delight.android.webview.AdvancedWebView;

public class MainActivity extends Activity implements AdvancedWebView.Listener {

    private final int ACTIVITY_LAYOUT = R.layout.activity_main;
    private final int TIMER_PERIOD = 500;

    private boolean lastStatus = false; //last value of js variable leaveConfirmRequired

    private AdvancedWebView mWebView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(ACTIVITY_LAYOUT);

        createNotificationChannel();

        mWebView = (AdvancedWebView) findViewById(R.id.webview);

        mWebView.setListener(this, this);
        mWebView.addPermittedHostname("");

        mWebView.loadUrl("file:///android_asset/index.html");

        Timer persistenceTimer = new Timer();
        persistenceTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mWebView.evaluateJavascript("leaveConfirmRequired", new ValueCallback<String>() {
                            @Override
                            public void onReceiveValue(String s) {
                                if (s.equalsIgnoreCase("true")) {
                                    if (!lastStatus) {
                                        //Start foreground service
                                        startForegroundService();

                                        lastStatus = true;
                                    }
                                } else {
                                    if (lastStatus) {
                                        //Stop foreground service
                                        stopForegroundService();

                                        lastStatus = false;
                                    }
                                }
                            }
                        });
                    }
                });
            }
        }, 1000, TIMER_PERIOD);
    }

    @SuppressLint("NewApi")
    @Override
    protected void onResume() {
        super.onResume();
        mWebView.onResume();
    }

    @SuppressLint("NewApi")
    @Override
    protected void onPause() {
        mWebView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mWebView.onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        mWebView.onActivityResult(requestCode, resultCode, intent);
    }

    @Override
    public void onBackPressed() {
        if (!mWebView.onBackPressed()) {
            return;
        }

        mWebView.evaluateJavascript("leaveConfirmRequired", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String s) {
                if (s.equalsIgnoreCase("true")) {
                    //Confirm exit the application
                    showExitDialog();
                } else {
                    System.exit(0);
                }
            }
        });


        //Prevents closing the app by mistake
        //super.onBackPressed();
    }

    @Override
    public void onPageStarted(String url, Bitmap favicon) {
    }

    @Override
    public void onPageFinished(String url) {
    }

    @Override
    public void onPageError(int errorCode, String description, String failingUrl) {
    }

    @Override
    public void onDownloadRequested(String url, String suggestedFilename, String mimeType, long contentLength, String contentDisposition, String userAgent) {
    }

    @Override
    public void onExternalPageRequest(String url) {
        //Opens the external url in the browser
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }


    // Misc methods
    private void showExitDialog() {
        AlertDialog.Builder exitDialog = new AlertDialog.Builder(this);
        exitDialog.setTitle("Sei sicuro di voler uscire dall'applicazione?");
        exitDialog.setMessage("Se uscirai annullerai la partita in corso!");

        exitDialog.setPositiveButton("Esci", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                System.exit(0);
            }
        });

        // A null listener allows the button to dismiss the dialog and take no further action.
        exitDialog.setNegativeButton("Annulla", null);
        AlertDialog alert = exitDialog.create();
        alert.show();
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= 26) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel("1", name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void startForegroundService() {
        Intent intent = new Intent(MainActivity.this, DozerCancer.class);
        intent.setAction(DozerCancer.ACTION_START_FOREGROUND_SERVICE);
        startService(intent);
    }

    private void stopForegroundService() {
        Intent intent = new Intent(MainActivity.this, DozerCancer.class);
        intent.setAction(DozerCancer.ACTION_STOP_FOREGROUND_SERVICE);
        startService(intent);
    }

}
