package com.xtremelabs.robolectric.shadows;

import android.view.View;
import android.widget.TabHost;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.internal.Implementation;
import com.xtremelabs.robolectric.internal.Implements;
import com.xtremelabs.robolectric.internal.RealObject;

import java.util.ArrayList;
import java.util.List;

import static com.xtremelabs.robolectric.Robolectric.shadowOf;

@SuppressWarnings({"UnusedDeclaration"})
@Implements(TabHost.class)
public class ShadowTabHost extends ShadowFrameLayout {
    private List<TabHost.TabSpec> tabSpecs = new ArrayList<TabHost.TabSpec>();
    private TabHost.OnTabChangeListener listener;
    private TabHost.TabSpec currentTab;

    @RealObject
    TabHost realObject;

    @Implementation
    public android.widget.TabHost.TabSpec newTabSpec(java.lang.String tag) {
        TabHost.TabSpec realTabSpec = Robolectric.newInstanceOf(TabHost.TabSpec.class);
        shadowOf(realTabSpec).setTag(tag);
        return realTabSpec;
    }

    @Implementation
    public void addTab(android.widget.TabHost.TabSpec tabSpec) {
        tabSpecs.add(tabSpec);
        View indicatorAsView = shadowOf(tabSpec).getIndicatorAsView();
        if (indicatorAsView != null) {
            realObject.addView(indicatorAsView);
        }
    }

    @Implementation
    public void setCurrentTab(int index) {
        currentTab = tabSpecs.get(index);
        if (listener != null) {
            listener.onTabChanged(currentTab.getTag());
        }
    }

    @Implementation
    public void setCurrentTabByTag(String tag) {
        for (TabHost.TabSpec tabSpec : tabSpecs) {
            if (tag.equals(tabSpec.getTag())) {
                currentTab = tabSpec;
            }
        }
        if (listener != null) {
            listener.onTabChanged(currentTab.getTag());
        }
    }

    @Implementation
    public void setOnTabChangedListener(android.widget.TabHost.OnTabChangeListener listener) {
        this.listener = listener;
    }

    public TabHost.TabSpec getSpecByTag(String tag) {
        for (TabHost.TabSpec tabSpec : tabSpecs) {
            if (tag.equals(tabSpec.getTag())) {
                return tabSpec;
            }
        }
        return null;
    }
}
