package com.v2retail.dotvik.ho;

import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.navigation.NavigationView;
import com.v2retail.dotvik.R;
import com.v2retail.util.AlertBox;

public class HoProcessSelectionActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        HoMenu.OnFragmentInteractionListener {

    private static final String TAG = HoProcessSelectionActivity.class.getName();
    Toolbar toolbar;
    AlertBox box;
    FragmentManager fm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ho_process_selection);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        fm = getSupportFragmentManager();
        box = new AlertBox(getApplicationContext());
        DrawerLayout drawer = findViewById(R.id.drawer_ho_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        setActionBarTitle("HO Process");
        NavigationView navigationView = findViewById(R.id.nav_ho_view);
        navigationView.setNavigationItemSelectedListener(this);
        if (savedInstanceState == null) {
            addDashboard();
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_ho_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }
        if (fm.getBackStackEntryCount() == 1) {
            box.getDialogBox(HoProcessSelectionActivity.this);
        } else if (fm.getBackStackEntryCount() > 1) {
            AlertBox box1 = new AlertBox(HoProcessSelectionActivity.this);
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
        } else {
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
        if (item.getItemId() == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.nav_home_frag) {
            addDashboard();
        }
        DrawerLayout drawer = findViewById(R.id.drawer_ho_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    public void setActionBarTitle(String title) {
        getSupportActionBar().setTitle(title);
    }

    public void clearStack() {
        if (fm != null) {
            int count = fm.getBackStackEntryCount();
            if (count > 1) {
                fm.popBackStackImmediate();
            }
        }
    }

    void addDashboard() {
        clearStack();
        Fragment newFragment = new HoMenu();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.home, newFragment, "Ho_process_selection");
        ft.addToBackStack("Ho_process_selection");
        ft.commit();
    }
}
