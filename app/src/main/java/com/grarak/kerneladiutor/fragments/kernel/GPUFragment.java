/*
 * Copyright (C) 2015-2016 Willi Ye <williye97@gmail.com>
 *
 * This file is part of Kernel Adiutor.
 *
 * Kernel Adiutor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Kernel Adiutor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Kernel Adiutor.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.grarak.kerneladiutor.fragments.kernel;

import com.grarak.kerneladiutor.R;
import com.grarak.kerneladiutor.fragments.ApplyOnBootFragment;
import com.grarak.kerneladiutor.fragments.BaseFragment;
import com.grarak.kerneladiutor.fragments.recyclerview.RecyclerViewFragment;
import com.grarak.kerneladiutor.utils.kernel.gpu.AdrenoIdler;
import com.grarak.kerneladiutor.utils.kernel.gpu.GPUFreq;
import com.grarak.kerneladiutor.utils.kernel.gpu.SimpleGPU;
import com.grarak.kerneladiutor.views.recyclerview.CardView;
import com.grarak.kerneladiutor.views.recyclerview.ClusterControlView;
import com.grarak.kerneladiutor.views.recyclerview.CpuDashboardView;
import com.grarak.kerneladiutor.views.recyclerview.RecyclerViewItem;
import com.grarak.kerneladiutor.views.recyclerview.SeekBarView;
import com.grarak.kerneladiutor.views.recyclerview.SelectView;
import com.grarak.kerneladiutor.views.recyclerview.SwitchView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by willi on 12.05.16.
 */
public class GPUFragment extends RecyclerViewFragment {

    private GPUFreq mGPUFreq;

    private CpuDashboardView mDashboard;
    private ClusterControlView mGpuControl;
    private int mGpuRowIndex = -1;
    private int mGpu2dRowIndex = -1;

    private PathReaderFragment mGPUGovernorTunableFragment;

    @Override
    protected boolean showViewPager() {
        return false;
    }

    @Override
    protected BaseFragment getForegroundFragment() {
        return mGPUGovernorTunableFragment = new PathReaderFragment();
    }

    @Override
    protected void init() {
        super.init();

        mGPUFreq = GPUFreq.getInstance();
    }

    @Override
    protected void addItems(List<RecyclerViewItem> items) {
        freqAndGovernorInit(items);
        if (SimpleGPU.supported()) {
            simpleGpuInit(items);
        }
        if (AdrenoIdler.supported()) {
            adrenoIdlerInit(items);
        }
    }

