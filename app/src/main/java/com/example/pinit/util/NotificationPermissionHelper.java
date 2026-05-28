package com.example.pinit.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

/**
 * Android 13(API 33) 이상에서 알림 권한(POST_NOTIFICATIONS)을 전담 요청하는 헬퍼 클래스
 */
public class NotificationPermissionHelper {

    private final AppCompatActivity activity;
    private final ActivityResultLauncher<String> requestPermissionLauncher;
    private Runnable onPermissionResult;

    public NotificationPermissionHelper(AppCompatActivity activity) {
        this.activity = activity;
        
        // Launcher 등록은 반드시 LifecycleOwner(Activity/Fragment)가 생성될 때(Started 이전) 수행되어야 함
        this.requestPermissionLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (onPermissionResult != null) {
                        onPermissionResult.run();
                        onPermissionResult = null; // 1회성 실행 후 초기화
                    }
                }
        );
    }

    /**
     * 알림 권한을 요청합니다.
     * 안드로이드 13 미만이거나 이미 권한이 있는 경우 즉시 callback을 실행합니다.
     */
    public void requestNotificationPermission(Runnable callback) {
        this.onPermissionResult = callback;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
                // 이미 권한이 있음
                runCallback();
            } else {
                // 권한 요청 팝업 띄우기
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            // 안드로이드 13 미만은 권한 요청이 필요 없음 (설치 시 자동 부여)
            runCallback();
        }
    }

    private void runCallback() {
        if (onPermissionResult != null) {
            onPermissionResult.run();
            onPermissionResult = null;
        }
    }
}
