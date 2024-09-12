package com.emicasolutions.eventdetector;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class LoadingDialog extends DialogFragment {
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = new Dialog(requireContext(), R.style.ProgressBarDialog);
        dialog.setContentView(R.layout.loading_dialog);
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }
}
