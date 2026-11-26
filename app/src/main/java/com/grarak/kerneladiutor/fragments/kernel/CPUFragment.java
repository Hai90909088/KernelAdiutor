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
import com.grarak.kerneladiutor.utils.Log;
import com.grarak.kerneladiutor.utils.Utils;
import com.grarak.kerneladiutor.utils.ViewUtils;
import com.grarak.kerneladiutor.utils.kernel.cpu.CPUBoost;
import com.grarak.kerneladiutor.utils.kernel.cpu.CPUFreq;
import com.grarak.kerneladiutor.utils.kernel.cpu.Misc;
import com.grarak.kerneladiutor.views.dialog.Dialog;
import com.grarak.kerneladiutor.views.recyclerview.CardView;
import com.grarak.kerneladiutor.views.recyclerview.ClusterControlView;
import com.grarak.kerneladiutor.views.recyclerview.CpuDashboardView;
import com.grarak.kerneladiutor.views.recyclerview.DescriptionView;
import com.grarak.kerneladiutor.views.recyclerview.RecyclerViewItem;
import com.grarak.kerneladiutor.views.recyclerview.SeekBarView;
import com.grarak.kerneladiutor.views.recyclerview.SelectView;
import com.grarak.kerneladiutor.views.recyclerview.SwitchView;
import com.grarak.kerneladiutor.views.recyclerview.TitleView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by willi on 01.05.16.
 */
public class CPUFragment extends RecyclerViewFragment {

    private static final String TAG = CPUFragment.class.getSimpleName();

    private CPUFreq mCPUFreq;
    private CPUBoost mCPUBoost;

    private CpuDashboardView mDashboard;
    private final List<ClusterHolder> mClusterHolders = new ArrayList<>();
    private final List<SelectView> mScreenOffSelects = new ArrayList<>();
    private final List<CPUFreq.Cluster> mScreenOffClusters = new ArrayList<>();

    private PathReaderFragment mGovernorTunableFragment;
    private Dialog mGovernorTunableErrorDialog;
    private Dialog mCoresDialog;

    private float[] mCPUUsages;

    private static class ClusterHolder {
        final CPUFreq.Cluster cluster;
        final ClusterControlView control;
        final List<Integer> freqs;
        int curFreq;
        int minFreq;
        int maxFreq;
        String governor;
        int screenOffFreq;

        ClusterHolder(CPUFreq.Cluster cluster, ClusterControlView control, List<Integer> freqs) {
            this.cluster = cluster;
            this.control = control;
            this.freqs = freqs;
        }
    }

    @Override
    protected boolean showViewPager() {
        return false;
    }

    @Override
    protected BaseFragment getForegroundFragment() {
        return mGovernorTunableFragment = new PathReaderFragment();
    }

    @Override
    protected void init() {
        super.init();

        mCPUFreq = CPUFreq.getInstance(getActivity());
        mCPUBoost = CPUBoost.getInstance();

        if (mGovernorTunableErrorDialog != null) {
            mGovernorTunableErrorDialog.show();
        }
        if (mCoresDialog != null) {
            mCoresDialog.show();
        }
    }

    @Override
    protected void addItems(List<RecyclerViewItem> items) {
        Log.crashlyticsI(TAG, "freqInit");
        freqInit(items);
        if (Misc.hasMcPowerSaving()) {
            mcPowerSavingInit(items);
        }
        if (Misc.hasCFSScheduler()) {
            cfsSchedulerInit(items);
        }
        if (Misc.hasCpuQuiet()) {
            cpuQuietInit(items);
        }
        if (mCPUBoost.supported()) {
            cpuBoostInit(items);
        }
        if (Misc.hasCpuTouchBoost()) {
            cpuTouchBoostInit(items);
        }
    }

