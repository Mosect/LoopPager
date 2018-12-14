package com.mosect.looppager.example;

import android.text.TextUtils;
import android.widget.EditText;

public class AppUtils {

    public static int getNumber(EditText editText) {
        String str = editText.getText().toString();
        if (!TextUtils.isEmpty(str)) {
            return Integer.valueOf(str);
        }
        return 0;
    }
}
