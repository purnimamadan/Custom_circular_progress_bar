package com.example.circularpb;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    //region private variables
    private CustomProgressBar mCustomProgressBar;
    private EditText mInputProgress;
    private Button mAnimate;
    private double mMaxProgress = 100;
    private int mDotWidth = 16;
    //endregion

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //region gather ids
        mCustomProgressBar = findViewById(R.id.circular_custom_progressBar);
        mInputProgress = findViewById(R.id.et_progress);
        mAnimate = findViewById(R.id.btn_animate);
        //endregion

        setupProgressBar();

        mAnimate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double value = Double.valueOf(mInputProgress.getText().toString());
                if (value > mMaxProgress)
                    showInputError();
                else
                    mCustomProgressBar.setCurrentProgress(value);
            }
        });
    }

    //region private methods
    private void setupProgressBar() {
        mCustomProgressBar.setMaxProgress(mMaxProgress);
        mCustomProgressBar.setDotWidthDp(mDotWidth);
        mCustomProgressBar.setDotColor(getResources().getColor(R.color.orange));
    }

    private void showInputError() {
        Toast.makeText(this, "Give Valid Input(0-100)", Toast.LENGTH_SHORT).show();
    }
    //endregion
}
