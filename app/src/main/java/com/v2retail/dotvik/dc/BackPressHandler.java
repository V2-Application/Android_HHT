package com.v2retail.dotvik.dc;

import android.content.Context;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

public final class BackPressHandler {

    private BackPressHandler() {
    }

    public static void registerCloseProcessBackPress(Fragment fragment, Runnable onConfirm) {
        fragment.requireActivity().getOnBackPressedDispatcher().addCallback(
                fragment,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        showCloseProcessDialog(fragment.requireContext(), onConfirm);
                    }
                });
    }

    public static void confirmCloseProcess(FragmentManager fragmentManager, Context context) {
        showCloseProcessDialog(context, fragmentManager::popBackStack);
    }

    public static void showCloseProcessDialog(Context context, Runnable onConfirm) {
        new AlertDialog.Builder(context)
                .setTitle("Alert")
                .setMessage("Do you want to close the process?")
                .setCancelable(false)
                .setPositiveButton("Yes", (dialog, which) -> onConfirm.run())
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
