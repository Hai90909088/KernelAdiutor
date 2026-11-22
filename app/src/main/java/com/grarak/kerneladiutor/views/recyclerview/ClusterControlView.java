package com.grarak.kerneladiutor.views.recyclerview;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.grarak.kerneladiutor.R;
import com.grarak.kerneladiutor.views.dialog.Dialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ClusterControlView extends RecyclerViewItem {

    public interface OnGovernorSelectedListener {
        void onGovernorSelected(String governor);
    }

    public interface OnFreqSelectedListener {
        void onFreqSelected(int index);
    }

    private View mTunablesBtn;
    private TextView mClusterTitle;
    private TextView mTunablesLabel;
    private View mGovernorPill;
    private TextView mGovernorText;
    private View mCoresBtn;
    private TextView mCoresValue;

    private View mMinRow;
    private View mMinPill;
    private TextView mMinText;
    private View mMinMinus;
    private SeekBar mMinSeekBar;
    private View mMinPlus;

    private View mMaxRow;
    private View mMaxPill;
    private TextView mMaxText;
    private View mMaxMinus;
    private SeekBar mMaxSeekBar;
    private View mMaxPlus;

    private CharSequence mTitle;
    private boolean mShowTunables = true;
    private boolean mShowCores = true;
    private CharSequence mCoresText;

    private List<String> mGovernors = new ArrayList<>();
    private String mCurrentGovernor;

    private List<String> mFreqLabels = new ArrayList<>();
    private boolean mHasMin = true;
    private boolean mHasMax = true;
    private int mMinIndex = -1;
    private int mMaxIndex = -1;
    private boolean mMinDragging;
    private boolean mMaxDragging;

    private OnGovernorSelectedListener mGovernorListener;
    private View.OnClickListener mTunablesListener;
    private View.OnClickListener mCoresListener;
    private OnFreqSelectedListener mMinListener;
    private OnFreqSelectedListener mMaxListener;
    private Dialog mDialog;

    public static String formatFreqKhz(int khz) {
        if (khz <= 0) return "---";
        if (khz >= 1000000) {
            return String.format(Locale.getDefault(), "%.2fGHz", khz / 1000000f);
        }
        return (khz / 1000) + "MHz";
    }

    public static String formatFreqHz(int rawHz, int offset) {
        if (rawHz <= 0 || offset <= 0) return "---";
        int mhz = rawHz / offset;
        if (mhz >= 1000) {
            return String.format(Locale.getDefault(), "%.2fGHz", mhz / 1000f);
        }
        return mhz + "MHz";
    }

    @Override
    public int getLayoutRes() {
        return R.layout.rv_cluster_control;
    }

    @Override
    public void onRecyclerViewCreate(Activity activity) {
        super.onRecyclerViewCreate(activity);
        if (mDialog != null) {
            mDialog.show();
        }
    }

    @Override
    public void onCreateView(View view) {
        mTunablesBtn = view.findViewById(R.id.tunables_btn);
        mClusterTitle = view.findViewById(R.id.cluster_title);
        mTunablesLabel = view.findViewById(R.id.tunables_label);
        mGovernorPill = view.findViewById(R.id.governor_pill);
        mGovernorText = view.findViewById(R.id.governor_text);
        mCoresBtn = view.findViewById(R.id.cores_btn);
        mCoresValue = view.findViewById(R.id.cores_value);

        mMinRow = view.findViewById(R.id.min_row);
        mMinPill = view.findViewById(R.id.min_pill);
        mMinText = view.findViewById(R.id.min_text);
        mMinMinus = view.findViewById(R.id.min_minus);
        mMinSeekBar = view.findViewById(R.id.min_seekbar);
        mMinPlus = view.findViewById(R.id.min_plus);

        mMaxRow = view.findViewById(R.id.max_row);
        mMaxPill = view.findViewById(R.id.max_pill);
        mMaxText = view.findViewById(R.id.max_text);
        mMaxMinus = view.findViewById(R.id.max_minus);
        mMaxSeekBar = view.findViewById(R.id.max_seekbar);
        mMaxPlus = view.findViewById(R.id.max_plus);

        mTunablesBtn.setOnClickListener(v -> {
            if (mShowTunables && mTunablesListener != null) {
                mTunablesListener.onClick(v);
            }
        });

        mCoresBtn.setOnClickListener(v -> {
            if (mShowCores && mCoresListener != null) {
                mCoresListener.onClick(v);
            }
        });

        mGovernorPill.setOnClickListener(v -> showGovernorDialog(v.getContext()));
        mMinPill.setOnClickListener(v -> showFreqDialog(v.getContext(), true));
        mMaxPill.setOnClickListener(v -> showFreqDialog(v.getContext(), false));

        mMinMinus.setOnClickListener(v -> stepFreq(true, -1));
        mMinPlus.setOnClickListener(v -> stepFreq(true, 1));
        mMaxMinus.setOnClickListener(v -> stepFreq(false, -1));
        mMaxPlus.setOnClickListener(v -> stepFreq(false, 1));

        mMinSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && progress >= 0 && progress < mFreqLabels.size()) {
                    mMinText.setText(mFreqLabels.get(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mMinDragging = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mMinDragging = false;
                applyFreqIndex(true, seekBar.getProgress());
            }
        });

        mMaxSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && progress >= 0 && progress < mFreqLabels.size()) {
                    mMaxText.setText(mFreqLabels.get(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mMaxDragging = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mMaxDragging = false;
                applyFreqIndex(false, seekBar.getProgress());
            }
        });

        super.onCreateView(view);
        setFullSpan(true);
    }

    private void stepFreq(boolean isMin, int delta) {
        if (mFreqLabels.isEmpty()) return;
        int current = isMin ? mMinIndex : mMaxIndex;
        if (current < 0) current = 0;
        int next = Math.max(0, Math.min(mFreqLabels.size() - 1, current + delta));
        if (next != current) {
            applyFreqIndex(isMin, next);
        }
    }

    private void applyFreqIndex(boolean isMin, int index) {
        if (index < 0 || index >= mFreqLabels.size()) return;
        if (isMin) {
            mMinIndex = index;
            if (mMaxIndex >= 0 && index > mMaxIndex) {
                mMaxIndex = index;
            }
            if (mMinListener != null) {
                mMinListener.onFreqSelected(index);
            }
        } else {
            mMaxIndex = index;
            if (mMinIndex >= 0 && index < mMinIndex) {
                mMinIndex = index;
            }
            if (mMaxListener != null) {
                mMaxListener.onFreqSelected(index);
            }
        }
        refresh();
    }

    private void showGovernorDialog(Context context) {
        if (mGovernors.isEmpty()) return;
        String[] items = mGovernors.toArray(new String[0]);
        mDialog = new Dialog(context)
                .setItems(items, (dialog, which) -> {
                    String chosen = items[which];
                    mCurrentGovernor = chosen;
                    refresh();
                    if (mGovernorListener != null) {
                        mGovernorListener.onGovernorSelected(chosen);
                    }
                })
                .setOnDismissListener(dialog -> mDialog = null);
        if (mTitle != null) {
            mDialog.setTitle(mTitle + " - " + context.getString(R.string.governor_short));
        }
        mDialog.show();
    }

    private void showFreqDialog(Context context, boolean isMin) {
        if (mFreqLabels.isEmpty()) return;
        String[] items = mFreqLabels.toArray(new String[0]);
        mDialog = new Dialog(context)
                .setItems(items, (dialog, which) -> applyFreqIndex(isMin, which))
                .setOnDismissListener(dialog -> mDialog = null);
        if (mTitle != null) {
            mDialog.setTitle(mTitle + " - " + context.getString(
                    isMin ? R.string.cpu_min_short : R.string.cpu_max_short));
        }
        mDialog.show();
    }

    public void setTitle(CharSequence title) {
        mTitle = title;
        refresh();
    }

    public void setShowTunables(boolean show) {
        mShowTunables = show;
        refresh();
    }

    public void setOnTunablesClickListener(View.OnClickListener listener) {
        mTunablesListener = listener;
    }

    public void setShowCores(boolean show) {
        mShowCores = show;
        refresh();
    }

    public void setCoresText(CharSequence text) {
        mCoresText = text;
        refresh();
    }

    public void setOnCoresClickListener(View.OnClickListener listener) {
        mCoresListener = listener;
    }

    public void setGovernors(List<String> governors) {
        mGovernors = governors != null ? new ArrayList<>(governors) : new ArrayList<>();
        refresh();
    }

    public void setCurrentGovernor(String governor) {
        mCurrentGovernor = governor;
        if (mGovernorText != null && governor != null && !governor.isEmpty()) {
            mGovernorText.setText(governor);
        }
    }

    public void setOnGovernorSelectedListener(OnGovernorSelectedListener listener) {
        mGovernorListener = listener;
    }

    public void setFreqLabels(List<String> labels) {
        mFreqLabels = labels != null ? new ArrayList<>(labels) : new ArrayList<>();
        refresh();
    }

    public void setHasMin(boolean hasMin) {
        mHasMin = hasMin;
        refresh();
    }

    public void setHasMax(boolean hasMax) {
        mHasMax = hasMax;
        refresh();
    }

    public void setMinIndex(int index) {
        if (mMinDragging) return;
        mMinIndex = index;
        if (mMinSeekBar != null && mFreqLabels.size() > 1 && index >= 0 && index < mFreqLabels.size()) {
            mMinSeekBar.setProgress(index);
            mMinText.setText(mFreqLabels.get(index));
        }
    }

    public void setMaxIndex(int index) {
        if (mMaxDragging) return;
        mMaxIndex = index;
        if (mMaxSeekBar != null && mFreqLabels.size() > 1 && index >= 0 && index < mFreqLabels.size()) {
            mMaxSeekBar.setProgress(index);
            mMaxText.setText(mFreqLabels.get(index));
        }
    }

    public void setOnMinFreqSelectedListener(OnFreqSelectedListener listener) {
        mMinListener = listener;
    }

    public void setOnMaxFreqSelectedListener(OnFreqSelectedListener listener) {
        mMaxListener = listener;
    }

    @Override
    protected void refresh() {
        super.refresh();
        if (mClusterTitle != null) {
            mClusterTitle.setText(mTitle);
        }
        if (mTunablesLabel != null) {
            mTunablesLabel.setVisibility(mShowTunables ? View.VISIBLE : View.GONE);
        }
        if (mCoresBtn != null) {
            mCoresBtn.setVisibility(mShowCores ? View.VISIBLE : View.INVISIBLE);
        }
        if (mCoresValue != null && mCoresText != null) {
            mCoresValue.setText(mCoresText);
        }
        if (mGovernorPill != null) {
            mGovernorPill.setVisibility(mGovernors.isEmpty() ? View.INVISIBLE : View.VISIBLE);
        }
        if (mGovernorText != null) {
            mGovernorText.setText(mCurrentGovernor != null && !mCurrentGovernor.isEmpty()
                    ? mCurrentGovernor : mGovernorText.getContext().getString(R.string.unknown));
        }

        int maxProgress = Math.max(0, mFreqLabels.size() - 1);
        if (mMinRow != null) {
            boolean showMin = mHasMin && !mFreqLabels.isEmpty();
            mMinRow.setVisibility(showMin ? View.VISIBLE : View.GONE);
            if (showMin) {
                mMinSeekBar.setMax(maxProgress);
                if (!mMinDragging && mMinIndex >= 0 && mMinIndex < mFreqLabels.size()) {
                    mMinSeekBar.setProgress(mMinIndex);
                    mMinText.setText(mFreqLabels.get(mMinIndex));
                }
            }
        }
        if (mMaxRow != null) {
            boolean showMax = mHasMax && !mFreqLabels.isEmpty();
            mMaxRow.setVisibility(showMax ? View.VISIBLE : View.GONE);
            if (showMax) {
                mMaxSeekBar.setMax(maxProgress);
                if (!mMaxDragging && mMaxIndex >= 0 && mMaxIndex < mFreqLabels.size()) {
                    mMaxSeekBar.setProgress(mMaxIndex);
                    mMaxText.setText(mFreqLabels.get(mMaxIndex));
                }
            }
        }
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
