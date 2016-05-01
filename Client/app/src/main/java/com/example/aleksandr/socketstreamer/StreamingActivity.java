package com.example.aleksandr.socketstreamer;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
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
import com.dynamsoft.ipcamera.R;

/**
 * Активити для проведения трансляций
 */
public class StreamingActivity extends Activity {
    /**
     * Дистанция обновления геолокации в метрах
     */
    private static final float LOCATION_REFRESH_DISTANCE = 5;
    /**
     * Минимальное время обновления геолокации
     */
    private static final long LOCATION_REFRESH_TIME = 3000;
    /**
     * Дефолтное значение сервера
     */
    private final String DEFAULT_SERVER = "77.94.175.81";
    /**
     * Дефолтный порт
     */
    private final int DEFAULT_PORT = 8585;
    /**
     * Текст на экране для порта
     */
    TextView lbl_port;
    /**
     * Текст на экране для ip
     */
    TextView lbl_ip;
//    /**
//     * Кнопка для записи
//     */
////    private Button btn_Start;
    /**
     * Текст локации на экране
     */
    TextView lbl_location;
    /**
     * кнопка старта
     */
    MorphingButton btnStart;
    /**
     * Последнее зарегестрированное значение геолокации
     */
    Location lastLocation;
    /**
     * Превью камеры
     */
    private CameraPreview mPreview;
    /**
     * Менеджер камеры
     */
    private CameraManager mCameraManager;
    /**
     * Идёт ли трансляция
     */
    private boolean mIsOn = false;
    /**
     * Сокет-клиент для отправки
     */
    private SocketClient mThread;
    /**
     * Контекст программы
     */
    private Context mContext;
    /**
     * Ip сервера
     */
    private String mIP = "77.94.175.81";
    /**
     * Порт сервера
     */
    private int mPort = 8585;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stream);

//        btn_Start = (Button) findViewById(R.id.button_capture);
//        btn_Start.setOnClickListener(
//                new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        // если трансляция ещё не идёт
//                        if (!mIsOn) {
//                            // нет Ip - дефолтный, есть - кастомный
//                            if (mIP == null) {
//                                mThread = new SocketClient(mPreview);
//                            } else {
//                                mThread = new SocketClient(mPreview, mIP, mPort);
//                            }
//
//                            // ставим запись в режим записи
//                            mIsOn = true;
//                            // текст меняется на стоп
//                            btn_Start.setText(R.string.stop);
//                        } else {
//                            // иначе закрытие сокета
//                            closeSocketClient();
//                            reset();
//                        }
//                    }
//                }
//        );
        /**
         * установка контекста
         */
        mContext = this;

        // установка менеджера камеры
        mCameraManager = new CameraManager(this);
        // превью
        mPreview = new CameraPreview(this, mCameraManager.getCamera());
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        // Установка ip
        lbl_ip = (TextView) findViewById(R.id.lbl_ip);
        setIp(DEFAULT_SERVER);
        // Установка port
        lbl_port = (TextView) findViewById(R.id.lbl_port);
        setPort(DEFAULT_PORT);

        // Установка локации
        lbl_location = (TextView) findViewById(R.id.lbl_location);

        // Morphing button
        btnStart = (MorphingButton) findViewById(R.id.button_capture);

        // настройка кнопки
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Выключение трансляции
                if (mIsOn) {
                    MorphingButton.Params square = MorphingButton.Params.create()
                            .duration(500)
                            .cornerRadius((int) getResources().getDimension(R.dimen.btn_10dp))
                            .width((int) getResources().getDimension(R.dimen.btn_200p))
                            .height((int) getResources().getDimension(R.dimen.btn_56dp))
                            .color(R.color.mb_blue)
                            .colorPressed(R.color.mb_blue_dark)
                            .text(getResources().getString(R.string.start));
                    btnStart.morph(square);

                    closeSocketClient();

                    mIsOn = !mIsOn;
                }
                // Запуск трансляции
                else {
                    MorphingButton.Params circle = MorphingButton.Params.create()
                            .duration(500)
                            .cornerRadius((int) getResources().getDimension(R.dimen.btn_56dp)) // 56 dp
                            .width((int) getResources().getDimension(R.dimen.btn_56dp)) // 56 dp
                            .height((int) getResources().getDimension(R.dimen.btn_56dp)) // 56 dp
                            .color(R.color.mb_blue) // normal state color
                            .colorPressed(R.color.mb_blue_dark) // pressed state color
                            .icon(R.drawable.stop); // icon
                    btnStart.morph(circle);

                    // создание сокет клиента для отправки
                    mThread = new SocketClient(mPreview, mIP, mPort);

                    // создаём менеджер геолокации и слушатель
                    LocationManager mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
                    LocationListener mLocationListener = new LocationListener() {
                        @Override
                        public void onLocationChanged(Location location) {
                            setLastLocation(location);
                        }

                        @Override
                        public void onStatusChanged(String provider, int status, Bundle extras) {

                        }

                        @Override
                        public void onProviderEnabled(String provider) {

                        }

                        @Override
                        public void onProviderDisabled(String provider) {

                        }
                    };
                    // Проверка наличия разрешения на геолокацию
                    if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                            && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(mContext, "Please give access to geolocation for app.", Toast.LENGTH_SHORT).show();
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_REFRESH_TIME,
                            LOCATION_REFRESH_DISTANCE, mLocationListener);

                    mIsOn = !mIsOn;
                }

            }
        });


        // изначально выключен
        MorphingButton.Params square = MorphingButton.Params.create()
                .duration(500)
                .cornerRadius((int) getResources().getDimension(R.dimen.btn_10dp))
                .width((int) getResources().getDimension(R.dimen.btn_200p))
                .height((int) getResources().getDimension(R.dimen.btn_56dp))
                .color(R.color.mb_blue)
                .colorPressed(R.color.mb_blue_dark)
                .text(getResources().getString(R.string.start));
        btnStart.morph(square);
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
        mPreview.onPause();
        mCameraManager.onPause();              // release the camera immediately on pause event
        if (mIsOn)
            btnStart.callOnClick();
        reset();
    }

    /**
     * Обнуление активити
     */
    private void reset() {
//        btn_Start.setText(R.string.start);
        mIsOn = false;
    }

    /**
     * Сетер порта
     *
     * @param port новый порт
     */
    public void setPort(int port) {
        mPort = port;
        lbl_port.setText(String.valueOf(mPort));
    }

    /**
     * Ставим айпи
     *
     * @param ip новый айпи
     */
    public void setIp(String ip) {
        mIP = ip;
        lbl_ip.setText(mIP);
    }

    /**
     * Установка новой локации
     *
     * @param lastLocation новая локация
     */
    public void setLastLocation(Location lastLocation) {
        this.lastLocation = lastLocation;
        if (lastLocation != null)
            lbl_location.setText(String.valueOf(lastLocation.getLatitude()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCameraManager.onResume();
        mPreview.setCamera(mCameraManager.getCamera());
    }

    /**
     * Закрытие клиента
     */
    private void closeSocketClient() {
        if (mThread == null)
            return;

        mThread.interrupt();
        try {
            mThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mThread = null;
    }
}
