package org.adw.library.widgets.discreteseekbar;

import android.content.Context;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.AttributeSet;
import android.widget.SeekBar;

public class DiscreteSeekBar extends AppCompatSeekBar {

    public interface OnProgressChangeListener {
        void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser);

        void onStartTrackingTouch(DiscreteSeekBar seekBar);

        void onStopTrackingTouch(DiscreteSeekBar seekBar);
    }

    private int mMin = 0;
    private int mMax = 100;
    private OnProgressChangeListener mListener;

    public DiscreteSeekBar(Context context) {
        this(context, null);
    }

    public DiscreteSeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, android.support.v7.appcompat.R.attr.seekBarStyle);
    }

    public DiscreteSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        super.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mListener != null) {
                    mListener.onProgressChanged(DiscreteSeekBar.this, progress + mMin, fromUser);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (mListener != null) {
                    mListener.onStartTrackingTouch(DiscreteSeekBar.this);
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mListener != null) {
                    mListener.onStopTrackingTouch(DiscreteSeekBar.this);
                }
            }
        });
    }

    public void setOnProgressChangeListener(OnProgressChangeListener listener) {
        mListener = listener;
    }

    @Override
    public synchronized void setMin(int min) {
        int current = getProgress();
        mMin = min;
        super.setMax(Math.max(0, mMax - mMin));
        setProgress(current);
    }

    @Override
    public synchronized void setMax(int max) {
        mMax = max;
        super.setMax(Math.max(0, mMax - mMin));
    }

    @Override
    public synchronized void setProgress(int progress) {
        super.setProgress(Math.max(0, Math.min(progress - mMin, Math.max(0, mMax - mMin))));
    }

    @Override
    public synchronized int getProgress() {
        return super.getProgress() + mMin;
    }
}
