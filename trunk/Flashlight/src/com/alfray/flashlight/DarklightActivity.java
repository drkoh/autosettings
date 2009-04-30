/*
 * (c) ralfoide gmail com, 2009
 * Project: Flashlight
 * License: GPLv3
 */

package com.alfray.flashlight;

import android.widget.ImageView;
import android.widget.TextView;

public class DarklightActivity extends FlashlightActivity {

    public DarklightActivity() {
        super("Dark");
    }

    @Override
    protected void initializeOnCreate(TextView label, ImageView icon) {
        setBrightness(0.1f);
        setTitle("Dark Flashlight");
        label.setText("Dark");
        icon.setImageResource(R.drawable.dark_icon);
    }
}