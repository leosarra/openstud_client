package com.lithium.leona.openstud.fragments;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.lithium.leona.openstud.R;
import com.lithium.leona.openstud.adapters.TaxAdapter;

public class TabFragment extends Fragment {

    public static TabLayout tabLayout;
    public static ViewPager viewPager;
    public static int int_items = 2;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        //this inflates out tab layout file.
        View x =  inflater.inflate(R.layout.tab_fragment_payment_layout,null);
        // set up stuff.
        tabLayout = (TabLayout) x.findViewById(R.id.tabs);
        viewPager = (ViewPager) x.findViewById(R.id.viewpager);

        // create a new adapter for our pageViewer. This adapters returns child com.lithium.leona.openstud.fragments as per the positon of the page Viewer.
        viewPager.setAdapter(new MyAdapter(getChildFragmentManager()));

        // this is a workaround
        tabLayout.post(new Runnable() {
            @Override
            public void run() {
                //provide the viewPager to TabLayout.
                tabLayout.setupWithViewPager(viewPager);
            }
        });
        //to preload the adjacent tabs. This makes transition smooth.
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        tabLayout.setTabMode(TabLayout.MODE_FIXED);

        return x;
    }

    class MyAdapter extends FragmentPagerAdapter{

        public MyAdapter(FragmentManager fm) {
            super(fm);
        }

        //return the fragment with respect to page position.
        @Override
        public Fragment getItem(int position)
        {
            switch (position){
                case 0 : {
                    Bundle bdl = new Bundle();
                    bdl.putInt("mode", TaxAdapter.Mode.UNPAID.getValue());
                    PaymentsFragment frag = new PaymentsFragment();
                    frag.setArguments(bdl);
                    return frag;
                }
                case 1 : {
                    Bundle bdl = new Bundle();
                    bdl.putInt("mode", TaxAdapter.Mode.PAID.getValue());
                    PaymentsFragment frag = new PaymentsFragment();
                    frag.setArguments(bdl);
                    return frag;
                }
            }
            return null;
        }

        @Override
        public int getCount() {

            return int_items;

        }

        //This method returns the title of the tab according to the position.
        @Override
        public CharSequence getPageTitle(int position) {

            switch (position){
                case 0 :
                    return getResources().getString(R.string.unpaid);
                case 1 :
                    return getResources().getString(R.string.paid);
            }
            return null;
        }
    }
}