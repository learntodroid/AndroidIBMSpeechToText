package com.learntodroid.androidibmspeechtotext;

public class Result {
    private String transcript;
    private double confidence;

    public Result(String transcript, double confidence) {
        this.transcript = transcript;
        this.confidence = confidence;
    }

    public String getTranscript() {
        return transcript;
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }
}
