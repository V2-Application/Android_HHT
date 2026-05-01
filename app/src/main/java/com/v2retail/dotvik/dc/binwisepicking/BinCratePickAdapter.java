package com.v2retail.dotvik.dc.binwisepicking;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.v2retail.dotvik.R;
import com.v2retail.dotvik.dc.ptlnew.BinCrateHU;
import com.v2retail.util.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for MSA Binwise picking bin/crate/qty rows (replaces heavy TableLayout adds).
 */
public class BinCratePickAdapter extends RecyclerView.Adapter<BinCratePickAdapter.RowVH> {

    private final List<BinCrateHU> rows = new ArrayList<>();

    public void submitRows(List<BinCrateHU> newRows) {
        rows.clear();
        if (newRows != null) {
            rows.addAll(newRows);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RowVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_msa_binwise_bin_crate_row, parent, false);
        return new RowVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RowVH holder, int position) {
        BinCrateHU data = rows.get(position);
        holder.tvSno.setText(String.valueOf(position + 1));
        holder.tvBin.setText(data.getBin());
        holder.tvCrate.setText(data.getCrate());
        holder.tvQty.setText(Util.convertToDoubleString(data.getQty()));
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class RowVH extends RecyclerView.ViewHolder {
        final TextView tvSno;
        final TextView tvBin;
        final TextView tvCrate;
        final TextView tvQty;

        RowVH(@NonNull View itemView) {
            super(itemView);
            tvSno = itemView.findViewById(R.id.tv_row_sno);
            tvBin = itemView.findViewById(R.id.tv_row_bin);
            tvCrate = itemView.findViewById(R.id.tv_row_crate);
            tvQty = itemView.findViewById(R.id.tv_row_qty);
        }
    }
}
