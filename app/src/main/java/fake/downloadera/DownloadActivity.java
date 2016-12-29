package fake.downloadera;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;


public class DownloadActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private TextView statusText;
    private ImageView imageView;
    private Button actionButton;

    private String PATH_KEY = "path";

    private DownloadManager downloadManager;
    private String downloadFileUrl = "https://pp.vk.me/c626716/v626716071/48300/ZC9iY1csIro.jpg";
    private long myDownloadReference = -1;
    private BroadcastReceiver receiverDownloadComplete;
    private BroadcastReceiver receiverNotificationClicked;

    private String savedPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_image);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        statusText = (TextView) findViewById(R.id.status_text);
        imageView = (ImageView) findViewById(R.id.image);
        actionButton = (Button) findViewById(R.id.action_button);

        if (savedInstanceState == null) {
            savedPath = "/storage/emulated/0/Android/data/fake.downloadera/files/Download/logo.jpg";
            File file = new File(savedPath);
            if (file.exists()) {
                statusText.setText(getResources().getText(R.string.status_Downloaded));
                Bitmap bitmap = BitmapFactory.decodeFile(savedPath);
                imageView.setImageBitmap(bitmap);
                actionButton.setText(getResources().getText(R.string.action_button_Open));
                actionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        openAction();
                    }
                });
            } else if (!isInternetConnected())
                Toast.makeText(this, "Internet is disconnected", Toast.LENGTH_LONG).show();
            actionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    downloadAction();
                }
            });
        } else {
            savedPath = savedInstanceState.getString(PATH_KEY);
            if (savedPath != null) {
                //savedPath = "/storage/emulated/0/Android/data/fake.downloadera/files/Download/logo.jpg";
                File file = new File(savedPath);
                if (file.exists()) {
                    statusText.setText(getResources().getText(R.string.status_Downloaded));
                    Bitmap bitmap = BitmapFactory.decodeFile(savedPath);
                    imageView.setImageBitmap(bitmap);
                    actionButton.setText(getResources().getText(R.string.action_button_Open));
                    actionButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            openAction();
                        }
                    });
                } else {
                    if (isInternetConnected()) {
                        actionButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                downloadAction();
                            }
                        });
                    } else
                        Toast.makeText(this, "Internet is disconnected", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private boolean isInternetConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null;
    }

    private void downloadAction() {
        if (isInternetConnected()) {
            progressBar.setVisibility(View.VISIBLE);
            statusText.setText(getResources().getText(R.string.status_Downloading));
            actionButton.setEnabled(false);

            downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

            Uri uri = Uri.parse(downloadFileUrl);
            DownloadManager.Request request = new DownloadManager.Request(uri);
            request.setDescription(getString(R.string.request_Description))
                    .setTitle(getString(R.string.request_Title));
            request.setDestinationInExternalFilesDir(DownloadActivity.this, Environment.DIRECTORY_DOWNLOADS, "logo.jpg");
            request.setVisibleInDownloadsUi(true);
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);

            myDownloadReference = downloadManager.enqueue(request);

            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    DownloadManager.Query q = new DownloadManager.Query();
                    q.setFilterById(myDownloadReference);
                    Cursor cursor = downloadManager.query(q);
                    cursor.moveToFirst();
                    int bytes_downloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                    int bytes_total = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                    cursor.close();
                    final int dl_progress = (bytes_downloaded * 100 / bytes_total);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setProgress(dl_progress);
                        }
                    });

                }
            }, 0, 10);
        } else Toast.makeText(this, "Internet is disconnected", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (savedPath != null) {
            File file = new File(savedPath);
            if (!file.exists()) {
                imageView.setImageBitmap(null);
                statusText.setText(getResources().getText(R.string.status_Idle));
                actionButton.setText(getResources().getText(R.string.action_button_Download));
                actionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        downloadAction();
                    }
                });
            } else {
                statusText.setText(getResources().getText(R.string.status_Downloaded));
                Bitmap bitmap = BitmapFactory.decodeFile(savedPath);
                imageView.setImageBitmap(bitmap);
                actionButton.setText(getResources().getText(R.string.action_button_Open));
                actionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        openAction();
                    }
                });
            }
        }
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED);
        receiverNotificationClicked = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String extraId = DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS;
                long[] references = intent.getLongArrayExtra(extraId);
                for (long reference : references) {
                    if (reference == myDownloadReference) {
                        //do something with the download file
                    }
                }
            }
        };
        registerReceiver(receiverNotificationClicked, filter);

        IntentFilter intentFilter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        receiverDownloadComplete = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long reference = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (myDownloadReference == reference) {
                    DownloadManager.Query query = new DownloadManager.Query();
                    query.setFilterById(reference);
                    Cursor cursor = downloadManager.query(query);
                    cursor.moveToFirst();
                    int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    int status = cursor.getInt(columnIndex);
                    int fileNameIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME);

                    int columnReason = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
                    int reason = cursor.getInt(columnReason);
                    switch (status) {
                        case DownloadManager.STATUS_SUCCESSFUL:
                            savedPath = cursor.getString(fileNameIndex);
                            progressBar.setVisibility(View.INVISIBLE);
                            Bitmap bitmap = BitmapFactory.decodeFile(savedPath);
                            imageView.setImageBitmap(bitmap);

                            statusText.setText(getResources().getText(R.string.status_Downloaded));
                            actionButton.setText(getResources().getText(R.string.action_button_Open));
                            actionButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    openAction();
                                }
                            });
                            actionButton.setEnabled(true);
                            break;
                        case DownloadManager.STATUS_FAILED:
                            break;
                        case DownloadManager.STATUS_PAUSED://обработать
                            break;
                        case DownloadManager.STATUS_PENDING:
                            break;
                        case DownloadManager.STATUS_RUNNING:
                            statusText.setText(R.string.status_Downloading);
                            break;
                    }
                    cursor.close();
                }
            }
        };
        registerReceiver(receiverDownloadComplete, intentFilter);
    }

    private void openAction() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse("file://" + savedPath), "image/*");
        startActivity(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiverDownloadComplete);
        unregisterReceiver(receiverNotificationClicked);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(PATH_KEY, savedPath);
    }
}



