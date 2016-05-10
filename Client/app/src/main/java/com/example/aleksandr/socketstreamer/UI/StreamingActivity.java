package com.example.aleksandr.socketstreamer.UI;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dd.morphingbutton.MorphingButton;
import com.example.aleksandr.socketstreamer.supporting.CameraManager;
import com.example.aleksandr.socketstreamer.supporting.CameraPreview;
import com.example.aleksandr.socketstreamer.R;
import com.example.aleksandr.socketstreamer.data.Listeners.ErrorListener;
import com.example.aleksandr.socketstreamer.io.Client;

/**
 * Активити для проведения трансляций
 */
public class StreamingActivity extends Activity implements ErrorListener {
    // Parts
    private Client client;


    // Parameters
    private static final float LOCATION_REFRESH_DISTANCE = 5;
    private static final long LOCATION_REFRESH_TIME = 3000;
    private final String DEFAULT_IP = "77.94.175.81";
    private final int DEFAULT_PORT = 8585;

    // Ui
    TextView lbl_port; // текст-порт
    TextView lbl_ip; // текст-ip
    TextView lbl_location; // текст-длкация
    MorphingButton btnStart; // кнопка-старт/стоп

    // Objects
    private CameraPreview cameraPreview;
    private CameraManager cameraManager;
    private Context context;
    private LocationManager locationManager;
    private MorphingOperations morphingOperations;

    // Indicators-local values
    private boolean streamStatus = false;
    private String ip = DEFAULT_IP;
    private int port = DEFAULT_PORT;
    private StreamFactory streamFactory = new StreamFactory();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stream);

        // контекст программы
        context = this;

        streamFactory.createCameraPreview();

        final FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(cameraPreview);

        // Установка ip
        lbl_ip = (TextView) findViewById(R.id.lbl_ip);
        setIp(DEFAULT_IP);
        // Установка port
        lbl_port = (TextView) findViewById(R.id.lbl_port);
        setPort(DEFAULT_PORT);

        // Установка локации
        lbl_location = (TextView) findViewById(R.id.lbl_location);

        // Morphing button
        btnStart = (MorphingButton) findViewById(R.id.button_capture);

        morphingOperations = new MorphingOperations();

        final StreamingActivity parent = this;
        // настройка кнопки
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Выключение трансляции
                if (streamStatus) {
                    btnStart.morph(morphingOperations.off);

                    // выключение трансляции
                    client.close();
                    streamStatus = false;
                }
                // Запуск трансляции
                else {
                    btnStart.morph(morphingOperations.on);

                    // создание сокет клиента для отправки
                    streamFactory.createStream(parent);
                    client.start();

                    streamStatus = true;
                }
            }
        });


        // изначально выключен
        btnStart.morph(morphingOperations.off);
    }

    class MorphingOperations{
        public final MorphingButton.Params off = MorphingButton.Params.create()
                .duration(500)
                .cornerRadius((int) getResources().getDimension(R.dimen.btn_10dp))
                .width((int) getResources().getDimension(R.dimen.btn_200p))
                .height((int) getResources().getDimension(R.dimen.btn_56dp))
                .color(R.color.mb_blue)
                .colorPressed(R.color.mb_blue_dark)
                .text(getResources().getString(R.string.start));
        public final MorphingButton.Params on = MorphingButton.Params.create()
                .duration(500)
                .cornerRadius((int) getResources().getDimension(R.dimen.btn_56dp)) // 56 dp
                .width((int) getResources().getDimension(R.dimen.btn_56dp)) // 56 dp
                .height((int) getResources().getDimension(R.dimen.btn_56dp)) // 56 dp
                .color(R.color.mb_blue) // normal state color
                .colorPressed(R.color.mb_blue_dark) // pressed state color
                .icon(R.drawable.stop); // icon
    }

    @Override
    public void onError(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void offButton(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnStart.morph(morphingOperations.off);
                streamStatus = false;
            }
        });
    }

    // Цепочка вызовов создания стрима
    private class StreamFactory {
        private void createCameraManager() {
            if (cameraManager != null) {
                return;
            }
            cameraManager = new CameraManager(context);
        }

        public void createCameraPreview() {
            createCameraManager();
            if (cameraPreview != null) {
                return;
            }
            cameraPreview = new CameraPreview(context, cameraManager.getCamera());
        }

        private void createLocationManager() {
            if (locationManager != null) {
                return;
            }
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        }

        public void createStream(StreamingActivity streamingActivity) {
            if (client != null && client.getTranslationStatus() != -1) {
                return;
            }

            createCameraPreview();
            if (client == null || client.getTranslationStatus() == -1) {
                client = new Client(streamingActivity, ip, port, cameraPreview);
            }
            createLocationManager();

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_REFRESH_TIME, LOCATION_REFRESH_DISTANCE, client);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.ipcamera, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_settings:
                setting();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setting() {
        LayoutInflater factory = LayoutInflater.from(this);
        final View textEntryView = factory.inflate(R.layout.server_setting, null);
        AlertDialog dialog = new AlertDialog.Builder(StreamingActivity.this)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setTitle(R.string.setting_title)
                .setView(textEntryView)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        EditText ipEdit = (EditText) textEntryView.findViewById(R.id.ip_edit);
                        EditText portEdit = (EditText) textEntryView.findViewById(R.id.port_edit);
                        if (!"".equals(ipEdit.getText().toString()))
                            setIp(ipEdit.getText().toString());
                        if (!"".equals(portEdit.getText().toString()))
                            setPort(Integer.parseInt(portEdit.getText().toString()));

//                        Toast.makeText(StreamingActivity.this, "New address: " + mIP + ":" + mPort, Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                    /* User clicked cancel so do some stuff */
                    }
                })
                .create();
        dialog.show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        closeSocketClient();
        cameraPreview.onPause();
        cameraManager.onPause();              // release the camera immediately on pause event
        if (streamStatus)
            btnStart.callOnClick();
        reset();
    }

    /**
     * Обнуление активити
     */
    private void reset() {
//        btn_Start.setText(R.string.start);
        streamStatus = false;
    }

    /**
     * Сетер порта
     *
     * @param port новый порт
     */
    public void setPort(int port) {
        this.port = port;
        lbl_port.setText(String.valueOf(port));
    }

    /**
     * Ставим айпи
     *
     * @param ip новый айпи
     */
    public void setIp(String ip) {
        this.ip = ip;
        lbl_ip.setText(ip);
    }


    @Override
    protected void onResume() {
        super.onResume();
        cameraManager.onResume();
        cameraPreview.setCamera(cameraManager.getCamera());
    }

    /**
     * Закрытие клиента
     */
    private void closeSocketClient() {
        if (client == null)
            return;

//        client.interrupt();
        client.close();
//        try {
//            mThread.join();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        client = null;
        streamStatus = false;
    }
}
