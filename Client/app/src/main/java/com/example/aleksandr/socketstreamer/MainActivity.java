package com.example.aleksandr.socketstreamer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.dynamsoft.ipcamera.R;

public class MainActivity extends Activity {
    Button btnLaunchStreamActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent loginActivity = new Intent(this, StartScreen.class);
        startActivityForResult(loginActivity, 1);
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
    }

    protected void launchStream() {
        Intent streaminActivity = new Intent(this, StreamingActivity.class);
        startActivity(streaminActivity);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data == null) {
            finish();
        }
        // получаем результат входа
        String result = data.getStringExtra("result");
        if (!result.equals("ok")) {
            finish();
        }

        Toast.makeText(this, "Login complete", Toast.LENGTH_SHORT).show();
    }
}
