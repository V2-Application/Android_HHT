package com.v2retail.dotvik.hub.inward;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.v2retail.dotvik.BuildConfig;
import com.v2retail.dotvik.R;
import com.v2retail.dotvik.hub.HubProcessSelectionActivity;
import com.v2retail.util.AlertBox;
import com.v2retail.util.SharedPreferencesData;

public class MenuHubInward extends Fragment implements View.OnClickListener {

    private static final String TAG = "MenuHubInward";

    private View rootView;
    private FragmentActivity activity;
    FragmentManager fm;
    Context con;
    ProgressDialog dialog;
    AlertBox box;

    Button hu_grc;
    Button hu_stock_review;
    Button hu_v11_v01;      // V11-V01 — ZWM_HU_STOCK_REV_RFC with Type=V11

    private MenuHubInward.OnFragmentInteractionListener mListener;

    public MenuHubInward() {}

    public static MenuHubInward newInstance(String param1, String param2) {
        return new MenuHubInward();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getParentFragmentManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.hub_inward_menu, container, false);
        con = getContext();

        hu_grc          = rootView.findViewById(R.id.hub_inward_hu_grc);
        hu_stock_review = rootView.findViewById(R.id.hub_inward_hu_stock_review);
        hu_v11_v01      = rootView.findViewById(R.id.hub_inward_v11_v01);

        hu_grc.setOnClickListener(this);
        hu_stock_review.setOnClickListener(this);
        hu_v11_v01.setOnClickListener(this);

        // Environment/role-driven visibility for V11-V01 button
        applyV11V01Visibility();

        return rootView;
    }

    /**
     * Controls V11-V01 button visibility using a layered policy.
     *
     * <p>Resolution order (any match → visible):
     *
     * <ol>
     *   <li>Build-time flag {@link BuildConfig#ENABLE_V11_V01} — default {@code true}.
     *       When {@code true}, V11-V01 shows in every build (dev + prod). Flip to
     *       {@code false} in {@code app/build.gradle} to gate production by host
     *       / role instead.</li>
     *   <li>DEV host override — if the device's configured SAP URL contains
     *       {@link BuildConfig#SAP_DEV_HOST}, V11-V01 shows regardless of the
     *       build flag (dev-machine escape hatch).</li>
     *   <li>Role / permission — implicit. This fragment only loads for users
     *       whose SAP {@code EX_GROUP="HUB"} (enforced by LoginActivity routing).
     *       Additional granular API-driven perm checks hook in here.</li>
     * </ol>
     */
    private void applyV11V01Visibility() {
        boolean flagOn = BuildConfig.ENABLE_V11_V01;
        boolean devHost = isDevHostConfigured();
        boolean rolePerm = hasV11V01Permission();

        boolean show = flagOn || devHost || rolePerm;
        hu_v11_v01.setVisibility(show ? View.VISIBLE : View.GONE);

        Log.d(TAG, "V11-V01 visibility → " + (show ? "VISIBLE" : "GONE")
                + "  [flag=" + flagOn
                + ", devHost=" + devHost
                + ", role=" + rolePerm + "]");
    }

    /** @return true when the device's URL points at the DEV SAP host. */
    private boolean isDevHostConfigured() {
        try {
            if (BuildConfig.SAP_DEV_HOST == null || BuildConfig.SAP_DEV_HOST.isEmpty()) {
                return false;
            }
            SharedPreferencesData data = new SharedPreferencesData(con);
            String url = data.read("URL");
            return url != null && url.contains(BuildConfig.SAP_DEV_HOST);
        } catch (Exception e) {
            Log.w(TAG, "isDevHostConfigured check failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Optional role/permission gate. Currently returns false because the API
     * exposes coarse {@code EX_GROUP} only (already checked at login routing).
     * Extend this when the backend surfaces a granular "V11_V01_ACCESS" perm
     * — read it from SharedPreferences / an authorised endpoint here.
     */
    private boolean hasV11V01Permission() {
        // Placeholder — returns false so the flag/host layers are authoritative.
        // Example future implementation:
        //   SharedPreferencesData data = new SharedPreferencesData(con);
        //   String perms = data.read("PERMISSIONS");
        //   return perms != null && perms.contains("V11_V01");
        return false;
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
        if (getFragmentManager().getBackStackEntryCount() == 1) {
            box.getDialogBox(getActivity());
        } else {
            fm.popBackStack();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof MenuHubInward.OnFragmentInteractionListener) {
            mListener = (MenuHubInward.OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        ((HubProcessSelectionActivity) getActivity()).setActionBarTitle("HUB Inward");
    }

    @Override
    public void onClick(View view) {
        setFragment(view.getId());
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }

    public void setFragment(int fragmentID) {
        Fragment fragment = null;
        switch (fragmentID) {
            case R.id.hub_inward_hu_grc:
                fragment = new FragmentHUGRC();
                break;
            case R.id.hub_inward_hu_stock_review:
                fragment = new FragmentHUStockReview();
                break;
            case R.id.hub_inward_v11_v01:
                // Guard: never route if button was hidden (defensive)
                if (hu_v11_v01.getVisibility() != View.VISIBLE) return;
                fragment = new FragmentHUStockReviewV11();
                break;
        }
        if (fragment != null) {
            FragmentTransaction ft = fm.beginTransaction();
            ft.replace(R.id.home, fragment);
            ft.addToBackStack("hub_menu_inward");
            ft.commit();
        }
    }
}
