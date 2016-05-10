package com.example.aleksandr.socketstreamer.UI;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.example.aleksandr.socketstreamer.R;
import com.example.aleksandr.socketstreamer.data.Abstractions.StreamData;
import com.example.aleksandr.socketstreamer.supporting.ListAdapter;
import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class MainActivity extends Activity {
    // UI
    Button btnLaunchStreamActivity;
    ListView listView;

    // Parameters
    private static final int REQUEST_CODE_LOGIN = 1;
    private static final int REQUEST_CODE_START = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent loginActivity = new Intent(this, StartScreen.class);
        startActivityForResult(loginActivity, REQUEST_CODE_LOGIN);
//        FloatingActionButton fabButton = new FloatingActionButton.Builder(this)
//                .withDrawable(R.drawable.camera)
//                .withButtonColor(Color.WHITE)
//                .withGravity(Gravity.BOTTOM | Gravity.RIGHT)
//                .withMargins(0, 0, 16, 16)
//                .create();

        // Установим запуск стрима по нажатию кнопки
        btnLaunchStreamActivity = (Button) findViewById(R.id.btnLaunchStreamActivity);
        btnLaunchStreamActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchStream();
            }
        });

        listView = (ListView) findViewById(R.id.lv_streams);
    }

    protected void launchStream() {
        Intent streamingActivity = new Intent(this, StreamingActivity.class);
        startActivity(streamingActivity);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data == null || resultCode != RESULT_OK) {
            finish();
            return;
        }

        switch (requestCode) {
            case REQUEST_CODE_LOGIN: {
                onLoginEnd(resultCode, data);
                break;
            }
            case REQUEST_CODE_START: {
                onStreamingActivityEnd(resultCode, data);
                break;
            }
        }
    }

    protected void onLoginEnd(int resultCode, Intent data) {
        // получаем результат входа
        Toast.makeText(this, "Server exist!", Toast.LENGTH_SHORT).show();

        ArrayList<String> streams = data.getStringArrayListExtra("streams");
        ArrayList<StreamData> streamDatas = null;
        try {
            streamDatas = parseJsonStreams(streams);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Server send wrong stream info", Toast.LENGTH_LONG).show();
            return;
        }

        listView.setAdapter(new ListAdapter(this, streamDatas));
    }

    private ArrayList<StreamData> parseJsonStreams(ArrayList<String> streams) throws JSONException {
        JSONObject jsonObject = null;
        String name;
        ArrayList<StreamData> streamDatas = new ArrayList<>(streams.size());
        for (String stream : streams) {
            jsonObject = new JSONObject(stream);
            StreamData streamData = new StreamData(jsonObject.getString("id"), null);
            name = jsonObject.getString("name");
            if (!"null".equals(name)) {
                streamData.setName(name);
            }
            streamDatas.add(streamData);
        }
        return streamDatas;
    }

    protected void onStreamingActivityEnd(int resultCode, Intent data) {

    }
}