    private void freqInit(List<RecyclerViewItem> items) {
        mClusterHolders.clear();
        mScreenOffSelects.clear();
        mScreenOffClusters.clear();

        List<CPUFreq.Cluster> clusters = mCPUFreq.getClusters();

        mDashboard = new CpuDashboardView();
        mDashboard.setShowOverall(true);
        List<CpuDashboardView.BarRow> barRows = new ArrayList<>();
        for (CPUFreq.Cluster cluster : clusters) {
            String label = getString(R.string.cluster_label, cluster.index + 1);
            int curFreq = readClusterCurFreq(cluster);
            List<Integer> freqs = mCPUFreq.getFreqs(cluster.firstCpu);
            int maxFreq = (freqs != null && !freqs.isEmpty()) ? freqs.get(freqs.size() - 1) : 0;
            int pct = (curFreq > 0 && maxFreq > 0) ? Math.round((float) curFreq / maxFreq * 100f) : 0;
            String val = curFreq > 0 ? ClusterControlView.formatFreqKhz(curFreq) : getString(R.string.offline);
            barRows.add(new CpuDashboardView.BarRow(label, pct, val));
        }
        mDashboard.setRows(barRows);
        setPinnedHeader(mDashboard);

        CardView screenOffCard = new CardView();
        screenOffCard.setTitle(getString(R.string.cpu_max_screen_off_freq));

        for (final CPUFreq.Cluster cluster : clusters) {
            final List<Integer> freqs = mCPUFreq.getFreqs(cluster.firstCpu);
            List<String> freqLabels = new ArrayList<>();
            if (freqs != null) {
                for (int freq : freqs) {
                    freqLabels.add(ClusterControlView.formatFreqKhz(freq));
                }
            }

            ClusterControlView control = new ClusterControlView();
            control.setTitle(getString(R.string.cluster_label, cluster.index + 1));
            control.setShowTunables(true);
            control.setOnTunablesClickListener(v -> showGovernorTunables(cluster.firstCpu, cluster.lastCpu));
            control.setShowCores(true);
            control.setCoresText(cluster.getCoresDisplay());
            control.setOnCoresClickListener(v -> showCoresDialog(cluster));

            List<String> govs = mCPUFreq.getGovernors(cluster.firstCpu);
            control.setGovernors(govs);
            String curGov = mCPUFreq.getGovernor(cluster.firstCpu, false);
            control.setCurrentGovernor(curGov);
            control.setOnGovernorSelectedListener(governor ->
                    mCPUFreq.setGovernor(governor, cluster.firstCpu, cluster.lastCpu, getActivity()));

            control.setFreqLabels(freqLabels);
            boolean hasFreqs = freqs != null && !freqs.isEmpty();
            if (hasFreqs) {
                int maxF = mCPUFreq.getMaxFreq(cluster.firstCpu, false);
                control.setFreqIndex(findClosestFreqIndex(freqs, maxF));
                control.setOnFreqSelectedListener(position ->
                        mCPUFreq.setMaxFreq(freqs.get(position), cluster.firstCpu, cluster.lastCpu, getActivity()));
            }

            ClusterHolder holder = new ClusterHolder(cluster, control, freqs);
            holder.governor = curGov;
            mClusterHolders.add(holder);
            items.add(control);

            if (mCPUFreq.hasMaxScreenOffFreq(cluster.firstCpu) && hasFreqs) {
                SelectView screenOff = new SelectView();
                screenOff.setTitle(getString(R.string.cluster_label, cluster.index + 1));
                screenOff.setSummary(getString(R.string.cpu_max_screen_off_freq_summary));
                screenOff.setItems(freqLabels);
                screenOff.setOnItemSelected((selectView, position, item) ->
                        mCPUFreq.setMaxScreenOffFreq(freqs.get(position), cluster.firstCpu, cluster.lastCpu, getActivity()));
                screenOffCard.addItem(screenOff);
                mScreenOffSelects.add(screenOff);
                mScreenOffClusters.add(cluster);
            }
        }

        if (screenOffCard.size() > 0) {
            items.add(screenOffCard);
        }
    }

