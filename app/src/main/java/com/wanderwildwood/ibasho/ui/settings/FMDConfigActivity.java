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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.wanderwildwood.ibasho.R;
import com.wanderwildwood.ibasho.data.EncryptedSettingsRepository;
import com.wanderwildwood.ibasho.data.Settings;
import com.wanderwildwood.ibasho.data.SettingsRepository;
import com.wanderwildwood.ibasho.ui.FmdActivity;
import com.wanderwildwood.ibasho.ui.common.PasswordSetDialog;
import kotlin.Unit;

public class FMDConfigActivity extends FmdActivity implements TextWatcher {

    private SettingsRepository settings;
    private EncryptedSettingsRepository encSettings;

    private Button buttonSelectRingtone;
    private EditText editTextLockScreenMessage;
    private CheckBox checkBoxDeviceWipe;
    private Button buttonDeletePassword;
    private EditText editTextFmdCommand;

    private static final int REQUEST_CODE_RINGTONE = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_f_m_d_config);

        setupEdgeToEdgeAppBar(findViewById(R.id.appBar));
        setupEdgeToEdgeScrollView(findViewById(R.id.scrollView));

        settings = SettingsRepository.Companion.getInstance(this);
        encSettings = EncryptedSettingsRepository.Companion.getInstance(this);

        editTextLockScreenMessage = findViewById(R.id.editTextTextLockScreenMessage);
        editTextLockScreenMessage.setText((String) settings.get(Settings.SET_LOCKSCREEN_MESSAGE));
        editTextLockScreenMessage.addTextChangedListener(this);

        // The wipe command is off unless it is switched on here, on this phone, and it needs a
        // password of its own: nobody can arm it from the server.
        checkBoxDeviceWipe = findViewById(R.id.checkBoxWipeData);
        checkBoxDeviceWipe.setChecked((Boolean) settings.get(Settings.SET_WIPE_ENABLED));
        checkBoxDeviceWipe.setOnCheckedChangeListener((button, isChecked) -> {
            settings.set(Settings.SET_WIPE_ENABLED, isChecked);
            updateDeletePasswordButton();
        });
        buttonDeletePassword = findViewById(R.id.buttonDeletePassword);
        buttonDeletePassword.setOnClickListener(this::onEnterDeletePasswordClicked);
        updateDeletePasswordButton();

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
                Toast.makeText(this, getString(R.string.Toast_Empty_FMDCommand), Toast.LENGTH_LONG).show();
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

    private void onEnterDeletePasswordClicked(View v) {
        new PasswordSetDialog(v.getContext(), (newPassword) -> {
            encSettings.setDeletePassword(newPassword);
            updateDeletePasswordButton();
            return Unit.INSTANCE;
        }, R.string.password_enter, getString(R.string.delete_pw_message), true, true).show();
    }

    // Upstream coloured this button green or red. On an ink screen the words have to say it.
    private void updateDeletePasswordButton() {
        boolean enabled = (boolean) settings.get(Settings.SET_WIPE_ENABLED);
        String password = encSettings.getDeletePassword();
        boolean isPasswordEmpty = password == null || password.isBlank();

        buttonDeletePassword.setText(isPasswordEmpty ? R.string.password_set : R.string.password_change);
        findViewById(R.id.textViewDeletePasswordWarning)
                .setVisibility(enabled && isPasswordEmpty ? View.VISIBLE : View.GONE);
    }
}