    private void freqAndGovernorInit(List<RecyclerViewItem> items) {
        mGpuRowIndex = -1;
        mGpu2dRowIndex = -1;

        List<Integer> freqs = mGPUFreq.getAvailableFreqs();
        List<Integer> freqs2d = mGPUFreq.get2dAvailableFreqs();

        mDashboard = new CpuDashboardView();
        mDashboard.setShowOverall(mGPUFreq.hasBusy());
        List<CpuDashboardView.BarRow> rows = new ArrayList<>();
        if (mGPUFreq.hasCurFreq() && freqs != null && !freqs.isEmpty()) {
            mGpuRowIndex = rows.size();
            int cur = mGPUFreq.getCurFreq();
            int max = freqs.get(freqs.size() - 1);
            int pct = max > 0 ? Math.round((float) cur / max * 100f) : 0;
            rows.add(new CpuDashboardView.BarRow(getString(R.string.gpu), pct,
                    ClusterControlView.formatFreqHz(cur, mGPUFreq.getCurFreqOffset())));
        }
        if (mGPUFreq.has2dCurFreq() && freqs2d != null && !freqs2d.isEmpty()) {
            mGpu2dRowIndex = rows.size();
            int cur = mGPUFreq.get2dCurFreq();
            int max = freqs2d.get(freqs2d.size() - 1);
            int pct = max > 0 ? Math.round((float) cur / max * 100f) : 0;
            rows.add(new CpuDashboardView.BarRow("2D", pct,
                    ClusterControlView.formatFreqHz(cur, 1000000)));
        }
        mDashboard.setRows(rows);
        if (mGPUFreq.hasBusy() || !rows.isEmpty()) {
            setPinnedHeader(mDashboard);
        } else {
            setPinnedHeader(null);
        }

        if (mGPUFreq.hasGovernor() || mGPUFreq.hasMaxFreq() || mGPUFreq.hasMinFreq()) {
            mGpuControl = new ClusterControlView();
            mGpuControl.setTitle(getString(R.string.gpu));
            mGpuControl.setShowCores(false);

            String currentGov = mGPUFreq.hasGovernor() ? mGPUFreq.getGovernor() : "";
            boolean hasTunables = mGPUFreq.hasGovernor() && mGPUFreq.hasTunables(currentGov);
            mGpuControl.setShowTunables(hasTunables);
            mGpuControl.setOnTunablesClickListener(v -> {
                String governor1 = mGPUFreq.getGovernor();
                setForegroundText(governor1);
                mGPUGovernorTunableFragment.setError(getString(R.string.tunables_error, governor1));
                mGPUGovernorTunableFragment.setPath(mGPUFreq.getTunables(governor1), ApplyOnBootFragment.GPU);
                showForeground();
            });

            if (mGPUFreq.hasGovernor()) {
                mGpuControl.setGovernors(mGPUFreq.getAvailableGovernors());
                mGpuControl.setCurrentGovernor(currentGov);
                mGpuControl.setOnGovernorSelectedListener(governor ->
                        mGPUFreq.setGovernor(governor, getActivity()));
            }

            if (freqs != null && !freqs.isEmpty()) {
                int offset = mGPUFreq.hasMaxFreq() ? mGPUFreq.getMaxFreqOffset()
                        : (mGPUFreq.hasMinFreq() ? mGPUFreq.getMinFreqOffset() : mGPUFreq.getCurFreqOffset());
                List<String> freqLabels = new ArrayList<>();
                for (int freq : freqs) {
                    freqLabels.add(ClusterControlView.formatFreqHz(freq, offset));
                }
                mGpuControl.setFreqLabels(freqLabels);
                int maxF = mGPUFreq.hasMaxFreq() ? mGPUFreq.getMaxFreq() : mGPUFreq.getCurFreq();
                mGpuControl.setFreqIndex(freqs.indexOf(maxF));
                mGpuControl.setOnFreqSelectedListener(idx ->
                        mGPUFreq.setMaxFreq(freqs.get(idx), getActivity()));
            }

            items.add(mGpuControl);
        }

        if ((mGPUFreq.has2dMaxFreq() && freqs2d != null) || mGPUFreq.has2dGovernor()) {
            CardView gpu2dCard = new CardView();
            gpu2dCard.setTitle(getString(R.string.gpu_2d_freq));

            if (mGPUFreq.has2dMaxFreq() && freqs2d != null) {
                SelectView max2dFreq = new SelectView();
                max2dFreq.setTitle(getString(R.string.gpu_2d_max_freq));
                max2dFreq.setSummary(getString(R.string.gpu_2d_max_freq_summary));
                max2dFreq.setItems(mGPUFreq.get2dAdjustedFreqs(getActivity()));
                max2dFreq.setItem((mGPUFreq.get2dMaxFreq() / 1000000) + getString(R.string.mhz));
                max2dFreq.setOnItemSelected((selectView, position, item)
                        -> mGPUFreq.set2dMaxFreq(freqs2d.get(position), getActivity()));
                gpu2dCard.addItem(max2dFreq);
            }

            if (mGPUFreq.has2dGovernor()) {
                SelectView governor2d = new SelectView();
                governor2d.setTitle(getString(R.string.gpu_2d_governor));
                governor2d.setSummary(getString(R.string.gpu_2d_governor_summary));
                governor2d.setItems(mGPUFreq.get2dAvailableGovernors());
                governor2d.setItem(mGPUFreq.get2dGovernor());
                governor2d.setOnItemSelected((selectView, position, item)
                        -> mGPUFreq.set2dGovernor(item, getActivity()));
                gpu2dCard.addItem(governor2d);
            }

            if (gpu2dCard.size() > 0) {
                items.add(gpu2dCard);
            }
        }
    }

