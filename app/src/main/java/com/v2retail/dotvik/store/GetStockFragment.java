package com.v2retail.dotvik.store;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.v2retail.commons.SapJsonObjectRequest;
import com.google.gson.Gson;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.v2retail.ApplicationController;
import com.v2retail.dotvik.R;
import com.v2retail.util.AlertBox;
import com.v2retail.util.CameraCheck;
import com.v2retail.util.RetailAdapter;
import com.v2retail.util.RetailAgeingResponseModal;
import com.v2retail.util.RetailModal;
import com.v2retail.util.RetailModal;
import com.v2retail.util.RetailResponseModal;
import com.v2retail.util.SharedPreferencesData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link GetStockFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link GetStockFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class GetStockFragment extends Fragment implements View.OnClickListener {
	// TODO: Rename parameter arguments, choose names that match
	// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
	private static final String ARG_PARAM1 = "data";
	private static final String ARG_PARAM2 = "param2";


	DecimalFormat df = new DecimalFormat("0.00");
	Context con;
	EditText ean_et;
	ImageButton filter;
	String code;
	private FrameLayout frameLayout;
	private View cardView;
	ProgressDialog dialog;
	// TODO: Rename and change types of parameters
	Button close;
	Button search;
	Button back;
	Button scan;
	Button rescan;
	Button dc;
	Button ageing;
	AlertBox box;
	LinearLayout lay;
	boolean isReloaded = false;
	FragmentManager fm;
	String URL = "";
	String WERKS = "";
	String USER = "";
	String seletedColor = "";
	View view;
	TextView filter_text;
	ArrayList<HashMap<String, String>> dataList;
	ArrayList<RetailModal> filteredData = new ArrayList<>();
	private String TAG = GetStockFragment.class.getName();
	private OnFragmentInteractionListener mListener;
	private RecyclerView recyclerView;
	private RetailAdapter adapter;
	private ArrayList<RetailModal> modal;
	private ArrayList<RetailModal> etData;
	private Set<String> colors = new TreeSet<>();
	public GetStockFragment() {
		// Required empty public constructor
	}

	/**
	 * Use this factory method to create a new instance of
	 * this fragment using the provided parameters.
	 *
	 * @param param1 Parameter 1.
	 * @param param2 Parameter 2.
	 * @return A new instance of fragment OutWardFragment.
	 */
	// TODO: Rename and change types and number of parameters
	public static GetStockFragment newInstance(String param1, String param2) {
		GetStockFragment fragment = new GetStockFragment();
		Bundle args = new Bundle();
		args.putString(ARG_PARAM1, param1);
		args.putString(ARG_PARAM2, param2);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onResume() {
		super.onResume();
		((Home_Activity) getActivity())
				.setActionBarTitle("Retail App");
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getArguments() != null) {
			//  dataList = ( ArrayList<HashMap<String,String>>) getArguments().getSerializable("data");


		}
		fm = getFragmentManager();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		isReloaded = true;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.article_stock_sales_detail, container, false);

		frameLayout = view.findViewById(R.id.frameLayout);
		modal = new ArrayList<>();
		con = getContext();
		dialog = new ProgressDialog(con);
		box = new AlertBox(con);
		SharedPreferencesData data = new SharedPreferencesData(con);


		URL = data.read("URL");
		WERKS = data.read("WERKS");
		USER = data.read("USER");
		if (!URL.isEmpty())
			Log.d(TAG, "URL->" + URL);
		if (!WERKS.isEmpty())
			Log.d(TAG, "WERKS->" + WERKS);
		if (!USER.isEmpty())
			Log.d(TAG, "USER->" + USER);

		scan = (Button) view.findViewById(R.id.scan);
		search = (Button) view.findViewById(R.id.search);
		back = (Button) view.findViewById(R.id.back);
		rescan = (Button) view.findViewById(R.id.rescan);
		ageing = (Button) view.findViewById(R.id.ageing);
		lay = (LinearLayout) view.findViewById(R.id.lay);
		dc = (Button) view.findViewById(R.id.dcstock);
		recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
		ean_et = (EditText) view.findViewById(R.id.ean_et);
		filter = view.findViewById(R.id.filterButton);
		filter_text = view.findViewById(R.id.artile_scan_filter_text);

		scan.setOnClickListener(this);
		search.setOnClickListener(this);
		back.setOnClickListener(this);
		rescan.setOnClickListener(this);
		ageing.setOnClickListener(this);
		filter.setOnClickListener(this);
		dc.setEnabled(false);
		dc.setVisibility(View.GONE);
		//dc.setOnClickListener(this);


		if (lay.getVisibility() == View.VISIBLE)
			lay.setVisibility(View.GONE);


		ean_et.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
				if (actionId == EditorInfo.IME_ACTION_SEARCH) {

					code = ean_et.getText().toString().trim();
					if (TextUtils.isEmpty(code)) {

						box.getBox("Alert", "Enter Barcode No!");
						return true;
					} else {
						InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
						imm.hideSoftInputFromWindow(ean_et.getWindowToken(), 0);

						dialog.setMessage("Please wait...");
						dialog.setCancelable(false);
						dialog.show();

						Handler handler = new Handler();
						handler.postDelayed(new Runnable() {
							@Override
							public void run() {


								try {
									//sendJSONGenericRequest("ZWM_APP_ARTICLE_SA_ST_DETAILS", "ZWM_APP_ARTICLE_SA_ST_DETAILS");
                                    sendJSONGenericRequest("ZWM_APP_ARTICLE_SA_RETAIL_APP", "ZWM_APP_ARTICLE_SA_RETAIL_APP");
								} catch (Exception e) {
									dialog.dismiss();
									box.getErrBox(e);
								}
							}
						}, 2000);

						return true;
					}
				}
				return false;
			}
		});
		ean_et.requestFocus();
		return view;
	}


	@Override
	public void onClick(View view) {
		int id = view.getId();
		switch (id) {

			case R.id.scan:
				if (CameraCheck.isCameraAvailable(con))
					IntentIntegrator.forSupportFragment(GetStockFragment.this).setBeepEnabled(true).setOrientationLocked(true).setTimeout(10000).initiateScan();

				break;
			case R.id.search:
				int actionId = EditorInfo.IME_ACTION_SEARCH;
				ean_et.onEditorAction(actionId);
//				code = ean_et.getText().toString().trim();
//				if (TextUtils.isEmpty(code)) {
//
//					box.getBox("Alert", "Enter Barcode No!");
//					return;
//				}
//
//				dialog.setMessage("Please wait...");
//				dialog.setCancelable(false);
//				dialog.show();
//
//				Handler handler = new Handler();
//				handler.postDelayed(new Runnable() {
//					@Override
//					public void run() {
//						try {
//							sendJSONGenericRequest("ZWM_APP_ARTICLE_SA_ST_DETAILS", "ZWM_APP_ARTICLE_SA_ST_DETAILS");
//
//						} catch (Exception e) {
//							dialog.dismiss();
//							box.getErrBox(e);
//						}
//					}
//				}, 2000);

				break;
			case R.id.dc:
				break;
			case R.id.filterButton:
					if (cardView == null) {
						cardView = LayoutInflater.from(getActivity()).inflate(R.layout.card_overlay, frameLayout, false);
						LinearLayout cardLayout = cardView.findViewById(R.id.card_overlay_layout);
						LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
						filter_text.setText("");
						colors.forEach(color -> {
							Button button = new Button(getActivity());
							button.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
							button.setTextColor(getResources().getColor(R.color.white));
							layoutParams.setMargins(15, 5, 15, 5);
							button.setLayoutParams(layoutParams);
							button.setText(color);
							button.setOnClickListener(new View.OnClickListener() {
								@Override
								public void onClick(View v) {
									seletedColor = ((Button)v).getText().toString();
									frameLayout.removeView(cardView);
									cardView = null;
									filterData(seletedColor);
								}
							});
							cardLayout.addView(button);
						});
						frameLayout.addView(cardView);
					}
				break;
			case R.id.ageing:

				if (TextUtils.isEmpty(code)) {

					code = ean_et.getText().toString();
				}


				dialog.setMessage("Please wait...");
				dialog.setCancelable(false);
				dialog.show();

				Handler handler2 = new Handler();
				handler2.postDelayed(new Runnable() {
					@Override
					public void run() {
						try {
							sendJSONGenericRequest("ZWM_APP_ARTICLE_AGEING", "ZWM_APP_ARTICLE_AGEING");

						} catch (Exception e) {
							dialog.dismiss();
							box.getErrBox(e);
						}
					}
				}, 2000);


				break;
			case R.id.rescan:
				ean_et.setText("");
				if (CameraCheck.isCameraAvailable(con))
					IntentIntegrator.forSupportFragment(GetStockFragment.this).setBeepEnabled(true).setOrientationLocked(true).setTimeout(10000).initiateScan();

				break;
			case R.id.back:
				fm.popBackStack();
				break;
		}
	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		Log.d(TAG, TAG + " scanned result...");
		IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
		if (scanningResult == null) {

			box.getBox("Scanner Err", "Unable to receive Data");

		} else {
			Log.d(TAG, "Scan data received");
			String scanContent = scanningResult.getContents();
			String scanFormat = scanningResult.getFormatName();
			Log.v(TAG, "scanContent = " + scanContent);
			Log.v(TAG, "scanFormat = " + scanFormat);

			if (scanContent != null) {

				ean_et.setText(scanContent);
//				code = scanContent;
//				if (TextUtils.isEmpty(scanContent)) {
//
//					box.getBox("Alert", "Enter Barcode No!");
//					return;
//				}
				int actionId = EditorInfo.IME_ACTION_SEARCH;
				ean_et.onEditorAction(actionId);
//				dialog.setMessage("Please wait...");
//				dialog.setCancelable(false);
//				dialog.show();
//
//				Handler handler = new Handler();
//				handler.postDelayed(new Runnable() {
//					@Override
//					public void run() {
//
//
//						try {
//							sendJSONGenericRequest("ZWM_APP_ARTICLE_SA_ST_DETAILS", "ZWM_APP_ARTICLE_SA_ST_DETAILS");
//
//						} catch (Exception e) {
//							dialog.dismiss();
//							box.getErrBox(e);
//						}
//					}
//				}, 2000);

			} else {
				box.getBox("Scanner Err", "No Content Received. Please Scan Again");

			}
		}


	}

	private void sendJSONGenericRequest(final String opcode, String rfc) {
		final RequestQueue mRequestQueue;
		JsonObjectRequest mJsonRequest = null;
		URL = URL.substring(0, URL.lastIndexOf("/"));
		URL += "/noacljsonrfcadaptor?bapiname=" + rfc + "&aclclientid=android";
		Log.d(TAG, "URL_>" + URL);
		final JSONObject params = new JSONObject();
		try {
			//
			params.put("IM_EAN", code);
			params.put("bapiname", rfc);
			params.put("IM_WERKS", WERKS);
			params.put("IM_GEN", "X");
			params.put("IM_LGNUM", "SDC");
		} catch (JSONException e) {
			e.printStackTrace();
			dialog.dismiss();
			box.getErrBox(e);
		}
		Log.d(TAG, "payload ->" + params.toString());

		mRequestQueue = ApplicationController.getInstance().getRequestQueue();

		mJsonRequest = new SapJsonObjectRequest(Request.Method.POST, URL, params, new Response.Listener<JSONObject>() {


			@Override
			public void onResponse(JSONObject responsebody) {


				dialog.dismiss();
				Log.d(TAG, "response ->" + responsebody);

				if (responsebody == null) {

					box.getBox("Err", "No response from Server");

				} else if (responsebody.equals("") || responsebody.equals("null") || responsebody.equals("{}")) {

					box.getBox("Err", "Unable to Connect Server/ Empty Response");
					return;

				} else {
					try {
						Log.d("tag", "  response ->" + responsebody.toString());
						JSONArray responsearray = null;
						if (responsebody.has("ET_DATA") && responsebody.get("ET_DATA") instanceof JSONArray) {
							responsearray = responsebody.getJSONArray("ET_DATA");
						} else if (responsebody.has("ES_RETURN") && responsebody.get("ES_RETURN") instanceof JSONObject) {
							JSONObject returnobj = responsebody.getJSONObject("ES_RETURN");
							if (returnobj != null) {
								String type = returnobj.getString("TYPE");
								if (type != null)
									if (type.equals("E")) {
										box.getBox("Err", returnobj.getString("MESSAGE"));
										return;
									}
							}
						} else {
							if (responsebody.has("error") && responsebody.get("error") instanceof String) {
								String errormsg = responsebody.getString("error");

								box.getBox("MSG", errormsg.split(":")[0]);

								return;
							}
						}


						switch (opcode) {
							//case "ZWM_APP_ARTICLE_SA_ST_DETAILS":
                            case "ZWM_APP_ARTICLE_SA_RETAIL_APP":
								//setDataView(responsearray);
								setView(responsebody);
								break;
							case "ZWM_APP_ARTICLE_AGEING":
								setAgeing(responsebody);
								break;

						}
						ean_et.setText("");
					} catch (JSONException e) {
						e.printStackTrace();
						box.getErrBox(e);
					}
				}
			}


		}, new Response.ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError error) {

				Log.i(TAG, "Error :" + error.toString());
				String err = "";

				if (error instanceof TimeoutError || error instanceof NoConnectionError) {
					err = "Communication Error!";

				} else if (error instanceof AuthFailureError) {
					err = "Authentication Error!";
				} else if (error instanceof ServerError) {
					err = "Server Side Error!";
				} else if (error instanceof NetworkError) {
					err = "Network Error!";
				} else if (error instanceof ParseError) {
					err = "Parse Error!";
				} else err = error.toString();

				dialog.dismiss();
				box.getBox("Err", err);
			}
		}) {
			@Override
			public String getBodyContentType() {
				return "application/json";
			}

			@Override
			public byte[] getBody() {
				return params.toString().getBytes();
			}


			@Override
			protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {

				Response<JSONObject> res = super.parseNetworkResponse(response);
				Log.d(TAG, "Network response -> " + res.toString());

				return res;
			}


		};
		mJsonRequest.setRetryPolicy(new RetryPolicy() {
			@Override
			public int getCurrentTimeout() {
				return 50000;
			}

			@Override
			public int getCurrentRetryCount() {
				return DefaultRetryPolicy.DEFAULT_MAX_RETRIES;
			}

			@Override
			public void retry(VolleyError error) throws VolleyError {

			}
		});
		mRequestQueue.add(mJsonRequest);
		Log.d(TAG, "jsonRequest getUrl ->" + mJsonRequest.getUrl());
		Log.d(TAG, "jsonRequest getBodyContentType->" + mJsonRequest.getBodyContentType());
		Log.d(TAG, "jsonRequest getBody->" + mJsonRequest.getBody().toString());
		Log.d(TAG, "jsonRequest getMethod->" + mJsonRequest.getMethod());
		try {
			Log.d(TAG, "jsonRequest getHeaders->" + mJsonRequest.getHeaders());
		} catch (AuthFailureError authFailureError) {
			authFailureError.printStackTrace();
			dialog.dismiss();
			box.getErrBox(authFailureError);

		}

	}


	public void setAgeing(JSONObject responsearray) throws JSONException {

		ArrayList<RetailModal.Ageing> modal2 = new ArrayList<>();

		if (responsearray != null && responsearray.length() > 1) {


			Gson gson = new Gson();

			RetailAgeingResponseModal modal = gson.fromJson(responsearray.toString(), RetailAgeingResponseModal.class);
			ArrayList<RetailModal.Ageing> rModal = modal.getAgeingArrayList();

			if (rModal == null) {
				box.getBox("Err", "No Ageing Found For barcode " + code);

				return;
			}
			rModal.remove(0);

			Log.d(TAG, "setAgeing Ageing list ################: "+modal2);


			isReloaded = true;
			Ageing_Fragment ageing_fragment = new Ageing_Fragment();
			Bundle bundle = new Bundle();
			bundle.putSerializable("ageingList", rModal);
			ageing_fragment.setArguments(bundle);
			FragmentTransaction ft = fm.beginTransaction();
			ft.add(R.id.home, ageing_fragment);
			ft.addToBackStack("Retail_fragment");
			ft.commit();


		} else {
			box.getBox("Err", "No Ageing Found For barcode " + code);
		}


	}


	public void setView(JSONObject responsearray) {

		if (modal != null)
			modal.clear();
		if (adapter != null)
			adapter.notifyDataSetChanged();

		Gson gson = new Gson();

		RetailResponseModal responseModal = gson.fromJson(responsearray.toString(), RetailResponseModal.class);
		Log.d(TAG, "setView: " + responseModal);


		Double totalStock = 0.0;
		Double totalStckFloor = 0.0;
		Double td = 0.0;
		Double mtd = 0.0;
		Double totalStckSTR = 0.0;
		colors = new TreeSet<>();
		colors.add("ALL");
		filter.setVisibility(View.INVISIBLE);
		if (responseModal != null && responseModal.getRetailModals().size() > 1) {
			modal = responseModal.getRetailModals();
			etData = responseModal.getRetailEtData();
			for (int i = 1; i < modal.size(); i++)
			{
				colors.add(modal.get(i).getColor());
				totalStock = getStockTotal(modal.get(i));
//				totalStckFloor = getSFTotal(modal.get(i));
				totalStckSTR = getSSTotal(modal.get(i));

				td = modal.get(i).getTd();
				mtd = modal.get(i).getMtd();
				//to remove zeros..from begining
				modal.get(i).setArticle(modal.get(i).getArticle());//sales - 7 args
				modal.get(i).setTotal(df.format(totalStock));//sales - 7 args
				modal.get(i).setStr_mtd(calST(mtd, totalStckSTR));
				//modal.get(i).setStr_mtdf(calST(mtd, totalStckFloor));
				//modal.get(i).setStr_td(calST(td, totalStckSTR));
				//modal.get(i).setSTR_TDf(calST(td, totalStckFloor));

				totalStock = 0.0;
				totalStckFloor = 0.0;
				td = 0.0;
				mtd = 0.0;
				totalStckSTR = 0.0;

			}
			if(colors.size() > 0){
				filter.setVisibility(View.VISIBLE);
			}
			modal.remove(0);
			if (modal.get(0).getArticle().length() != 10)
				calTS();
			filter_text.setText("Found : "+(modal.size())+" records");
			etData.remove(0);
			adapter = new RetailAdapter(con, modal);

			recyclerView.setAdapter(adapter);
			adapter.notifyDataSetChanged();
			ean_et.setText("");
			if (lay.getVisibility() == View.GONE)
				lay.setVisibility(View.VISIBLE);

		} else {
			box.getBox("Err", "No Data Found For barcode " + ean_et.getText().toString());
		}


	}
	private void filterData(String color){
		filteredData = new ArrayList<>();
		if(color.equals("ALL")){
			filteredData = etData;
			filter_text.setText("Found : "+(etData.size())+" records");
		}else{
			etData.stream().forEach(data ->{
				if(data.getColor().equals(color)){
					filteredData.add(data);
				}
			});
			filter_text.setText("Color : "+color+" ("+filteredData.size()+")");
		}
		if(filteredData.size() > 0){

			adapter = new RetailAdapter(con, filteredData);
			recyclerView.setAdapter(adapter);
			adapter.notifyDataSetChanged();
		}
	}

	private void calTS() {

		RetailModal genericModal = null;

		if (modal != null) {
			genericModal = new RetailModal();
			for (int i = 0; i < modal.size(); i++) {
				genericModal.setIntransit(df.format(genericModal.getIntransit() + modal.get(i).getIntransit()));
				genericModal.setVo1(df.format(genericModal.getVo1() + modal.get(i).getVo1()));
				genericModal.setOne(df.format(genericModal.getOne() + modal.get(i).getOne()));
				genericModal.setThree(df.format(genericModal.getThree() + modal.get(i).getThree()));
				genericModal.setMsa(df.format(genericModal.getMsa() + modal.get(i).getMsa()));
				genericModal.setTd(df.format(genericModal.getTd() + modal.get(i).getTd()));
				genericModal.setYtd(df.format(genericModal.getYtd() + modal.get(i).getYtd()));
				genericModal.setMtd(df.format(genericModal.getMtd() + modal.get(i).getMtd()));
				genericModal.setLmtd(df.format(genericModal.getLmtd() + modal.get(i).getLmtd()));
				genericModal.setTotal(df.format(genericModal.getTotal() + modal.get(i).getTotal()));


			}
			Double storeStck = getSSTotal(genericModal);
			Double floorStock = getSFTotal(genericModal);
			//genericModal.setSTR_TDf(calST(genericModal.getTd(), floorStock));
			//genericModal.setStr_td(calST(genericModal.getTd(), storeStck));
			genericModal.setStr_mtd(calST(genericModal.getMtd(), storeStck));
			//genericModal.setStr_mtdf(calST(genericModal.getMtd(), floorStock));

			int ind = modal.size() - 1;

			genericModal.setMatDesc(modal.get(ind).getMatDesc().split(",")[0]);
			genericModal.setMerchandise(modal.get(ind).getMerchandise());

			if (modal.get(ind).getArticle().length() > 10) {
				genericModal.setArticle(modal.get(ind).getArticle().substring(0, modal.get(ind).getArticle().length() - 3));
				modal.add(0, genericModal);
			} else {
				genericModal.setArticle(modal.get(ind).getArticle() + "001");
				modal.add(genericModal);
			}


		}


	}


	private String calST(double td, double totalStock) {

		if (td != 0.00) {
			double temp = totalStock / td;

			return df.format(temp).replace("∞", "-").replace("Infinity", "-").replace("nan", "-").replace("NAN", "-");
		} else return "0.00";
	}

	private double getStockTotal(RetailModal modal) {

		if (modal == null) return 0.00;
		double temp = 0.0;
		temp += modal.getVo1();
		temp += modal.getIntransit();
		temp += modal.getOne();
		temp += modal.getVo4();
		temp += modal.getThree();
		temp += modal.getMsa();

		return temp;
	}


	private Double getSSTotal(RetailModal modal) {
		Double temp = 0.0;

		temp += modal.getVo1();
		temp += modal.getOne();
		temp += modal.getVo4();
		temp += modal.getMsa();

		return temp;
	}


	private Double getSFTotal(RetailModal modal) {
		Double temp = 0.0;
		temp += modal.getOne();
		return temp;
	}


	public void onButtonPressed(Uri uri) {
		if (mListener != null) {
			mListener.onFragmentInteraction(uri);
		}
		//fm.popBackStackImmediate();
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		if (context instanceof OnFragmentInteractionListener) {
			mListener = (OnFragmentInteractionListener) context;
		} else {
			throw new RuntimeException(context.toString()
					+ " must implement OnFragmentInteractionListener");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;
	}

	/**
	 * This interface must be implemented by activities that contain this
	 * fragment to allow an interaction in this fragment to be communicated
	 * to the activity and potentially other fragments contained in that
	 * activity.
	 * <p>
	 * See the Android Training lesson <a href=
	 * "http://developer.android.com/training/basics/fragments/communicating.html"
	 * >Communicating with Other Fragments</a> for more information.
	 */
	public interface OnFragmentInteractionListener {
		// TODO: Update argument type and name
		void onFragmentInteraction(Uri uri);
	}
}
