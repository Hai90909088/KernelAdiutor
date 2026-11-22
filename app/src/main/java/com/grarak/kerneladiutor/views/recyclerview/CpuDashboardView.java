package com.grarak.kerneladiutor.views.recyclerview;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.grarak.kerneladiutor.R;

import java.util.ArrayList;
import java.util.List;

public class CpuDashboardView extends RecyclerViewItem {

    public static class BarRow {
        public String label;
        public int percent;
        public String valueText;

        public BarRow(String label, int percent, String valueText) {
            this.label = label;
            this.percent = percent;
            this.valueText = valueText;
        }
    }

    private View mOverallContainer;
    private ProgressBar mOverallProgress;
    private TextView mOverallText;
    private LinearLayout mBarsContainer;

    private boolean mShowOverall = true;
    private int mOverallPercent;
    private String mOverallLabel;
    private final List<BarRow> mRows = new ArrayList<>();

    @Override
    public int getLayoutRes() {
        return R.layout.rv_cpu_dashboard;
    }

    @Override
    public void onCreateView(View view) {
        mOverallContainer = view.findViewById(R.id.overall_container);
        mOverallProgress = view.findViewById(R.id.overall_progress);
        mOverallText = view.findViewById(R.id.overall_text);
        mBarsContainer = view.findViewById(R.id.cluster_bars_container);
        super.onCreateView(view);
        setFullSpan(true);
    }

    public void setShowOverall(boolean showOverall) {
        mShowOverall = showOverall;
        refresh();
    }

    public void setOverallUsage(int percent) {
        setOverallUsage(percent, Math.max(0, Math.min(100, percent)) + "%");
    }

    public void setOverallUsage(int percent, String label) {
        mOverallPercent = Math.max(0, Math.min(100, percent));
        mOverallLabel = label;
        refresh();
    }

    public void setRows(List<BarRow> rows) {
        mRows.clear();
        if (rows != null) {
            mRows.addAll(rows);
        }
        rebuildRows();
        refresh();
    }

    public void updateRow(int index, int percent, String valueText) {
        if (index < 0 || index >= mRows.size()) return;
        BarRow row = mRows.get(index);
        row.percent = Math.max(0, Math.min(100, percent));
        row.valueText = valueText;
        if (mBarsContainer != null && index < mBarsContainer.getChildCount()) {
            View child = mBarsContainer.getChildAt(index);
            ProgressBar pb = child.findViewById(R.id.bar_progress);
            TextView val = child.findViewById(R.id.bar_value);
            if (pb != null) pb.setProgress(row.percent);
            if (val != null) val.setText(row.valueText);
        }
    }

    private void rebuildRows() {
        if (mBarsContainer == null) return;
        if (mRows.isEmpty()) {
            mBarsContainer.setVisibility(View.GONE);
            mBarsContainer.removeAllViews();
            return;
        }
        mBarsContainer.setVisibility(View.VISIBLE);
        if (mBarsContainer.getChildCount() != mRows.size()) {
            mBarsContainer.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(mBarsContainer.getContext());
            for (int i = 0; i < mRows.size(); i++) {
                View rowView = inflater.inflate(R.layout.item_cluster_bar, mBarsContainer, false);
                mBarsContainer.addView(rowView);
            }
        }
        for (int i = 0; i < mRows.size(); i++) {
            View rowView = mBarsContainer.getChildAt(i);
            BarRow row = mRows.get(i);
            TextView label = rowView.findViewById(R.id.bar_label);
            ProgressBar pb = rowView.findViewById(R.id.bar_progress);
            TextView val = rowView.findViewById(R.id.bar_value);
            if (label != null) label.setText(row.label);
            if (pb != null) pb.setProgress(row.percent);
            if (val != null) val.setText(row.valueText);
        }
    }

    @Override
    protected void refresh() {
        super.refresh();
        if (mOverallContainer != null) {
            mOverallContainer.setVisibility(mShowOverall ? View.VISIBLE : View.GONE);
        }
        if (mOverallProgress != null) {
            mOverallProgress.setProgress(mOverallPercent);
        }
        if (mOverallText != null) {
            mOverallText.setText(mOverallLabel != null ? mOverallLabel : (mOverallPercent + "%"));
        }
        rebuildRows();
    }

    @Override
    protected boolean cardCompatible() {
        return false;
    }

    @Override
    boolean cacheable() {
        return true;
    }
}