    private void simpleGpuInit(List<RecyclerViewItem> items) {
        CardView card = new CardView();
        card.setTitle(getString(R.string.simple_gpu_algorithm));

        if (SimpleGPU.hasSimpleGpuEnable()) {
            SwitchView enable = new SwitchView();
            enable.setTitle(getString(R.string.simple_gpu_algorithm));
            enable.setSummary(getString(R.string.simple_gpu_algorithm_summary));
            enable.setChecked(SimpleGPU.isSimpleGpuEnabled());
            enable.addOnSwitchListener((switchView, isChecked)
                    -> SimpleGPU.enableSimpleGpu(isChecked, getActivity()));

            card.addItem(enable);
        }

        if (SimpleGPU.hasSimpleGpuLaziness()) {
            SeekBarView laziness = new SeekBarView();
            laziness.setTitle(getString(R.string.laziness));
            laziness.setSummary(getString(R.string.laziness_summary));
            laziness.setMax(10);
            laziness.setProgress(SimpleGPU.getSimpleGpuLaziness());
            laziness.setOnSeekBarListener(new SeekBarView.OnSeekBarListener() {
                @Override
                public void onMove(SeekBarView seekBarView, int position, String value) {
                }

                @Override
                public void onStop(SeekBarView seekBarView, int position, String value) {
                    SimpleGPU.setSimpleGpuLaziness(position, getActivity());
                }
            });

            card.addItem(laziness);
        }

        if (SimpleGPU.hasSimpleGpuRampThreshold()) {
            SeekBarView rampThreshold = new SeekBarView();
            rampThreshold.setTitle(getString(R.string.ramp_thresold));
            rampThreshold.setSummary(getString(R.string.ramp_thresold_summary));
            rampThreshold.setMax(10);
            rampThreshold.setProgress(SimpleGPU.getSimpleGpuRampThreshold());
            rampThreshold.setOnSeekBarListener(new SeekBarView.OnSeekBarListener() {
                @Override
                public void onMove(SeekBarView seekBarView, int position, String value) {
                }

                @Override
                public void onStop(SeekBarView seekBarView, int position, String value) {
                    SimpleGPU.setSimpleGpuRampThreshold(position, getActivity());
                }
            });

            card.addItem(rampThreshold);
        }

        if (card.size() > 0) {
            items.add(card);
        }
    }

