package com.wanderwildwood.ibasho.ui.settings;

import static com.wanderwildwood.ibasho.ui.UiUtil.setupEdgeToEdgeAppBar;
import static com.wanderwildwood.ibasho.ui.UiUtil.setupEdgeToEdgeScrollView;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.wanderwildwood.ibasho.R;
import com.wanderwildwood.ibasho.data.Settings;
import com.wanderwildwood.ibasho.data.SettingsRepository;
import com.wanderwildwood.ibasho.ui.FmdActivity;
import kotlin.Unit;

public class FMDConfigActivity extends FmdActivity implements TextWatcher {

    private SettingsRepository settings;

    private Button buttonSelectRingtone;
    private EditText editTextLockScreenMessage;
    private EditText editTextFmdCommand;

    private static final int REQUEST_CODE_RINGTONE = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_f_m_d_config);

        setupEdgeToEdgeAppBar(findViewById(R.id.appBar));
        setupEdgeToEdgeScrollView(findViewById(R.id.scrollView));

        settings = SettingsRepository.Companion.getInstance(this);

        editTextLockScreenMessage = findViewById(R.id.editTextTextLockScreenMessage);
        editTextLockScreenMessage.setText((String) settings.get(Settings.SET_LOCKSCREEN_MESSAGE));
        editTextLockScreenMessage.addTextChangedListener(this);

        buttonSelectRingtone = findViewById(R.id.buttonSelectRingTone);
        buttonSelectRingtone.setOnClickListener(this::onSelectRingtoneClicked);
        updateRingtoneButton();

        editTextFmdCommand = findViewById(R.id.editTextFmdCommand);
        editTextFmdCommand.setText((String) settings.get(Settings.SET_FMD_COMMAND));
        editTextFmdCommand.addTextChangedListener(this);

    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // unused
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // unused
    }

    @Override
    public void afterTextChanged(Editable edited) {
        if (edited == editTextLockScreenMessage.getText()) {
            settings.set(Settings.SET_LOCKSCREEN_MESSAGE, edited.toString());
        } else if (edited == editTextFmdCommand.getText()) {
            if (edited.toString().isEmpty()) {
                com.wanderwildwood.ibasho.ui.Notices.say(this, getString(R.string.Toast_Empty_FMDCommand));
                settings.set(Settings.SET_FMD_COMMAND, "fmd");
            } else {
                settings.set(Settings.SET_FMD_COMMAND, edited.toString().toLowerCase());
            }
        }
    }

    /** The button names the sound it chooses, so the choice can be seen from this page. */
    private void updateRingtoneButton() {
        String name = null;
        try {
            Uri uri = Uri.parse((String) settings.get(Settings.SET_RINGER_TONE));
            name = RingtoneManager.getRingtone(this, uri).getTitle(this);
        } catch (RuntimeException ignored) {
        }
        String label = getString(R.string.Settings_Select_Ringtone);
        buttonSelectRingtone.setText(name == null ? label : label + "\n" + name);
    }

    private void onSelectRingtoneClicked(View v) {
        // Chosen in the app's own list: Android's picker has no OK button on a Kompakt, so a
        // sound picked there was lost on going back.
        SoundChooser.INSTANCE.show(this, RingtoneManager.TYPE_ALARM, getString(R.string.Settings_Select_Ringtone),
                Uri.parse((String) settings.get(Settings.SET_RINGER_TONE)), false, android.media.AudioAttributes.USAGE_ALARM, uri -> {
                    if (uri != null) {
                        settings.set(Settings.SET_RINGER_TONE, uri.toString());
                        updateRingtoneButton();
                    }
                    return kotlin.Unit.INSTANCE;
                });
    }
}
