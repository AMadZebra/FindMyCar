package luckong.findmycar;

import android.graphics.Color;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

/**
 * Created by Luc on 5/19/2017.
 */
public class IntroAdapter extends FragmentPagerAdapter {

    public IntroAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return IntroFragment.newInstance(Color.parseColor("#03A9F4"), position); // blue
            case 1:
                return IntroFragment.newInstance(Color.parseColor("#FFFF00"), position); // yellow
            case 2:
                return IntroFragment.newInstance(Color.parseColor("#4CAF50"), position); // green
            default:
                return IntroFragment.newInstance(Color.parseColor("#847264"), position); // brown
        }
    }

    @Override
    public int getCount() {
        return 4;
    }

}