    private int readClusterCurFreq(CPUFreq.Cluster cluster) {
        for (int cpu : cluster.cpus) {
            int freq = mCPUFreq.getCurFreq(cpu);
            if (freq > 0) return freq;
        }
        return 0;
    }

    private int findClosestFreqIndex(List<Integer> freqs, int target) {
        if (freqs == null || freqs.isEmpty() || target <= 0) return -1;
        int exact = freqs.indexOf(target);
        if (exact >= 0) return exact;
        int bestIdx = 0;
        int bestDiff = Math.abs(freqs.get(0) - target);
        for (int i = 1; i < freqs.size(); i++) {
            int diff = Math.abs(freqs.get(i) - target);
            if (diff < bestDiff) {
                bestDiff = diff;
                bestIdx = i;
            }
        }
        return bestIdx;
    }

    private void showCoresDialog(final CPUFreq.Cluster cluster) {
        if (getActivity() == null) return;
        String[] labels = new String[cluster.cpus.size()];
        for (int i = 0; i < cluster.cpus.size(); i++) {
            int core = cluster.cpus.get(i);
            int freq = mCPUFreq.getCurFreq(core);
            String state = freq == 0 ? getString(R.string.offline) : ClusterControlView.formatFreqKhz(freq);
            labels[i] = getString(R.string.core, core + 1) + "  •  " + state;
        }

        mCoresDialog = new Dialog(getActivity())
                .setTitle(getString(R.string.cores_toggle_title, cluster.getCoresDisplay()))
                .setItems(labels, (dialog, which) -> {
                    int core = cluster.cpus.get(which);
                    if (core == 0) {
                        Utils.toast(R.string.no_offline_core, getActivity());
                    } else {
                        boolean currentlyOnline = mCPUFreq.getCurFreq(core) != 0;
                        mCPUFreq.onlineCpu(core, !currentlyOnline, true, getActivity());
                    }
                })
                .setOnDismissListener(dialog -> mCoresDialog = null);
        mCoresDialog.show();
    }

    private void showGovernorTunables(int min, int max) {
        boolean offline = mCPUFreq.isOffline(min);
        if (offline) {
            mCPUFreq.onlineCpu(min, true, false, null);
        }
        String governor = mCPUFreq.getGovernor(min, false);
        if (governor.isEmpty()) {
            mGovernorTunableErrorDialog = ViewUtils.dialogBuilder(getString(R.string.cpu_governor_tunables_read_error),
                    null,
                    (dialog, which) -> {
                    },
                    dialog -> mGovernorTunableErrorDialog = null, getActivity());
            mGovernorTunableErrorDialog.show();
        } else {
            setForegroundText(governor);
            mGovernorTunableFragment.setError(getString(R.string.tunables_error, governor));
            mGovernorTunableFragment.setPath(mCPUFreq.getGovernorTunablesPath(min, governor), min, max,
                    ApplyOnBootFragment.CPU);
            showForeground();
        }
        if (offline) {
            mCPUFreq.onlineCpu(min, false, false, null);
        }
    }

    private void mcPowerSavingInit(List<RecyclerViewItem> items) {
        SelectView mcPowerSaving = new SelectView();
        mcPowerSaving.setTitle(getString(R.string.mc_power_saving));
        mcPowerSaving.setSummary(getString(R.string.mc_power_saving_summary));
        mcPowerSaving.setItems(Arrays.asList(getResources().getStringArray(R.array.mc_power_saving_items)));
        mcPowerSaving.setItem(Misc.getCurMcPowerSaving());
        mcPowerSaving.setOnItemSelected((selectView, position, item)
                -> Misc.setMcPowerSaving(position, getActivity()));

        items.add(mcPowerSaving);
    }