    private void adrenoIdlerInit(List<RecyclerViewItem> items) {
        CardView card = new CardView();
        card.setTitle(getString(R.string.adreno_idler));

        if (AdrenoIdler.hasAdrenoIdlerEnable()) {
            SwitchView enable = new SwitchView();
            enable.setTitle(getString(R.string.adreno_idler));
            enable.setSummary(getString(R.string.adreno_idler_summary));
            enable.setChecked(AdrenoIdler.isAdrenoIdlerEnabled());
            enable.addOnSwitchListener((switchView, isChecked)
                    -> AdrenoIdler.enableAdrenoIdler(isChecked, getActivity()));

            card.addItem(enable);
        }

        if (AdrenoIdler.hasAdrenoIdlerDownDiff()) {
            SeekBarView downDiff = new SeekBarView();
            downDiff.setTitle(getString(R.string.down_differential));
            downDiff.setSummary(getString(R.string.down_differential_summary));
            downDiff.setMax(99);
            downDiff.setProgress(AdrenoIdler.getAdrenoIdlerDownDiff());
            downDiff.setOnSeekBarListener(new SeekBarView.OnSeekBarListener() {
                @Override
                public void onMove(SeekBarView seekBarView, int position, String value) {
                }

                @Override
                public void onStop(SeekBarView seekBarView, int position, String value) {
                    AdrenoIdler.setAdrenoIdlerDownDiff(position, getActivity());
                }
            });

            card.addItem(downDiff);
        }

        if (AdrenoIdler.hasAdrenoIdlerIdleWait()) {
            SeekBarView idleWait = new SeekBarView();
            idleWait.setTitle(getString(R.string.idle_wait));
            idleWait.setSummary(getString(R.string.idle_wait_summary));
            idleWait.setMax(99);
            idleWait.setProgress(AdrenoIdler.getAdrenoIdlerIdleWait());
            idleWait.setOnSeekBarListener(new SeekBarView.OnSeekBarListener() {
                @Override
                public void onMove(SeekBarView seekBarView, int position, String value) {
                }

                @Override
                public void onStop(SeekBarView seekBarView, int position, String value) {
                    AdrenoIdler.setAdrenoIdlerIdleWait(position, getActivity());
                }
            });

            card.addItem(idleWait);
        }

        if (AdrenoIdler.hasAdrenoIdlerIdleWorkload()) {
            SeekBarView idleWorkload = new SeekBarView();
            idleWorkload.setTitle(getString(R.string.workload));
            idleWorkload.setSummary(getString(R.string.workload_summary));
            idleWorkload.setMax(10);
            idleWorkload.setMin(1);
            idleWorkload.setProgress(AdrenoIdler.getAdrenoIdlerIdleWorkload() - 1);
            idleWorkload.setOnSeekBarListener(new SeekBarView.OnSeekBarListener() {
                @Override
                public void onMove(SeekBarView seekBarView, int position, String value) {
                }

                @Override
                public void onStop(SeekBarView seekBarView, int position, String value) {
                    AdrenoIdler.setAdrenoIdlerIdleWorkload(position + 1, getActivity());
                }
            });

            card.addItem(idleWorkload);
        }

        if (card.size() > 0) {
            items.add(card);
        }
    }

    private Integer m2dFreq;
    private List<Integer> m2dFreqs;
    private Integer mBusy;
    private Integer mFreq;
    private List<Integer> mFreqs;

    @Override
    protected void refreshThread() {
        super.refreshThread();

        m2dFreq = mGPUFreq.get2dCurFreq();
        m2dFreqs = mGPUFreq.get2dAvailableFreqs();
        mBusy = mGPUFreq.hasBusy() ? mGPUFreq.getBusy() : null;
        mFreq = mGPUFreq.getCurFreq();
        mFreqs = mGPUFreq.getAvailableFreqs();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void refresh() {
        super.refresh();

        if (mDashboard != null) {
            if (mBusy != null) {
                int load = Math.max(0, Math.min(100, mBusy));
                mDashboard.setOverallUsage(load);
            }
            if (mGpuRowIndex >= 0 && mFreq != null && mFreqs != null && !mFreqs.isEmpty()) {
                float maxFreq = mFreqs.get(mFreqs.size() - 1);
                int pct = maxFreq > 0 ? Math.round((float) mFreq / maxFreq * 100f) : 0;
                mDashboard.updateRow(mGpuRowIndex, pct,
                        ClusterControlView.formatFreqHz(mFreq, mGPUFreq.getCurFreqOffset()));
            }
            if (mGpu2dRowIndex >= 0 && m2dFreq != null && m2dFreqs != null && !m2dFreqs.isEmpty()) {
                float maxFreq = m2dFreqs.get(m2dFreqs.size() - 1);
                int pct = maxFreq > 0 ? Math.round((float) m2dFreq / maxFreq * 100f) : 0;
                mDashboard.updateRow(mGpu2dRowIndex, pct,
                        ClusterControlView.formatFreqHz(m2dFreq, 1000000));
            }
        }
    }
}
