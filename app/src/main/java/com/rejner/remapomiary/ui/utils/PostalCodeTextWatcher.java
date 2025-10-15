package com.rejner.remapomiary.ui.utils;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

public class PostalCodeTextWatcher implements TextWatcher {

    private final EditText editText;
    private boolean isEditing;

    public PostalCodeTextWatcher(EditText editText) {
        this.editText = editText;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {}

    @Override
    public void afterTextChanged(Editable s) {
        if (isEditing) return;

        isEditing = true;

        String digits = s.toString().replace("-", "");

        if (digits.length() > 2) {
            digits = digits.substring(0, 2) + "-" + digits.substring(2);
        }

        if (digits.length() > 6) {
            digits = digits.substring(0, 6);
        }

        editText.setText(digits);
        editText.setSelection(digits.length());

        isEditing = false;
    }
}