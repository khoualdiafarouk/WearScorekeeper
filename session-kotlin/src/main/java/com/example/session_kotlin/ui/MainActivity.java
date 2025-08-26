package com.example.session_kotlin.ui;

import android.os.Bundle;
import androidx.activity.ComponentActivity;
import androidx.compose.ui.platform.ComposeView;
import androidx.lifecycle.ViewModelProvider;

public class MainActivity extends ComponentActivity {

    private ScoreViewModel vm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vm = new ViewModelProvider(this).get(ScoreViewModel.class);

        ComposeView composeView = new ComposeView(this);
        setContentView(composeView);

        // Pass VM + a quit lambda (finish activity)
        UIBridge.setMainContent(composeView, vm, this::finish);
    }
}
