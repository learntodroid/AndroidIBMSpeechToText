package com.learntodroid.androidibmspeechtotext;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ResultsRecyclerAdapter extends RecyclerView.Adapter<ResultsRecyclerAdapter.ResultViewHolder> {
    private List<Result> results;

    public ResultsRecyclerAdapter() {
        this.results = new ArrayList<>();
    }

    @NonNull
    @Override
    public ResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_result, parent, false);
        return new ResultViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ResultViewHolder holder, int position) {
        holder.bind(results.get(position));
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    public void setResults(List<Result> results) {
        this.results = results;
        notifyDataSetChanged();
    }

    class ResultViewHolder extends RecyclerView.ViewHolder {
        private TextView transcript;
        private TextView confidence;

        public ResultViewHolder(@NonNull View itemView) {
            super(itemView);

            transcript = itemView.findViewById(R.id.item_result_transcript);
            confidence = itemView.findViewById(R.id.item_result_confidence);
        }

        public void bind(Result result) {
            transcript.setText(result.getTranscript());
            confidence.setText(String.valueOf(result.getConfidence()));
        }
    }
}
