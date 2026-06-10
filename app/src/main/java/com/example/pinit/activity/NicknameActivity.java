package com.example.pinit.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pinit.R;
import com.example.pinit.manager.FirebaseManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class NicknameActivity extends AppCompatActivity {

    private EditText etNickname;
    private Button btnNicknameSubmit;
    private ProgressBar pbNickname;
    private FirebaseAuth mAuth;
    private TextView tvLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nickname);

        mAuth = FirebaseAuth.getInstance();

        etNickname = findViewById(R.id.etNickname);
        btnNicknameSubmit = findViewById(R.id.btnNicknameSubmit);
        pbNickname = findViewById(R.id.pbNickname);
        tvLogout = findViewById(R.id.tvLogout);

        btnNicknameSubmit.setOnClickListener(v -> saveNickname());
        tvLogout.setOnClickListener(v -> handleLogout());
    }

    private void handleLogout() {
        if (mAuth != null) {
            mAuth.signOut();
        }

        Toast.makeText(this, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(NicknameActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void saveNickname() {
        String nickname = etNickname.getText().toString().trim();

        if (nickname.isEmpty()) {
            Toast.makeText(this, "닉네임을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (nickname.length() < 2) {
            Toast.makeText(this, "닉네임은 최소 2자 이상이어야 합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        pbNickname.setVisibility(View.VISIBLE);
        btnNicknameSubmit.setEnabled(false);

        FirebaseManager.getInstance().updateNickname(nickname, new FirebaseManager.OnActionListener() {
            @Override
            public void onSuccess() {
                pbNickname.setVisibility(View.GONE);
                btnNicknameSubmit.setEnabled(true);

                String email = user.getEmail();
                SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
                prefs.edit().putBoolean("nickname_done_" + email, true).apply();

                Toast.makeText(NicknameActivity.this, "닉네임 설정 완료!", Toast.LENGTH_SHORT).show();
                navigateToNext(prefs, email);
            }

            @Override
            public void onFailure(Exception e) {
                pbNickname.setVisibility(View.GONE);
                btnNicknameSubmit.setEnabled(true);
                String msg = e != null ? e.getMessage() : "오류 발생";
                Toast.makeText(NicknameActivity.this, "저장 실패: " + msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void navigateToNext(SharedPreferences prefs, String email) {
        boolean surveyDone = prefs.getBoolean("survey_done_" + email, false);
        Intent intent;

        if (!surveyDone) {
            intent = new Intent(this, SurveyActivity.class);
        } else {
            intent = new Intent(this, MainActivity.class);
        }

        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        Toast.makeText(this, "닉네임 설정을 완료해야 합니다.", Toast.LENGTH_SHORT).show();
    }
}