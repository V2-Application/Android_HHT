package com.v2retail.dotvik.hub;

import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.material.navigation.NavigationView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.v2retail.dotvik.R;
import com.v2retail.dotvik.dc.DC_DashBoard;
import com.v2retail.dotvik.dc.InwardFragment;
import com.v2retail.dotvik.dc.OutWardFragment;
import com.v2retail.dotvik.hub.inward.MenuHubInward;
import com.v2retail.dotvik.hub.outward.MenuHubOutward;
import com.v2retail.util.AlertBox;

public class HubProcessSelectionActivity extends AppCompatActivity implements
        View.OnKeyListener,
        NavigationView.OnNavigationItemSelectedListener,
        HubMenu.OnFragmentInteractionListener,
        MenuHubInward.OnFragmentInteractionListener,
        MenuHubOutward.OnFragmentInteractionListener {

    private static final String TAG = HubProcessSelectionActivity.class.getName();
    Toolbar toolbar;
    AlertBox box;
    FragmentManager fm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hub_process_selection);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        fm=getSupportFragmentManager();
        box=new AlertBox(getApplicationContext());
        int backStackEntry = getSupportFragmentManager().getBackStackEntryCount();
        DrawerLayout drawer = findViewById(R.id.drawer_hub_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        setActionBarTitle("HUB Process");
        NavigationView navigationView = findViewById(R.id.nav_hub_view);
        navigationView.setNavigationItemSelectedListener(this);
        if (savedInstanceState == null) {
            addDashbaord();
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_hub_layout);
        FragmentManager fm = getSupportFragmentManager();
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }
        if (fm.getBackStackEntryCount() == 1){

            box.getDialogBox(HubProcessSelectionActivity.this);

        }else if (fm.getBackStackEntryCount() >1){
            AlertBox box1 = new AlertBox(HubProcessSelectionActivity.this);
            box1.getBox("Alert", "Do you want to go back.", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    fm.popBackStack();
                }
            }, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
        }
        else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home_, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        setFragment(id);
        DrawerLayout drawer = findViewById(R.id.drawer_hub_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    public void setActionBarTitle(String title) {
        getSupportActionBar().setTitle(title);
    }

    public void setFragment(int fragmentID) {
        Fragment fragment = null;
        switch (fragmentID) {
            case R.id.nav_inward:
                fragment = new InwardFragment();
                break;
            case R.id.nav_outward:
                fragment = new OutWardFragment();
                break;
        }
        clearStack();
        if (fragment != null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.home, fragment, "Home");
            ft.addToBackStack("Home");
            ft.commit();

        }
        if (fragmentID == R.id.nav_home_frag) {
            addDashbaord();
        }
    }

    public void clearStack() {
        if(fm!=null)
        {int count=fm.getBackStackEntryCount();
            if (count > 1) {
                fm.popBackStackImmediate();
            }

        }
    }
    void addDashbaord() {
        clearStack();
        Fragment newFragment = new HubMenu();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.home, newFragment, "Hub_process_selection");
        ft.addToBackStack("Hub_process_selection");
        ft.commit();
    }

    @Override
    public boolean onKey(View view, int keyCode, KeyEvent event) {
        if ((event.getAction() == KeyEvent.ACTION_DOWN)
                && (keyCode == KeyEvent.KEYCODE_ENTER)) {
            Log.d(TAG,"---------enter clicked-----------");
            return    true;
        }
        return false;
    }
}