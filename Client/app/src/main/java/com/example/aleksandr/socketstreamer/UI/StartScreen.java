package com.example.aleksandr.socketstreamer.UI;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.text.InputFilter;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.aleksandr.socketstreamer.R;
import com.example.aleksandr.socketstreamer.data.Abstractions.StreamData;
import com.google.gson.JsonObject;
import com.pnikosis.materialishprogress.ProgressWheel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class StartScreen extends Activity {
    // Objects
    private SharedPreferences sharedPreferences;

    // UI
    private ProgressWheel progressWheel;
    private Button btnLogin;
    private EditText txt_serverIp;
    private EditText txt_serverPort;
    private TextView version;
    private TextView lblStatus;

    //Properties
    private final String HOST_IP = "HOST_IP";
    private final String HOST_PORT = "HOST_PORT";
    private final String LOG_TAG = "START_SCREEN";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // убрать тайтл бар
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        // убрать ноутификации
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_start_screen);

        // инициализация форм
        initForms();

        setVersion(); // set version
        setLoginListener(); // set login button

        tryLoadPref(); // load pref from last access
    }

    // initialize forms
    private void initForms() {
        progressWheel = (ProgressWheel) findViewById(R.id.progress_wheel);
        progressWheel.stopSpinning();
        version = (TextView) findViewById(R.id.lblVersion);
        btnLogin = (Button) findViewById(R.id.btnLogin);
        txt_serverIp = (EditText) findViewById(R.id.txt_serverIp);
        txt_serverPort = (EditText) findViewById(R.id.txt_serverPort);
        lblStatus = (TextView) findViewById(R.id.lblStatus);
        lblStatus.setText("");
    }

    // setVersion
    private void setVersion() {
        try {
            String versionName = "version " + getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            version.setText(versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

    }

    // set button login listener
    private void setLoginListener() {
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                receiveStreams();
            }
        });
    }

    // try to load inet adress from last login
    private void tryLoadPref() {
        InetSocketAddress lastLoginToCorrectServer = loadFromPreferences();
        if (lastLoginToCorrectServer != null) {
            txt_serverIp.setText(lastLoginToCorrectServer.getAddress().getHostAddress());
            txt_serverPort.setText(String.valueOf(lastLoginToCorrectServer.getPort()));
        }
    }

    private void receiveStreams() {
        new ReceiveStreams().execute(String.valueOf(txt_serverIp.getText()), String.valueOf(txt_serverPort.getText()));
    }

    class ReceiveStreams extends AsyncTask<String, Void, List<String>> {
        String ip = null;
        String port = null;

        @Override
        protected List<String> doInBackground(String... params) {
            if (params.length < 2) {
                return null;
            }
            ip = params[0];
            port = params[1];

            Socket socket = null;
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(ip, Integer.valueOf(port)));
//                socket = new Socket(ip, Integer.valueOf(port));
//                socket.connect(new InetSocketAddress(ip, Integer.valueOf(port)));

                BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                JSONObject jsonObjectTo = new JSONObject();
                jsonObjectTo.put("streams", "get");

                bufferedWriter.write(jsonObjectTo.toString() + "\n");
                bufferedWriter.flush();

                String from = readAll(bufferedReader);
                JSONObject jsonObjectFrom = new JSONObject(from);

                List<String> result = parseAnswer(jsonObjectFrom.get("streams").toString());

                return result;
            } catch (Exception e) {
                Log.d(LOG_TAG, "Server not responding.");
                return null;
            }
        }

        private String readAll(Reader rd) throws IOException {
            StringBuilder sb = new StringBuilder();
            int cp;
            while ((cp = rd.read()) != -1) {
                sb.append((char) cp);
            }
            return sb.toString();
        }

        private List<String> parseAnswer(String answer) throws JSONException {
            JSONArray jsonArray = new JSONArray(answer);
            List<String> strings = new ArrayList<>();
            JSONObject currentStream;
//            if (jsonArray != null) {
//                int len = jsonArray.length();
//                for (int i=0;i<len;i++){
//                    currentStream = new JSONObject(jsonArray.get(i).toString());
//                    streamDatas.add(new StreamData(currentStream.getString("id"),currentStream.getString("name")));
//                }
//            }
            if (jsonArray != null) {
                int len = jsonArray.length();
                for (int i = 0; i < len; i++) {
                    strings.add(jsonArray.get(i).toString());
                }
            }

            return strings;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            setStatus(0);
        }

        @Override
        protected void onPostExecute(List<String> strings) {
            super.onPostExecute(strings);
            if (strings == null) {
                setStatus(-1);
                return;
            }
            setStatus(1);

            saveToSharedPreferences(new InetSocketAddress(ip, Integer.valueOf(port)));

            Intent intent = new Intent();
            intent.putStringArrayListExtra("streams", (ArrayList<String>) strings);
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    private void setStatus(int status) {
        switch (status) {
            case -1: {
                lblStatus.setText(R.string.txt_status_Error);
                lblStatus.setTextColor(getResources().getColor(R.color.statusError));
                progressWheel.stopSpinning();
                break;
            }
            case 0: {
                lblStatus.setText(R.string.txt_status_Connecting);
                lblStatus.setTextColor(getResources().getColor(R.color.statusNoMatter));
                progressWheel.spin();
                break;
            }
            case 1: {
                lblStatus.setText(R.string.txt_status_Connected);
                lblStatus.setTextColor(getResources().getColor(R.color.statusNoMatter));
                progressWheel.stopSpinning();
                break;
            }
        }
    }

    @Nullable
    private InetSocketAddress loadFromPreferences() {
        if (sharedPreferences == null) {
            sharedPreferences = getPreferences(MODE_PRIVATE);
        }
        String hostIp = sharedPreferences.getString(HOST_IP, null);
        int hostPort = sharedPreferences.getInt(HOST_PORT, -1);
        if (hostIp == null || hostPort == -1) {
            return null;
        }
        InetSocketAddress inetSocketAddress;
        try {
            inetSocketAddress = new InetSocketAddress(hostIp, Integer.valueOf(hostPort));
        } catch (Exception e) {
            Log.d(LOG_TAG, "Error trying to create InetSocketAdress: " + e.getMessage());
            return null;
        }
        return inetSocketAddress;
    }

    private void saveToSharedPreferences(InetSocketAddress inetSocketAddress) {
        if (sharedPreferences == null) {
            sharedPreferences = getPreferences(MODE_PRIVATE);
        }
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(HOST_IP, inetSocketAddress.getAddress().getHostAddress());
        editor.putInt(HOST_PORT, inetSocketAddress.getPort());
        editor.apply();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}
