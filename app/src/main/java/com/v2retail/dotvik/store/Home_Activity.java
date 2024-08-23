package com.v2retail.dotvik.store;

import android.content.Intent;
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
import android.view.Menu;
import android.view.MenuItem;

import com.v2retail.dotvik.R;
import com.v2retail.util.AlertBox;


public class Home_Activity extends AppCompatActivity
        implements OutboundFragment.OnFragmentInteractionListener,
        InboundFragment.OnFragmentInteractionListener,
        StockTakeFragment.OnFragmentInteractionListener,
        GRT_From_DisplayFragment.OnFragmentInteractionListener,
        GRT_From_MSAFragment.OnFragmentInteractionListener,
        MenuFragmentStore.OnFragmentInteractionListener,
        MenuFragmentStoreDisplay.OnFragmentInteractionListener,
        MenuFragmentStoreDisplayInbound.OnFragmentInteractionListener,
        MenuFragmentStoreDisplayOutbound.OnFragmentInteractionListener,
        MenuFragmentStoreDisplayInernal.OnFragmentInteractionListener,
        DashBoard.OnFragmentInteractionListener,
        GRC_Putway_Fragment.OnFragmentInteractionListener,
        HU_GRC_Process_Fragment.OnFragmentInteractionListener,
        Scan_HU_GRC_Fragment.OnFragmentInteractionListener,
        Floor_Putway_Fragment.OnFragmentInteractionListener,
        Direct_Picking_Fragment.OnFragmentInteractionListener,
        NavigationView.OnNavigationItemSelectedListener,
        Picking_Against_Picklist_Fragment.OnFragmentInteractionListener,
        Sloc_To_Sloc_without_WM_Fragment.OnFragmentInteractionListener,
        Bin_To_Bin_Transfer_Fragment.OnFragmentInteractionListener,
        Scan_Floor_putway_Process_Fragment.OnFragmentInteractionListener,
        Scan_Direct__picking_Process_Fragment.OnFragmentInteractionListener,
        Scan_GRT_Display_Fragment.OnFragmentInteractionListener,
        Scan_Stock_Take_Process_Fragment.OnFragmentInteractionListener,
        Scan_GRT_MSA_Fragment.OnFragmentInteractionListener,
        Scan_Bin_To_BinTransfer_Process_Fragment.OnFragmentInteractionListener,
        Scan_SlocToSloc_Without_WM_Process_Fragment.OnFragmentInteractionListener,
        Scan_Picking_against_picking_Process_Fragment.OnFragmentInteractionListener,
        Issue_Fire.OnFragmentInteractionListener,
        Scan_grc_putway_Process_Fragment.OnFragmentInteractionListener,
        Scan_TRFDispToProc_Fragment.OnFragmentInteractionListener,
        TRFDispToProc.OnFragmentInteractionListener,
        Article_Sales_Fragment.OnFragmentInteractionListener,
        Article_Sales_Detail_Fragment.OnFragmentInteractionListener,
        Article_Sale_Scan_Fragment.OnFragmentInteractionListener,
        Article_Sales_Variant_Fragment.OnFragmentInteractionListener,
        Article_Scan_Fragment.OnFragmentInteractionListener,
        Article_Detail_Variant_Fragment.OnFragmentInteractionListener,
        Article_Detail_Fragment.OnFragmentInteractionListener,
        Retail_App_Fragment.OnFragmentInteractionListener,
        GetStockFragment.OnFragmentInteractionListener,
        Ageing_Fragment.OnFragmentInteractionListener,
        Stock_Details_Fragment.OnFragmentInteractionListener,
        StockTakeV2_Fragment.OnFragmentInteractionListener,
        Stock_Take_DetailV2_Fragment.OnFragmentInteractionListener,
        Scan_Stock_take_V2Fragment.OnFragmentInteractionListener,
        DisplayAreaProcessFragment.OnFragmentInteractionListener,
        EcommFragmentProcess.OnFragmentInteractionListener,
        MenuFragmentStoreDisplayInernalIRODToIROD.OnFragmentInteractionListener {

    String werks;
    String user;
    AlertBox box;
    private static final String TAG = Home_Activity.class.getName();
    Toolbar toolbar;
    FragmentManager fm;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home_);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        box = new AlertBox(Home_Activity.this);
        fm=getSupportFragmentManager();
        Log.d(TAG, TAG + " created");

        Intent in = getIntent();

        if (in != null) {
            werks = in.getStringExtra("werks");
            user = in.getStringExtra("user");

            Log.d(TAG, " login : " + werks);
            Log.d(TAG, " werks : " + user);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);

        navigationView.setNavigationItemSelectedListener(this);

        try {
            addDashbaord();
        } catch (Exception e) {
            box.getErrBox(e);
        }

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        FragmentManager fm = getSupportFragmentManager();
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }
        if (fm.getBackStackEntryCount() == 1){

            box.getDialogBox(Home_Activity.this);

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


        try {
            setFragment(id);
        } catch (Exception e) {
            box.getErrBox(e);
        }


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
            case R.id.nav_inbound:
                fragment = new InboundFragment();
                break;
            case R.id.nav_outbound:
                fragment = new OutboundFragment();
                break;
            case R.id.nav_retailApp:
              //  fragment = new Retail_App_Fragment();
                fragment = new GetStockFragment(); //new
                break;
            case R.id.nav_stocktake:
                fragment = new StockTakeFragment();
                break;
                //home
            case R.id.nav_home:
                clearStack();


        }
        clearStack();
        if (fragment != null) {
            Log.d(TAG, TAG + " fragment created");
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.home, fragment, "Home");
            ft.addToBackStack("Home");
            ft.commit();

        }

        if (fragmentID == R.id.nav_home) {
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
        Fragment newFragment = new MenuFragmentStore();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(R.id.home, newFragment, "StoreDashboard");
        ft.addToBackStack("StoreDashboard");
        ft.commit();
    }
}
