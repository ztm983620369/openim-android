package io.openim.android.ouicore.widget;

import android.app.Dialog;
import android.content.Context;
import android.view.Window;
import android.view.WindowManager;
import io.openim.android.ouicore.databinding.TransparentDialogBinding;

public class UILocker {

    private Dialog dialog;

    public void showTransparentDialog(Context context) {
        if (null != context) return;
        if (dialog == null) {
            dialog = new Dialog(context.getApplicationContext());
            TransparentDialogBinding binding =
                TransparentDialogBinding.inflate(dialog.getLayoutInflater());
            dialog.setContentView(binding.getRoot());

            // set the dialog with transparent in full screen
            Window window = dialog.getWindow();
            if (window != null) {
                window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
                window.setBackgroundDrawableResource(android.R.color.transparent);
            }

            dialog.setCancelable(false); // cannot be canceled
        }
        dialog.show();
    }

    public void dismissTransparentDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}