    private void cfsSchedulerInit(List<RecyclerViewItem> items) {
        SelectView cfsScheduler = new SelectView();
        cfsScheduler.setTitle(getString(R.string.cfs_scheduler_policy));
        cfsScheduler.setSummary(getString(R.string.cfs_scheduler_policy_summary));
        cfsScheduler.setItems(Misc.getAvailableCFSSchedulers());
        cfsScheduler.setItem(Misc.getCurrentCFSScheduler());
        cfsScheduler.setOnItemSelected((selectView, position, item)
                -> Misc.setCFSScheduler(item, getActivity()));

        items.add(cfsScheduler);
    }

    private void cpuQuietInit(List<RecyclerViewItem> items) {
        List<RecyclerViewItem> views = new ArrayList<>();
        CardView cpuQuietCard = new CardView();
        cpuQuietCard.setTitle(getString(R.string.cpu_quiet));

        if (Misc.hasCpuQuietEnable()) {
            SwitchView cpuQuietEnable = new SwitchView();
            cpuQuietEnable.setSummary(getString(R.string.cpu_quiet));
            cpuQuietEnable.setChecked(Misc.isCpuQuietEnabled());
            cpuQuietEnable.addOnSwitchListener((switchView, isChecked)
                    -> Misc.enableCpuQuiet(isChecked, getActivity()));

            views.add(cpuQuietEnable);
        }

        if (Misc.hasCpuQuietGovernors()) {
            SelectView cpuQuietGovernors = new SelectView();
            cpuQuietGovernors.setSummary(getString(R.string.cpu_quiet_governor));
            cpuQuietGovernors.setItems(Misc.getCpuQuietAvailableGovernors());
            cpuQuietGovernors.setItem(Misc.getCpuQuietCurGovernor());
            cpuQuietGovernors.setOnItemSelected((selectView, position, item)
                    -> Misc.setCpuQuietGovernor(item, getActivity()));

            views.add(cpuQuietGovernors);
        }

        if (!views.isEmpty()) {
            DescriptionView descriptionView = new DescriptionView();
            descriptionView.setSummary(getString(R.string.cpu_quiet_summary));
            cpuQuietCard.addItem(descriptionView);

            for (RecyclerViewItem item : views) {
                cpuQuietCard.addItem(item);
            }
            items.add(cpuQuietCard);
        }
    }

