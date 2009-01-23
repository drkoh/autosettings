/*
 * (c) ralfoide gmail com, 2009
 * Project: Timeriffic
 * License TBD
 */

package com.alfray.timeriffic.profiles;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alfray.timeriffic.R;

public class Profile {
    
    private String mName;
    private String mValue1;

    public Profile() {
        mName = "Test";
        mValue1 = "7 am start";
    }

    public void addTo(LayoutInflater layoutInflater, LinearLayout profilesLinear) {
        View pv = layoutInflater.inflate(R.layout.profile, null /*root*/);
        profilesLinear.addView(pv);
        
        TextView tv = (TextView) pv.findViewById(R.id.profileName);
        tv.setText(mName);
        tv = (TextView) pv.findViewById(R.id.TextView01);
        tv.setText(mValue1);
    }

}