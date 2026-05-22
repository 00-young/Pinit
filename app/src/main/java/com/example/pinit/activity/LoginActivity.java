package com.example.pinit.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pinit.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

// [화면] 앱 최초 진입점 - Firebase 이메일/비밀번호 로그인 및 회원가입
// 이미 로그인된 계정이 있으면 onStart()에서 바로 MainActivity로 이동 (자동 로그인)
// LoginActivity → MainActivity 단방향 이동, finish()로 뒤로가기 차단
public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin, btnRegister;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth; // Firebase 인증 인스턴스

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        etEmail    = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin   = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        progressBar = findViewById(R.id.progressBar);

        btnLogin.setOnClickListener(v -> login());
        btnRegister.setOnClickListener(v -> register());
    }

    // 액티비티 시작 시 기존 로그인 세션 확인 → 세션 있으면 바로 메인으로 이동
    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            goToMain();
        }
    }

    // 이메일/비밀번호로 Firebase 로그인 시도
    // 성공: goToMain() / 실패: 에러 메시지 Toast 표시
    // 요청 중 버튼 비활성화 + ProgressBar 표시로 중복 요청 방지
    private void login() {
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "이메일과 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);
        btnRegister.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);
                    btnRegister.setEnabled(true);

                    if (task.isSuccessful()) {
                        goToMain();
                    } else {
                        String msg = task.getException() != null
                                ? task.getException().getMessage() : "로그인 실패";
                        Toast.makeText(this, "로그인 실패: " + msg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Firebase에 새 계정 생성 (이메일/비밀번호)
    // 비밀번호 6자 미만이면 서버 요청 전 클라이언트에서 차단
    // 성공: 회원가입 즉시 로그인 상태가 되므로 바로 goToMain()
    private void register() {
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "이메일과 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "비밀번호는 6자 이상이어야 합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);
        btnRegister.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);
                    btnRegister.setEnabled(true);

                    if (task.isSuccessful()) {
                        Toast.makeText(this, "회원가입 완료! 로그인합니다.", Toast.LENGTH_SHORT).show();
                        goToMain();
                    } else {
                        String msg = task.getException() != null
                                ? task.getException().getMessage() : "회원가입 실패";
                        Toast.makeText(this, "회원가입 실패: " + msg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    // MainActivity로 이동 후 현재 Activity 종료 (백 스택에서 제거 → 뒤로가기로 복귀 불가)
    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