    private void cpuBoostInit(List<RecyclerViewItem> items) {
        List<RecyclerViewItem> cpuBoost = new ArrayList<>();

        TitleView title = new TitleView();
        title.setText(getString(R.string.cpu_boost));

        if (mCPUBoost.hasEnable()) {
            SwitchView enable = new SwitchView();
            enable.setSummary(getString(R.string.cpu_boost));
            enable.setChecked(mCPUBoost.isEnabled());
            enable.addOnSwitchListener((switchView, isChecked)
                    -> mCPUBoost.enableCpuBoost(isChecked, getActivity()));

            cpuBoost.add(enable);
        }

        if (mCPUBoost.hasCpuBoostDebugMask()) {
            SwitchView debugMask = new SwitchView();
            debugMask.setTitle(getString(R.string.debug_mask));
            debugMask.setSummary(getString(R.string.debug_mask_summary));
            debugMask.setChecked(mCPUBoost.isCpuBoostDebugMaskEnabled());
            debugMask.addOnSwitchListener((switchView, isChecked)
                    -> mCPUBoost.enableCpuBoostDebugMask(isChecked, getActivity()));

            cpuBoost.add(debugMask);
        }

        if (mCPUBoost.hasCpuBoostMs()) {
            SeekBarView ms = new SeekBarView();
            ms.setTitle(getString(R.string.interval));
            ms.setSummary(getString(R.string.interval_summary));
            ms.setUnit(getString(R.string.ms));
            ms.setMax(5000);
            ms.setOffset(10);
            ms.setProgress(mCPUBoost.getCpuBootMs() / 10);
            ms.setOnSeekBarListener(new SeekBarView.OnSeekBarListener() {
                @Override
                public void onMove(SeekBarView seekBarView, int position, String value) {
                }

                @Override
                public void onStop(SeekBarView seekBarView, int position, String value) {
                    mCPUBoost.setCpuBoostMs(position * 10, getActivity());
                }
            });

            cpuBoost.add(ms);
        }

        if (mCPUBoost.hasCpuBoostSyncThreshold() && mCPUFreq.getFreqs() != null) {
            List<String> list = new ArrayList<>();
            list.add(getString(R.string.disabled));
            list.addAll(mCPUFreq.getAdjustedFreq(getActivity()));

            SelectView syncThreshold = new SelectView();
            syncThreshold.setTitle(getString(R.string.sync_threshold));
            syncThreshold.setSummary(getString(R.string.sync_threshold_summary));
            syncThreshold.setItems(list);
            syncThreshold.setItem(mCPUBoost.getCpuBootSyncThreshold());
            syncThreshold.setOnItemSelected((selectView, position, item)
                    -> mCPUBoost.setCpuBoostSyncThreshold(position == 0 ?
                            0 : mCPUFreq.getFreqs().get(position - 1),
                    getActivity()));

            cpuBoost.add(syncThreshold);
        }

        if (mCPUBoost.hasCpuBoostInputMs()) {
            SeekBarView inputMs = new SeekBarView();
            inputMs.setTitle(getString(R.string.input_interval));
            inputMs.setSummary(getString(R.string.input_interval_summary));
            inputMs.setUnit(getString(R.string.ms));
            inputMs.setMax(5000);
            inputMs.setOffset(10);
            inputMs.setProgress(mCPUBoost.getCpuBootInputMs() / 10);
            inputMs.setOnSeekBarListener(new SeekBarView.OnSeekBarListener() {
                @Override
                public void onMove(SeekBarView seekBarView, int position, String value) {
                }

                @Override
                public void onStop(SeekBarView seekBarView, int position, String value) {
                    mCPUBoost.setCpuBoostInputMs(position * 10, getActivity());
                }
            });

            cpuBoost.add(inputMs);
        }

        if (mCPUBoost.hasCpuBoostInputFreq()) {
            List<Integer> freqs = mCPUBoost.getCpuBootInputFreq();
            for (int i = 0; i < freqs.size(); i++) {
                List<String> list = new ArrayList<>();
                list.add(getString(R.string.disabled));
                list.addAll(mCPUFreq.getAdjustedFreq(i, getActivity()));

                SelectView inputCard = new SelectView();
                if (freqs.size() > 1) {
                    inputCard.setTitle(getString(R.string.input_boost_freq_core, i + 1));
                } else {
                    inputCard.setTitle(getString(R.string.input_boost_freq));
                }
                inputCard.setSummary(getString(R.string.input_boost_freq_summary));
                inputCard.setItems(list);
                inputCard.setItem(freqs.get(i));

                final int core = i;
                inputCard.setOnItemSelected((selectView, position, item)
                        -> mCPUBoost.setCpuBoostInputFreq(position == 0 ?
                                0 : mCPUFreq.getFreqs(core).get(position - 1),
                        core, getActivity()));

                cpuBoost.add(inputCard);
            }
        }

        if (mCPUBoost.hasCpuBoostWakeup()) {
            SwitchView wakeup = new SwitchView();
            wakeup.setTitle(getString(R.string.wakeup_boost));
            wakeup.setSummary(getString(R.string.wakeup_boost_summary));
            wakeup.setChecked(mCPUBoost.isCpuBoostWakeupEnabled());
            wakeup.addOnSwitchListener((switchView, isChecked)
                    -> mCPUBoost.enableCpuBoostWakeup(isChecked, getActivity()));

            cpuBoost.add(wakeup);
        }

        if (mCPUBoost.hasCpuBoostHotplug()) {
            SwitchView hotplug = new SwitchView();
            hotplug.setTitle(getString(R.string.hotplug_boost));
            hotplug.setSummary(getString(R.string.hotplug_boost_summary));
            hotplug.setChecked(mCPUBoost.isCpuBoostHotplugEnabled());
            hotplug.addOnSwitchListener((switchView, isChecked)
                    -> mCPUBoost.enableCpuBoostHotplug(isChecked, getActivity()));

            cpuBoost.add(hotplug);
        }

        if (!cpuBoost.isEmpty()) {
            CardView boostCard = new CardView();
            boostCard.setTitle(getString(R.string.cpu_boost));
            for (RecyclerViewItem item : cpuBoost) {
                boostCard.addItem(item);
            }
            items.add(boostCard);
        }
    }

