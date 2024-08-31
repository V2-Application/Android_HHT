package com.v2retail.dotvik.dc;

import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import com.google.android.material.navigation.NavigationView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.v2retail.dotvik.R;
//import com.v2retail.dotvik.ScannerActivity;
import com.v2retail.dotvik.dc.grt.GRTProcessFragment;
import com.v2retail.dotvik.dc.ptl.PTLProcessFragment;
import com.v2retail.util.AlertBox;


public class Process_Selection_Activity extends AppCompatActivity
        implements View.OnKeyListener,
        DC_DashBoard.OnFragmentInteractionListener,
        NavigationView.OnNavigationItemSelectedListener,
        InwardFragment.OnFragmentInteractionListener,
        Stock_In_Out_Fragment.OnFragmentInteractionListener,
        ValidateCrate_Process_Fragment.OnFragmentInteractionListener,
        TO_Creation_Fragment.OnFragmentInteractionListener,
        OutWardFragment.OnFragmentInteractionListener,
        Scan_Packing_Material_Fragment.OnFragmentInteractionListener,
        Picking_Delivery_Scan_Fragment.OnFragmentInteractionListener,
        HU_Detail_Fragment.OnFragmentInteractionListener,
        HU_Material_Scan_Fragment.OnFragmentInteractionListener,
        Stock_Take_Process_Fragment.OnFragmentInteractionListener ,
        Scan_Stock_take_Fragment.OnFragmentInteractionListener,
        PTLProcessFragment.OnFragmentInteractionListener,
        GRTProcessFragment.OnFragmentInteractionListener,
        OutwardHUWeightFragment.OnFragmentInteractionListener,
        MenuFragmentInwardTVSPaperLess.OnFragmentInteractionListener
{

    String URL="";
    String WERKS="";
    String USER="";
    private static final String TAG = Process_Selection_Activity.class.getName();
    Toolbar toolbar;
    AlertBox box;
    FragmentManager fm;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home_dc);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        fm=getSupportFragmentManager();
        box=new AlertBox(getApplicationContext());
        Log.d(TAG, TAG + " created");
        int backStackEntry = getSupportFragmentManager().getBackStackEntryCount();
        Log.e("count", "backStackEntry = " + backStackEntry);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        addDashbaord();

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        FragmentManager fm = getSupportFragmentManager();
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }
        if (fm.getBackStackEntryCount() == 1){

           box.getDialogBox(Process_Selection_Activity.this);

        }else if (fm.getBackStackEntryCount() >1){
            AlertBox box1 = new AlertBox(Process_Selection_Activity.this);
            box1.getBox("Alert", "Do you want to go back.", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // fm.popBackStack();
                    //  ApplicationController.getInstance().refreshObservable().notifyObservers();

                    fm.popBackStack();

                }
            }, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // negative

                }
            });
        }
        else {
            super.onBackPressed();
        }



    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home_, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        setFragment(id);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
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
            case R.id.nav_stocktake_frag:
                fragment = new Stock_Take_Process_Fragment();
                break;


        }
        clearStack();
        if (fragment != null) {
            Log.d(TAG, TAG + " fragment created");
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
        Fragment newFragment = new DC_DashBoard();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.home, newFragment, "DC_Dashboard");
        ft.addToBackStack("DC_Dashboard");
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
