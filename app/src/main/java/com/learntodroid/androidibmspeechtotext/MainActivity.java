package com.learntodroid.androidibmspeechtotext;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.FFmpeg;
import com.ibm.cloud.sdk.core.http.HttpMediaType;
import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import com.ibm.watson.speech_to_text.v1.SpeechToText;
import com.ibm.watson.speech_to_text.v1.model.RecognizeOptions;
import com.ibm.watson.speech_to_text.v1.model.SpeechRecognitionResults;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final String[] permissions = {Manifest.permission.RECORD_AUDIO};
    private boolean audioRecordingPermissionGranted;

    private static final String API_KEY = "<<API KEY>>";
    private static final String URL = "<<URL>>";

    private Button startRecordingButton;

    private List<Result> results;
    private ResultsRecyclerAdapter resultsRecyclerAdapter;

    private MediaRecorder mediaRecorder;

    private String recordedFileName;
    private String convertedFileName;

    private Handler mainHandler;

    private boolean isRecording;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainHandler = new Handler();
        isRecording = false;
        mediaRecorder = new MediaRecorder();
        results = new ArrayList<>();

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        startRecordingButton= findViewById(R.id.activity_main_recordSpeech);

        RecyclerView resultsRecyclerView = findViewById(R.id.activity_main_results);
        resultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        resultsRecyclerAdapter = new ResultsRecyclerAdapter();
        resultsRecyclerView.setAdapter(resultsRecyclerAdapter);

        startRecordingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isRecording) {
                    if (audioRecordingPermissionGranted) {
                        try {
                            startAudioRecording();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                convertSpeech();
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    thread.start();
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_RECORD_AUDIO_PERMISSION:
                audioRecordingPermissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }

        if (!audioRecordingPermissionGranted) {
            finish();
        }
    }

    private void toggleRecording() {
        isRecording = !isRecording;
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isRecording) {
                    startRecordingButton.setText("Convert Speech");
                } else {
                    startRecordingButton.setText("Start Recording");
                }
            }
        });
    }

    private void startAudioRecording() throws IOException {
        toggleRecording();
        String uuid = UUID.randomUUID().toString();
        recordedFileName = getFilesDir().getPath() + "/" + uuid + ".3gp";
        convertedFileName = getFilesDir().getPath() + "/" + uuid + ".mp3";

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setOutputFile(recordedFileName);

        mediaRecorder.prepare();
        mediaRecorder.start();
    }

    private void convertSpeech() throws FileNotFoundException {
        toggleRecording();

        mediaRecorder.stop();
        mediaRecorder.reset();
        mediaRecorder.release();

        int rc = FFmpeg.execute(String.format("-i %s -c:a libmp3lame %s", recordedFileName, convertedFileName));

        if (rc == RETURN_CODE_SUCCESS) {
            Log.i(Config.TAG, "Command execution completed successfully.");

            try {
                playRecording(convertedFileName);
            } catch (IOException e) {
                e.printStackTrace();
            }

            IamAuthenticator authenticator = new IamAuthenticator(API_KEY);
            SpeechToText speechToText = new SpeechToText(authenticator);
            speechToText.setServiceUrl(URL);

            File audioFile = new File(convertedFileName);

            RecognizeOptions options = new RecognizeOptions.Builder()
                    .audio(audioFile)
                    .contentType(HttpMediaType.AUDIO_MP3)
                    .model("en-AU_NarrowbandModel")
                    .build();

            final SpeechRecognitionResults transcript = speechToText.recognize(options).execute().getResult();
            System.out.println(transcript);

            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        results.clear();
                        JSONObject jsonObject = new JSONObject(transcript.toString());
                        JSONArray resultsArray = jsonObject.getJSONArray("results");
                        for (int i = 0; i < resultsArray.length(); i++) {
                            JSONArray alternativesArray = resultsArray.getJSONObject(i).getJSONArray("alternatives");
                            for (int j = 0; j < alternativesArray.length(); j++) {
                                JSONObject resultObject = alternativesArray.getJSONObject(j);
                                results.add(
                                        new Result(
                                                resultObject.getString("transcript"),
                                                resultObject.getDouble("confidence")
                                        )
                                );
                            }
                        }
                        resultsRecyclerAdapter.setResults(results);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });

        } else if (rc == RETURN_CODE_CANCEL) {
            Log.i(Config.TAG, "Command execution cancelled by user.");
        } else {
            Log.i(Config.TAG, String.format("Command execution failed with rc=%d and the output below.", rc));
            Config.printLastCommandOutput(Log.INFO);
        }
    }


    private void playRecording(String fileName) throws IOException {
        MediaPlayer player = new MediaPlayer();
        player.setDataSource(fileName);
        player.prepare();
        player.start();
    }
}