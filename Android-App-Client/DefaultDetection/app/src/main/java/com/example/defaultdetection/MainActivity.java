package com.example.defaultdetection;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button UploadBn, ChooseBn;
    private EditText Ipv4Address, Port;
    private TextView result;
    private ImageView imgView;
    private final int IMG_REQUEST = 1;
    private Bitmap bitmap;
    private String UploadUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        UploadBn = findViewById(R.id.uploadBn);
        ChooseBn = findViewById(R.id.chooseBn);
        Ipv4Address = findViewById(R.id.IPAddress);
        Port = findViewById(R.id.portNumber);
        imgView = findViewById(R.id.imageView);
        result = findViewById(R.id.result);

        ChooseBn.setOnClickListener(this);
        UploadBn.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.chooseBn:
                selectImage();
                break;

            case R.id.uploadBn:
                uploadImage();
                break;
        }
    }

    private void selectImage() {
        // Intent to select image
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, IMG_REQUEST);

        // reset last selected image and the last result
        imgView.setVisibility(View.GONE);
        imgView.setImageResource(0);
        result.setVisibility(View.GONE);
        result.setText("");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMG_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri path = data.getData();
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), path);
                imgView.setImageBitmap(bitmap);
                imgView.setVisibility(View.VISIBLE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void  uploadImage() {
        // form server url
        UploadUrl = "http://" + Ipv4Address.getText() + ":" + Port.getText() + "/";
        Log.d("UploadUrl", UploadUrl);

        StringRequest stringRequest = new StringRequest(Request.Method.POST, UploadUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // handle the response from server
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            String predicted_class = jsonObject.getString("prediction");

                            //String probability = jsonObject.getString("probability");
                            //Toast.makeText(MainActivity.this, predicted_class, Toast.LENGTH_LONG).show();

                            // set the result on the basis of response returned by the server
                            result.setVisibility(View.VISIBLE);
                            result.setText(predicted_class);
                            if (predicted_class.contentEquals("Defective"))
                                result.setTextColor(Color.RED);
                            else
                                result.setTextColor(Color.GREEN);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("Volley Error", error.toString());
            }
        })
        {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("image", imageToString(bitmap));
                return params;
            }
        };

        MySingleton.getInstance(MainActivity.this).addToRequestQue(stringRequest);
    }

    private String imageToString(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] imgBytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(imgBytes, android.util.Base64.DEFAULT);

    }
}