    private void cpuTouchBoostInit(List<RecyclerViewItem> items) {
        SwitchView touchBoost = new SwitchView();
        touchBoost.setTitle(getString(R.string.touch_boost));
        touchBoost.setSummary(getString(R.string.touch_boost_summary));
        touchBoost.setChecked(Misc.isCpuTouchBoostEnabled());
        touchBoost.addOnSwitchListener((switchView, isChecked)
                -> Misc.enableCpuTouchBoost(isChecked, getActivity()));

        items.add(touchBoost);
    }

    @Override
    protected void refreshThread() {
        super.refreshThread();
        if (mClusterHolders.isEmpty()) return;

        try {
            mCPUUsages = mCPUFreq.getCpuUsage();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (mCPUUsages == null) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }
        }

        for (ClusterHolder holder : mClusterHolders) {
            int cpu = holder.cluster.firstCpu;
            holder.curFreq = readClusterCurFreq(holder.cluster);
            holder.minFreq = mCPUFreq.getMinFreq(cpu, holder.minFreq == 0);
            holder.maxFreq = mCPUFreq.getMaxFreq(cpu, holder.maxFreq == 0);
            String gov = mCPUFreq.getGovernor(cpu, holder.governor == null || holder.governor.isEmpty());
            if (gov != null && !gov.isEmpty()) {
                holder.governor = gov;
            }
            if (mCPUFreq.hasMaxScreenOffFreq(cpu)) {
                holder.screenOffFreq = mCPUFreq.getMaxScreenOffFreq(cpu, holder.screenOffFreq == 0);
            }
        }
    }

    @Override
    protected void refresh() {
        super.refresh();

        if (mDashboard != null) {
            if (mCPUUsages != null && mCPUUsages.length > 0) {
                mDashboard.setOverallUsage(Math.round(mCPUUsages[0]));
            }
            for (int i = 0; i < mClusterHolders.size(); i++) {
                ClusterHolder holder = mClusterHolders.get(i);
                int maxFreq = (holder.freqs != null && !holder.freqs.isEmpty())
                        ? holder.freqs.get(holder.freqs.size() - 1) : 0;
                int pct = (holder.curFreq > 0 && maxFreq > 0)
                        ? Math.round((float) holder.curFreq / maxFreq * 100f) : 0;
                String freqText = holder.curFreq > 0
                        ? ClusterControlView.formatFreqKhz(holder.curFreq)
                        : getString(R.string.offline);
                mDashboard.updateRow(i, pct, freqText);
            }
        }

        for (ClusterHolder holder : mClusterHolders) {
            if (holder.governor != null && !holder.governor.isEmpty()) {
                holder.control.setCurrentGovernor(holder.governor);
            }
            if (holder.freqs != null && !holder.freqs.isEmpty()) {
                if (holder.maxFreq > 0) {
                    holder.control.setFreqIndex(findClosestFreqIndex(holder.freqs, holder.maxFreq));
                }
            }
        }

        for (int i = 0; i < mScreenOffSelects.size() && i < mScreenOffClusters.size(); i++) {
            SelectView selectView = mScreenOffSelects.get(i);
            CPUFreq.Cluster cluster = mScreenOffClusters.get(i);
            for (ClusterHolder holder : mClusterHolders) {
                if (holder.cluster == cluster && holder.screenOffFreq > 0) {
                    selectView.setItem(ClusterControlView.formatFreqKhz(holder.screenOffFreq));
                    break;
                }
            }
        }
    }
}
