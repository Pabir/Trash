package com.pabirul.trash;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.database.Cursor;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.app.ActivityCompat;

public class MainActivity extends AppCompatActivity {

    private final String TRASH_DIR_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Trash";
    private final int PERMISSION_REQUEST_CODE = 100;
    private static final int PICK_FILE_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check and request for permissions
        if (checkStoragePermissions()) {
            setupApp();
        } else {
            requestStoragePermissions();
        }
    }

    // Method to check if storage permissions are granted
    private boolean checkStoragePermissions() {
        int writePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int readPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        return writePermission == PackageManager.PERMISSION_GRANTED && readPermission == PackageManager.PERMISSION_GRANTED;
    }

    // Method to request storage permissions
    private void requestStoragePermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
											  Manifest.permission.WRITE_EXTERNAL_STORAGE,
											  Manifest.permission.READ_EXTERNAL_STORAGE
										  }, PERMISSION_REQUEST_CODE);
    }

    // Handle the result of permission request
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupApp();
            } else {
                Toast.makeText(this, "Storage permissions are required to use this app.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Method to setup the app after permissions are granted
    private void setupApp() {
        // Ensure Trash directory exists
        File trashDir = new File(TRASH_DIR_PATH);
        if (!trashDir.exists()) {
            trashDir.mkdirs();
        }

        Button moveToTrashButton = findViewById(R.id.btn_move_to_trash);
        Button restoreButton = findViewById(R.id.btn_restore_from_trash);
        Button deleteButton = findViewById(R.id.btn_delete_permanently);

        // Move file to trash
        moveToTrashButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					openFilePicker();
				}
			});

        // Restore file from trash
        restoreButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					restoreFileFromTrash("testfile.txt");
				}
			});

        // Permanently delete file
        deleteButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					deletePermanently("testfile.txt");
				}
			});
    }

    // Method to open file picker
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK_FILE_REQUEST_CODE);
    }

    // Handle the result of file selection
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                moveFileToTrash(uri);
            }
        }
    }

    // Method to move file to Trash (from file picker)
    private void moveFileToTrash(Uri fileUri) {
        try {
            // Get file name
            String fileName = getFileName(fileUri);

            // Copy the file to the Trash directory
            File trashFile = new File(TRASH_DIR_PATH, fileName);
            try (InputStream inputStream = getContentResolver().openInputStream(fileUri);
			FileOutputStream outputStream = new FileOutputStream(trashFile)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
            }

            Toast.makeText(this, "File moved to Trash: " + fileName, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to move file to Trash", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    // Helper method to get the file name from the Uri
    private String getFileName(Uri uri) {
        String fileName = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    fileName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (fileName == null) {
            fileName = uri.getLastPathSegment();
        }
        return fileName;
    }

    // Method to restore file from Trash
    private void restoreFileFromTrash(String fileName) {
        File trashFile = new File(TRASH_DIR_PATH, fileName);
        if (trashFile.exists()) {
            File restoreLocation = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download", trashFile.getName());
            if (trashFile.renameTo(restoreLocation)) {
                Toast.makeText(this, "File restored", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to restore file", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "File not found in Trash", Toast.LENGTH_SHORT).show();
        }
    }

    // Method to delete file permanently
    private void deletePermanently(String fileName) {
        File trashFile = new File(TRASH_DIR_PATH, fileName);
        if (trashFile.exists()) {
            if (trashFile.delete()) {
                Toast.makeText(this, "File deleted permanently", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to delete file", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "File not found in Trash", Toast.LENGTH_SHORT).show();
        }
    }
}

