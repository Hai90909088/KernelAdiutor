package com.grarak.kerneladiutor.views.recyclerview;

import android.app.Activity;
import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
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

    private View mFreqPill;
    private TextView mFreqText;

    private View mFreqSliderRow;
    private View mFreqMinus;
    private SeekBar mFreqSeekBar;
    private View mFreqPlus;

    private CharSequence mTitle;
    private boolean mShowTunables = true;
    private boolean mShowCores = true;
    private CharSequence mCoresText;

    private List<String> mGovernors = new ArrayList<>();
    private String mCurrentGovernor;

    private List<String> mFreqLabels = new ArrayList<>();
    private int mFreqIndex = -1;
    private boolean mDragging;

    private OnGovernorSelectedListener mGovernorListener;
    private View.OnClickListener mTunablesListener;
    private View.OnClickListener mCoresListener;
    private OnFreqSelectedListener mFreqListener;
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

        mFreqPill = view.findViewById(R.id.freq_pill);
        mFreqText = view.findViewById(R.id.freq_text);

        mFreqSliderRow = view.findViewById(R.id.freq_slider_row);
        mFreqMinus = view.findViewById(R.id.freq_minus);
        mFreqSeekBar = view.findViewById(R.id.freq_seekbar);
        mFreqPlus = view.findViewById(R.id.freq_plus);

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
        mFreqPill.setOnClickListener(v -> showFreqDialog(v.getContext()));

        mFreqMinus.setOnClickListener(v -> stepFreq(-1));
        mFreqPlus.setOnClickListener(v -> stepFreq(1));

        mFreqSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && progress >= 0 && progress < mFreqLabels.size()) {
                    mFreqText.setText(mFreqLabels.get(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mDragging = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mDragging = false;
                applyFreqIndex(seekBar.getProgress());
            }
        });

        mFreqSeekBar.setOnTouchListener((v, event) -> {
            ViewParent parent = v.getParent();
            if (parent != null) {
                int action = event.getActionMasked();
                parent.requestDisallowInterceptTouchEvent(
                        action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE);
            }
            return false;
        });

        super.onCreateView(view);
        setFullSpan(true);
    }

    private void stepFreq(int delta) {
        if (mFreqLabels.isEmpty()) return;
        int current = mFreqIndex;
        if (current < 0) current = 0;
        int next = Math.max(0, Math.min(mFreqLabels.size() - 1, current + delta));
        if (next != current) {
            applyFreqIndex(next);
        }
    }

    private void applyFreqIndex(int index) {
        if (index < 0 || index >= mFreqLabels.size()) return;
        mFreqIndex = index;
        if (mFreqListener != null) {
            mFreqListener.onFreqSelected(index);
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

    private void showFreqDialog(Context context) {
        if (mFreqLabels.isEmpty()) return;
        String[] items = mFreqLabels.toArray(new String[0]);
        mDialog = new Dialog(context)
                .setItems(items, (dialog, which) -> applyFreqIndex(which))
                .setOnDismissListener(dialog -> mDialog = null);
        if (mTitle != null) {
            mDialog.setTitle(mTitle + " - " + context.getString(R.string.frequencies));
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

    public void setFreqIndex(int index) {
        if (mDragging) return;
        mFreqIndex = index;
        syncSliderAndLabels();
    }

    public void setOnFreqSelectedListener(OnFreqSelectedListener listener) {
        mFreqListener = listener;
    }

    private void syncSliderAndLabels() {
        int maxProgress = Math.max(0, mFreqLabels.size() - 1);
        if (mFreqIndex < 0 || mFreqIndex > maxProgress) {
            mFreqIndex = maxProgress;
        }
        if (mFreqText != null && !mFreqLabels.isEmpty() && mFreqIndex >= 0 && mFreqIndex < mFreqLabels.size()) {
            mFreqText.setText(mFreqLabels.get(mFreqIndex));
        }
        if (mFreqSeekBar != null) {
            mFreqSeekBar.setMax(maxProgress);
            if (!mDragging) {
                mFreqSeekBar.setProgress(mFreqIndex);
            }
        }
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

        boolean hasFreqs = !mFreqLabels.isEmpty();
        if (mFreqPill != null) {
            mFreqPill.setVisibility(hasFreqs ? View.VISIBLE : View.GONE);
        }
        if (mFreqSliderRow != null) {
            mFreqSliderRow.setVisibility(hasFreqs ? View.VISIBLE : View.GONE);
        }

        if (hasFreqs) {
            syncSliderAndLabels();
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
