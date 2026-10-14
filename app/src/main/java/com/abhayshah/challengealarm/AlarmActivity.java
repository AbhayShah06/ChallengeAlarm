package com.abhayshah.challengealarm;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import java.util.Random;

public class AlarmActivity extends Activity {
    private int answer, tier, round, totalRounds;
    private final Random random = new Random();
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (android.os.Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(true); setTurnScreenOn(true); }
        else getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        setContentView(R.layout.activity_alarm);
        AlarmItem alarm = AlarmStore.find(this, getIntent().getIntExtra("alarmId", -1));
        tier = alarm == null ? 2 : alarm.difficulty;
        totalRounds = MathProblem.roundsForTier(tier);
        if (state != null) {
            answer = state.getInt("answer"); round = state.getInt("round");
            ((TextView)findViewById(R.id.problem)).setText(state.getString("prompt"));
            updateProgress();
        } else nextProblem();
        findViewById(R.id.dismiss).setOnClickListener(v -> checkAnswer());
    }
    private void nextProblem() {
        MathProblem problem = MathProblem.generate(random, tier); answer = problem.answer;
        ((TextView)findViewById(R.id.problem)).setText("Solve: " + problem.prompt);
        ((TextView)findViewById(R.id.error)).setText("");
        updateProgress();
    }
    private void updateProgress() { ((TextView)findViewById(R.id.progress)).setText("Problem " + (round + 1) + " of " + totalRounds + "  ·  Tier " + tier); }
    private void checkAnswer() {
        EditText field = findViewById(R.id.answer);
        try {
            if (Integer.parseInt(field.getText().toString().trim()) == answer) {
                round++; field.setText("");
                if (round >= totalRounds) { stopService(new Intent(this, AlarmService.class).setAction(AlarmService.ACTION_STOP)); finishAndRemoveTask(); }
                else nextProblem();
            } else { ((TextView)findViewById(R.id.error)).setText("Not quite. Try again."); field.setText(""); }
        } catch (NumberFormatException e) { ((TextView)findViewById(R.id.error)).setText("Enter an answer to dismiss."); }
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state); state.putInt("answer", answer); state.putInt("round", round);
        state.putString("prompt", ((TextView)findViewById(R.id.problem)).getText().toString());
    }
    @Override public void onBackPressed() { }
}
