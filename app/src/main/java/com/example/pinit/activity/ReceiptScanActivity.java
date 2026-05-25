package com.example.pinit.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.pinit.R;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReceiptScanActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int REQUEST_GALLERY = 200;
    private static final int REQUEST_CAMERA_CAPTURE = 300;

    private PreviewView previewView;
    private ImageView ivPreview;
    private TextView tvResult;
    private ProgressBar progressBar;
    private Button btnScan, btnGallery, btnConfirm, btnRetry;
    private ImageCapture imageCapture;
    private double detectedAmount = 0;
    private int tripId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt_scan);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("영수증 스캔");
        }

        tripId = getIntent().getIntExtra("trip_id", -1);

        previewView = findViewById(R.id.previewView);
        ivPreview = findViewById(R.id.ivPreview);
        tvResult = findViewById(R.id.tvResult);
        progressBar = findViewById(R.id.progressBar);
        btnScan = findViewById(R.id.btnScan);
        btnGallery = findViewById(R.id.btnGallery);
        btnConfirm = findViewById(R.id.btnConfirm);
        btnRetry = findViewById(R.id.btnRetry);

        btnGallery.setOnClickListener(v -> openGallery());
        btnScan.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                takePicture();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            }
        });

        btnConfirm.setOnClickListener(v -> {
            Intent result = new Intent();
            result.putExtra("amount", detectedAmount);
            setResult(Activity.RESULT_OK, result);
            finish();
        });

        btnRetry.setOnClickListener(v -> resetUI());

        // 카메라 권한 있으면 바로 미리보기 시작
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this,
                        CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture);
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "카메라 초기화 실패", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePicture() {
        if (imageCapture == null) return;
        File photoFile = new File(getCacheDir(), "receipt_" + System.currentTimeMillis() + ".jpg");
        ImageCapture.OutputFileOptions options =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();
        imageCapture.takePicture(options, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        try {
                            InputImage image = InputImage.fromFilePath(
                                    ReceiptScanActivity.this, Uri.fromFile(photoFile));
                            ivPreview.setImageURI(Uri.fromFile(photoFile));
                            previewView.setVisibility(View.GONE);
                            ivPreview.setVisibility(View.VISIBLE);
                            runOcr(image);
                        } catch (IOException e) {
                            Toast.makeText(ReceiptScanActivity.this,
                                    "이미지 처리 실패", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onError(@NonNull ImageCaptureException e) {
                        Toast.makeText(ReceiptScanActivity.this,
                                "촬영 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_GALLERY && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                ivPreview.setImageBitmap(bitmap);
                previewView.setVisibility(View.GONE);
                ivPreview.setVisibility(View.VISIBLE);
                InputImage image = InputImage.fromBitmap(bitmap, 0);
                runOcr(image);
            } catch (IOException e) {
                Toast.makeText(this, "이미지 불러오기 실패", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void runOcr(InputImage image) {
        progressBar.setVisibility(View.VISIBLE);
        tvResult.setText("인식 중...");
        btnScan.setEnabled(false);
        btnGallery.setEnabled(false);

        TextRecognizer recognizer = TextRecognition.getClient(
                new KoreanTextRecognizerOptions.Builder().build());
        recognizer.process(image)
                .addOnSuccessListener(text -> {
                    progressBar.setVisibility(View.GONE);
                    String fullText = text.getText();
                    detectedAmount = extractAmount(fullText);
                    if (detectedAmount > 0) {
                        tvResult.setText("✅ 인식된 금액: " + (int) detectedAmount + "원\n\n" +
                                "--- 전체 인식 텍스트 ---\n" + fullText);
                        btnConfirm.setVisibility(View.VISIBLE);
                    } else {
                        tvResult.setText("금액을 찾지 못했습니다.\n\n--- 인식된 텍스트 ---\n" + fullText);
                    }
                    btnRetry.setVisibility(View.VISIBLE);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    tvResult.setText("인식 실패: " + e.getMessage());
                    btnRetry.setVisibility(View.VISIBLE);
                    btnScan.setEnabled(true);
                    btnGallery.setEnabled(true);
                });
    }

    /**
     * OCR 인식 오류 전처리
     * "90. 000" / "90, 000" / "90. O00" → "90000"으로 통일
     */
    private String normalizeOcrText(String text) {
        // 1. 알파벳 O/o/D/Q → 숫자 0으로 치환
        text = text.replace("O", "0")
                .replace("o", "0")
                .replace("D", "0")
                .replace("Q", "0");

        // 2. "숫자. 숫자" or "숫자, 숫자" (공백 포함) → 붙이기
        // 예: "90. 000" → "90000", "9, 000" → "9000"
        text = text.replaceAll("([0-9]+)[.,]\\s+([0-9]{3})", "$1$2");

        // 3. 공백 없는 경우: "90.000" or "90,000" → "90000" or "90,000"
        // 쉼표는 천단위 구분자로 유지 (패턴 매칭용), 점은 제거
        text = text.replaceAll("([0-9]+)\\.([0-9]{3})", "$1$2");

        return text;
    }

    // 금액 추출 로직 - 합계/총액/결제금액 키워드 우선, 없으면 가장 큰 숫자
    private double extractAmount(String text) {

        // 여기 추가 - OCR 결과 전체를 먼저 보정
        text = normalizeOcrText(text);
        String[] lines = text.split("\n");

        Pattern amountPattern =
                Pattern.compile("[0-9]{1,3}(,[0-9]{3})+|[0-9]{3,7}");

        double bestAmount = 0;

        // =========================
        // 1차: 합계 키워드 주변 탐색
        // =========================

        for (int i = 0; i < lines.length; i++) {

            String lower = lines[i].toLowerCase();

            boolean isTotalLine =
                    lower.matches(".*합\\s*계.*")
                            || lower.matches(".*총\\s*액.*")
                            || lower.matches(".*받\\s*을\\s*금\\s*액.*")
                            || lower.matches(".*받\\s*은\\s*금\\s*액.*")
                            || lower.matches(".*결\\s*제\\s*금\\s*액.*")
                            || lower.contains("total");

            if (!isTotalLine)
                continue;

            // 현재 줄 + 아래 2줄까지 탐색
            for (int j = i; j <= Math.min(i + 2, lines.length - 1); j++) {

                Matcher matcher =
                        amountPattern.matcher(lines[j]);

                double localMax = 0;

                while (matcher.find()) {

                    try {

                        String amountStr = matcher.group();

                        // =========================
                        // OCR 숫자 후처리
                        // =========================

                        // 숫자만 남기기
                        amountStr = amountStr.replaceAll("[^0-9]", "");

                        if (amountStr.length() < 3)
                            continue;

                        double amountValue =
                                Double.parseDouble(amountStr);

                        // 현실적인 범위
                        if (amountValue < 100
                                || amountValue > 3000000)
                            continue;

                        // 가장 큰 금액 저장
                        if (amountValue > localMax) {
                            localMax = amountValue;
                        }

                    } catch (Exception ignored) {}
                }
                // 합계 주변 최대값 반환
                if (localMax > 0) {
                    return localMax;
                }
            }
        }

        // =========================
        // 2차 fallback
        // =========================
        // 합계 탐색 실패 시만 실행

        if (bestAmount == 0) {

            int bestScore = -999;

            for (String line : lines) {

                String lower = line.toLowerCase();

                int score = 0;

                // 금액 가능성 높은 키워드
                if (lower.contains("원")) score += 3;

                // 제외 대상
                if (lower.contains("tel")) score -= 20;
                if (lower.contains("전화")) score -= 20;
                if (lower.contains("사업자")) score -= 20;
                if (lower.contains("승인번호")) score -= 20;
                if (lower.contains("번호")) score -= 20;
                if (lower.contains("카드")) score -= 20;
                if (lower.contains("주소")) score -= 20;
                if (lower.contains("서울")) score -= 20;
                if (lower.contains("아파트")) score -= 20;
                if (lower.contains("동")) score -= 10;
                if (lower.contains("호")) score -= 10;
                if (lower.contains("전자전표")) score -= 20;
                if (lower.contains("부가세")) score -= 20;
                if (lower.contains("bill")) score -= 20;
                if (lower.contains("pos")) score -= 20;

                if (lower.matches(".*\\d{8}-\\d{2}-\\d+.*"))
                    score -= 30;

                if (lower.matches(".*\\d{4}-\\d{2}-\\d{2}.*"))
                    score -= 20;

                Matcher matcher =
                        amountPattern.matcher(line);

                while (matcher.find()) {

                    try {

                        String numStr =
                                matcher.group().replace(",", "");

                        // 긴 숫자 제외
                        if (numStr.length() >= 6
                                && !numStr.contains(",")) {
                            continue;
                        }

                        double value =
                                Double.parseDouble(numStr);

                        if (value < 100 || value > 3000000)
                            continue;

                        if (score > bestScore) {

                            bestScore = score;
                            bestAmount = value;
                        }

                        else if (score == bestScore
                                && value > bestAmount) {

                            bestAmount = value;
                        }

                    } catch (Exception ignored) {}
                }
            }
        }

        return bestAmount;
    }

    private double extractNumber(String text) {
        Pattern pattern =
                Pattern.compile("(\\\\d{1,3}(,\\\\d{3})+|\\\\d+)");
        Matcher matcher = pattern.matcher(text);
        double max = 0;
        while (matcher.find()) {
            String numStr = matcher.group().replace(",", "");
            try {
                double num = Double.parseDouble(numStr);
                if (num > max) max = num;
            } catch (NumberFormatException ignored) {}
        }
        return max;
    }

    private void resetUI() {
        previewView.setVisibility(View.VISIBLE);
        ivPreview.setVisibility(View.GONE);
        tvResult.setText("영수증을 카메라에 비추면 자동으로 금액을 인식합니다");
        btnConfirm.setVisibility(View.GONE);
        btnRetry.setVisibility(View.GONE);
        btnScan.setEnabled(true);
        btnGallery.setEnabled(true);
        detectedAmount = 0;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}
