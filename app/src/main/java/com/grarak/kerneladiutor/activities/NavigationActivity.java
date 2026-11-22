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
package com.grarak.kerneladiutor.activities;

import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;

import com.grarak.kerneladiutor.R;
import com.grarak.kerneladiutor.fragments.ApplyOnBootFragment;
import com.grarak.kerneladiutor.fragments.BaseFragment;
import com.grarak.kerneladiutor.fragments.kernel.CPUFragment;
import com.grarak.kerneladiutor.fragments.kernel.GPUFragment;
import com.grarak.kerneladiutor.fragments.kernel.IOFragment;
import com.grarak.kerneladiutor.utils.AppSettings;
import com.grarak.kerneladiutor.utils.Device;
import com.grarak.kerneladiutor.utils.Log;
import com.grarak.kerneladiutor.utils.Utils;
import com.grarak.kerneladiutor.utils.ViewUtils;
import com.grarak.kerneladiutor.utils.kernel.cpu.CPUBoost;
import com.grarak.kerneladiutor.utils.kernel.cpu.CPUFreq;
import com.grarak.kerneladiutor.utils.kernel.cpu.MSMPerformance;
import com.grarak.kerneladiutor.utils.kernel.cpu.Temperature;
import com.grarak.kerneladiutor.utils.kernel.gpu.GPU;
import com.grarak.kerneladiutor.utils.kernel.io.IO;
import com.grarak.kerneladiutor.utils.root.RootUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class NavigationActivity extends BaseActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String PACKAGE = NavigationActivity.class.getCanonicalName();
    public static final String INTENT_SECTION = PACKAGE + ".INTENT.SECTION";

    private ArrayList<NavigationFragment> mFragments = new ArrayList<>();
    private final List<NavigationFragment> mTabs = new ArrayList<>();
    private Map<Integer, Class<? extends Fragment>> mActualFragments = new LinkedHashMap<>();

    private DrawerLayout mDrawer;
    private NavigationView mNavigationView;
    private ViewPager mViewPager;
    private TabLayout mTabLayout;
    private MainPagerAdapter mPagerAdapter;
    private View mApplyOnBootContainer;
    private SwitchCompat mApplyOnBootSwitch;
    private long mLastTimeBackbuttonPressed;

    private int mSelection;
    private boolean mLicenseDialog = true;

    @Override
    protected boolean setStatusBarColor() {
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            new FragmentLoader(this).execute();
        } else {
            mFragments = savedInstanceState.getParcelableArrayList("fragments");
            init(savedInstanceState);
        }
    }

    private static class FragmentLoader extends AsyncTask<Void, Void, Void> {

        private WeakReference<NavigationActivity> mRefActivity;
        private boolean mHasRoot;
        private boolean mHasBusybox;

        private FragmentLoader(NavigationActivity activity) {
            mRefActivity = new WeakReference<>(activity);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            NavigationActivity activity = mRefActivity.get();
            if (activity == null) return null;

            mHasRoot = RootUtils.rootAccess();
            if (mHasRoot) {
                mHasBusybox = RootUtils.busyboxInstalled();
                if (mHasBusybox) {
                    CPUBoost.getInstance();
                    CPUFreq.getInstance(activity);
                    Device.CPUInfo.getInstance();
                    GPU.supported();
                    IO.getInstance();
                    MSMPerformance.getInstance();
                    Temperature.getInstance(activity);

                    Log.crashlyticsI(NavigationActivity.class.getSimpleName(), "Kernel version: "
                            + Device.getKernelVersion(true));
                    Log.crashlyticsI(NavigationActivity.class.getSimpleName(), "Board: "
                            + Device.getBoard());

                    activity.initFragments();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            NavigationActivity activity = mRefActivity.get();
            if (activity == null || activity.isFinishing()) return;

            if (!mHasRoot || !mHasBusybox) {
                Intent intent = new Intent(activity, TextActivity.class);
                intent.putExtra(TextActivity.MESSAGE_INTENT, activity.getString(mHasRoot ?
                        R.string.no_busybox : R.string.no_root));
                intent.putExtra(TextActivity.SUMMARY_INTENT,
                        mHasRoot ? "https://play.google.com/store/apps/details?id=stericson.busybox" :
                                "https://www.google.com/search?site=&source=hp&q=root+"
                                        + Device.getVendor() + "+" + Device.getModel());
                activity.startActivity(intent);
                activity.finish();
                return;
            }
            activity.init(null);
        }
    }

    private void initFragments() {
        mFragments.clear();
        mFragments.add(new NavigationFragment(R.string.kernel));
        mFragments.add(new NavigationFragment(R.string.cpu, CPUFragment.class, R.drawable.ic_cpu));
        if (GPU.supported()) {
            mFragments.add(new NavigationFragment(R.string.gpu, GPUFragment.class, R.drawable.ic_gpu));
        }
        if (IO.getInstance().supported()) {
            mFragments.add(new NavigationFragment(R.string.io_scheduler, IOFragment.class, R.drawable.ic_sdcard));
        }
    }

    private void init(Bundle savedInstanceState) {
        int result = getIntent().getIntExtra("result", -1);

        if ((result == 1 || result == 2) && mLicenseDialog) {
            ViewUtils.dialogBuilder(getString(R.string.license_invalid),
                    null,
                    (dialog, which) -> {
                    },
                    dialog -> mLicenseDialog = false, this)
                    .show();
        } else if (result == 3 && mLicenseDialog) {
            ViewUtils.dialogBuilder(getString(R.string.pirated),
                    null,
                    (dialog, which) -> {
                    },
                    dialog -> mLicenseDialog = false, this)
                    .show();
        } else {
            mLicenseDialog = false;
            if (result == 0) {
                Utils.DONATED = true;
            }
        }

        setContentView(R.layout.activity_navigation);
        Toolbar toolbar = getToolBar();
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
        }

        mDrawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, mDrawer, toolbar, 0, 0);
        mDrawer.addDrawerListener(toggle);
        toggle.syncState();

        mNavigationView = findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(this);
        mNavigationView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                v.clearFocus();
            }
        });

        mTabLayout = findViewById(R.id.tab_layout);
        mViewPager = findViewById(R.id.main_viewpager);
        mApplyOnBootContainer = findViewById(R.id.apply_on_boot_container);
        mApplyOnBootSwitch = findViewById(R.id.apply_on_boot_switch);

        if (savedInstanceState != null) {
            mSelection = savedInstanceState.getInt(INTENT_SECTION);
            mLicenseDialog = savedInstanceState.getBoolean("license");
        }

        appendFragments(false);

        mPagerAdapter = new MainPagerAdapter(getSupportFragmentManager(), mTabs);
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.setOffscreenPageLimit(Math.max(1, mTabs.size()));
        mTabLayout.setupWithViewPager(mViewPager);
        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                syncTabSelection(position, true);
            }
        });

        mApplyOnBootContainer.setOnClickListener(v -> {
            int pos = mViewPager.getCurrentItem();
            if (pos >= 0 && pos < mTabs.size()) {
                String category = ApplyOnBootFragment.getAssignment(mTabs.get(pos).mFragmentClass);
                boolean next = !mApplyOnBootSwitch.isChecked();
                mApplyOnBootSwitch.setChecked(next);
                AppSettings.saveBoolean(category, next, NavigationActivity.this);
                Utils.toast(getString(R.string.apply_on_boot) + ": "
                        + getString(next ? R.string.enabled : R.string.disabled), NavigationActivity.this);
            }
        });

        String section = getIntent().getStringExtra(INTENT_SECTION);
        if (section != null) {
            for (Map.Entry<Integer, Class<? extends Fragment>> entry : mActualFragments.entrySet()) {
                Class<? extends Fragment> fragmentClass = entry.getValue();
                if (fragmentClass != null && fragmentClass.getCanonicalName().equals(section)) {
                    mSelection = entry.getKey();
                    break;
                }
            }
            getIntent().removeExtra(INTENT_SECTION);
        }

        if (mSelection == 0 || mActualFragments.get(mSelection) == null) {
            mSelection = firstTab();
        }
        onItemSelected(mSelection, false);
    }

    private void syncTabSelection(int position, boolean saveOpened) {
        if (position < 0 || position >= mTabs.size()) return;
        NavigationFragment tab = mTabs.get(position);
        mSelection = tab.mId;
        mNavigationView.setCheckedItem(mSelection);

        if (mApplyOnBootSwitch != null && tab.mFragmentClass != null) {
            String category = ApplyOnBootFragment.getAssignment(tab.mFragmentClass);
            mApplyOnBootSwitch.setChecked(AppSettings.getBoolean(category, false, this));
        }

        if (saveOpened && tab.mFragmentClass != null) {
            AppSettings.saveFragmentOpened(tab.mFragmentClass,
                    AppSettings.getFragmentOpened(tab.mFragmentClass, this) + 1, this);
            setShortcuts();
        }
    }

    private int firstTab() {
        for (Map.Entry<Integer, Class<? extends Fragment>> entry : mActualFragments.entrySet()) {
            if (entry.getValue() != null) {
                return entry.getKey();
            }
        }
        return 0;
    }

    public void appendFragments() {
        appendFragments(true);
    }

    private void appendFragments(boolean setShortcuts) {
        mActualFragments.clear();
        mTabs.clear();
        Menu menu = mNavigationView.getMenu();
        menu.clear();

        SubMenu lastSubMenu = null;
        for (NavigationFragment navigationFragment : mFragments) {
            Class<? extends Fragment> fragmentClass = navigationFragment.mFragmentClass;
            int id = navigationFragment.mId;

            Drawable drawable = ContextCompat.getDrawable(this,
                    navigationFragment.mDrawable != 0 ? navigationFragment.mDrawable : R.drawable.ic_blank);

            if (fragmentClass == null) {
                lastSubMenu = menu.addSubMenu(id);
                mActualFragments.put(id, null);
            } else if (AppSettings.isFragmentEnabled(fragmentClass, this)) {
                MenuItem menuItem = lastSubMenu == null ? menu.add(0, id, 0, id) :
                        lastSubMenu.add(0, id, 0, id);
                menuItem.setIcon(drawable);
                menuItem.setCheckable(true);
                if (mSelection != 0) {
                    mNavigationView.setCheckedItem(mSelection);
                }
                mActualFragments.put(id, fragmentClass);
                mTabs.add(navigationFragment);
            }
        }
        if (mPagerAdapter != null) {
            mPagerAdapter.notifyDataSetChanged();
        }
        if (setShortcuts) {
            setShortcuts();
        }
    }

    private NavigationFragment findNavigationFragmentByClass(Class<? extends Fragment> fragmentClass) {
        if (fragmentClass == null) return null;
        for (NavigationFragment navigationFragment : mFragments) {
            if (fragmentClass == navigationFragment.mFragmentClass) {
                return navigationFragment;
            }
        }
        return null;
    }

    private void setShortcuts() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return;

        PriorityQueue<Class<? extends Fragment>> queue = new PriorityQueue<>(
                (o1, o2) -> {
                    int opened1 = AppSettings.getFragmentOpened(o1, this);
                    int opened2 = AppSettings.getFragmentOpened(o2, this);
                    return opened2 - opened1;
                });

        for (Map.Entry<Integer, Class<? extends Fragment>> entry : mActualFragments.entrySet()) {
            Class<? extends Fragment> fragmentClass = entry.getValue();
            if (fragmentClass == null) continue;

            queue.offer(fragmentClass);
        }

        List<ShortcutInfo> shortcutInfos = new ArrayList<>();
        ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
        if (shortcutManager == null) return;
        shortcutManager.removeAllDynamicShortcuts();
        for (int i = 0; i < 4; i++) {
            NavigationFragment fragment = findNavigationFragmentByClass(queue.poll());
            if (fragment == null || fragment.mFragmentClass == null) continue;
            Intent intent = new Intent(this, NavigationActivity.class);
            intent.setAction(Intent.ACTION_VIEW);
            intent.putExtra(INTENT_SECTION, fragment.mFragmentClass.getCanonicalName());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            ShortcutInfo shortcut = new ShortcutInfo.Builder(this,
                    fragment.mFragmentClass.getSimpleName())
                    .setShortLabel(getString(fragment.mId))
                    .setLongLabel(Utils.strFormat(getString(R.string.open), getString(fragment.mId)))
                    .setIcon(Icon.createWithResource(this, fragment.mDrawable == 0 ?
                            R.drawable.ic_blank : fragment.mDrawable))
                    .setIntent(intent)
                    .build();
            shortcutInfos.add(shortcut);
        }
        shortcutManager.setDynamicShortcuts(shortcutInfos);
    }

    public ArrayList<NavigationFragment> getFragments() {
        return mFragments;
    }

    public Map<Integer, Class<? extends Fragment>> getActualFragments() {
        return mActualFragments;
    }

    @Override
    public void onBackPressed() {
        if (mDrawer != null && mDrawer.isDrawerOpen(GravityCompat.START)) {
            mDrawer.closeDrawer(GravityCompat.START);
        } else {
            Fragment currentFragment = (mPagerAdapter != null && mViewPager != null)
                    ? mPagerAdapter.getRegisteredFragment(mViewPager.getCurrentItem()) : null;
            if (!(currentFragment instanceof BaseFragment)
                    || !((BaseFragment) currentFragment).onBackPressed()) {
                long currentTime = SystemClock.elapsedRealtime();
                if (currentTime - mLastTimeBackbuttonPressed > 2000) {
                    mLastTimeBackbuttonPressed = currentTime;
                    Utils.toast(R.string.press_back_again_exit, this);
                } else {
                    super.onBackPressed();
                }
            }
        }
    }

    @Override
    public void finish() {
        super.finish();
        RootUtils.closeSU();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelableArrayList("fragments", mFragments);
        outState.putInt(INTENT_SECTION, mSelection);
        outState.putBoolean("license", mLicenseDialog);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        onItemSelected(item.getItemId(), true);
        return true;
    }

    private void onItemSelected(final int res, boolean saveOpened) {
        if (mDrawer != null && mDrawer.isDrawerOpen(GravityCompat.START)) {
            mDrawer.closeDrawer(GravityCompat.START);
        }
        for (int i = 0; i < mTabs.size(); i++) {
            if (mTabs.get(i).mId == res) {
                if (mViewPager != null) {
                    mViewPager.setCurrentItem(i, saveOpened);
                }
                syncTabSelection(i, saveOpened);
                break;
            }
        }
    }

    private class MainPagerAdapter extends FragmentPagerAdapter {

        private final List<NavigationFragment> mItems;
        private final SparseArray<Fragment> mRegistered = new SparseArray<>();

        MainPagerAdapter(FragmentManager fm, List<NavigationFragment> items) {
            super(fm);
            mItems = items;
        }

        @Override
        public Fragment getItem(int position) {
            return Fragment.instantiate(NavigationActivity.this,
                    mItems.get(position).mFragmentClass.getCanonicalName());
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return getString(mItems.get(position).mId);
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            Fragment fragment = (Fragment) super.instantiateItem(container, position);
            mRegistered.put(position, fragment);
            return fragment;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            mRegistered.remove(position);
            super.destroyItem(container, position, object);
        }

        Fragment getRegisteredFragment(int position) {
            return mRegistered.get(position);
        }
    }

    public static class NavigationFragment implements Parcelable {

        public int mId;
        public Class<? extends Fragment> mFragmentClass;
        private int mDrawable;

        NavigationFragment(int id) {
            this(id, null, 0);
        }

        NavigationFragment(int id, Class<? extends Fragment> fragment, int drawable) {
            mId = id;
            mFragmentClass = fragment;
            mDrawable = drawable;
        }

        NavigationFragment(Parcel parcel) {
            mId = parcel.readInt();
            mFragmentClass = (Class<? extends Fragment>) parcel.readSerializable();
            mDrawable = parcel.readInt();
        }

        @Override
        public String toString() {
            return String.valueOf(mId);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(mId);
            dest.writeSerializable(mFragmentClass);
            dest.writeInt(mDrawable);
        }

        public static final Creator CREATOR = new Creator<NavigationFragment>() {
            @Override
            public NavigationFragment createFromParcel(Parcel source) {
                return new NavigationFragment(source);
            }

            @Override
            public NavigationFragment[] newArray(int size) {
                return new NavigationFragment[0];
            }
        };
    }

}
