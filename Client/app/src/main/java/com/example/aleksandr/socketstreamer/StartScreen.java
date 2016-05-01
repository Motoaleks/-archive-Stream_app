package com.example.aleksandr.socketstreamer;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.dynamsoft.ipcamera.R;
import com.pnikosis.materialishprogress.ProgressWheel;

public class StartScreen extends Activity {
    // колесо прогрцзкм
    ProgressWheel progressWheel;
    // кнопка входа
    Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // убрать тайтл бар
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        // убрать ноутификации
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_start_screen);

        // колессо прогрузки
        progressWheel = (ProgressWheel) findViewById(R.id.progress_wheel);
        progressWheel.spin();

        // установка версии
        TextView version = (TextView) findViewById(R.id.lblVersion);
        try {
            String versionName = "version " + getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            version.setText(versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        // запуск входа в аккаунт
        btnLogin = (Button) findViewById(R.id.btnLogin);
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginSim();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    protected void loginSim() {
        Intent intent = new Intent();
        intent.putExtra("result", "ok");
        setResult(RESULT_OK, intent);
        finish();
    }
}
