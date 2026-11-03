package com.wanderwildwood.ibasho.ui.helper;

import android.content.Context;

import androidx.annotation.StringRes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.wanderwildwood.ibasho.BuildConfig;
import com.wanderwildwood.ibasho.R;


/**
 * One row of the Settings list. Rows come in groups, set apart by space rather than rules:
 * the server and who sees this phone; the commands and who may send them; where a place
 * comes from; backing up; this app; and the logs last.
 */
public class SettingsEntry {

    /** What a row opens. The list is matched on this, never on a row's position. */
    public enum Action {
        SERVER, FAMILY,
        COMMANDS, ACCESS_CONTROL,
        LOCATION_SOURCES,
        EXPORT, IMPORT,
        LANGUAGE, SETUP_GUIDE,
        LOGS, TROUBLESHOOTING,
    }

    /** The rows in order; true where a row begins a new group. */
    public static final List<Row> ORDER = Collections.unmodifiableList(Arrays.asList(
            new Row(Action.SERVER, R.string.Settings_FMDServer, true),
            new Row(Action.FAMILY, R.string.Settings_Family, false),
            new Row(Action.COMMANDS, R.string.Settings_FMDConfig, true),
            new Row(Action.ACCESS_CONTROL, R.string.Settings_Access_Control, false),
            new Row(Action.LOCATION_SOURCES, R.string.Settings_OpenCellId, true),
            new Row(Action.EXPORT, R.string.Settings_Export, true),
            new Row(Action.IMPORT, R.string.Settings_Import, false),
            new Row(Action.LANGUAGE, R.string.Settings_Appearance, true),
            new Row(Action.SETUP_GUIDE, R.string.Settings_Setup_Guide, false),
            new Row(Action.LOGS, R.string.Settings_Logs, true),
            new Row(Action.TROUBLESHOOTING, R.string.Settings_Debugging, false)
    ));

    public static final class Row {
        public final Action action;
        @StringRes public final int label;
        public final boolean startsGroup;

        Row(Action action, @StringRes int label, boolean startsGroup) {
            this.action = action;
            this.label = label;
            this.startsGroup = startsGroup;
        }
    }

    /** The rows this build shows: Troubleshooting is for debug builds only. */
    public static List<Row> rows(boolean debug) {
        List<Row> rows = new ArrayList<>();
        for (Row r : ORDER) {
            if (r.action == Action.TROUBLESHOOTING && !debug) continue;
            rows.add(r);
        }
        return rows;
    }

    final String string;
    public final Action action;
    final boolean startsGroup;

    SettingsEntry(Context context, Row row, boolean first) {
        this.string = context.getString(row.label);
        this.action = row.action;
        this.startsGroup = row.startsGroup && !first;
    }

    public static List<SettingsEntry> getSettingsEntries(Context context) {
        List<SettingsEntry> entries = new ArrayList<>();
        for (Row r : rows(BuildConfig.DEBUG)) {
            entries.add(new SettingsEntry(context, r, entries.isEmpty()));
        }
        return entries;
    }
}
