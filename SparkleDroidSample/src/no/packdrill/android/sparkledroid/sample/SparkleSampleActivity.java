/*
   Copyright [2013] [Donald Munro]

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package no.packdrill.android.sparkledroid.sample;

import android.annotation.*;
import android.app.*;
import android.os.*;
import android.util.*;

import java.net.*;
import java.util.*;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class SparkleSampleActivity extends Activity implements ActionBar.TabListener
//============================================================================
{
   static final String LOGTAG = SparkleSampleActivity.class.getSimpleName();

   QueryFragment queryFragment = null;
   ResultFragment resultFragment = null;

   private int activeTab = -1;
   private ArrayList<String> saveColumns;

   URI endPoint = null;
   public URI getEndPoint() { return endPoint;  }

   String tableName = null;
   public String getTableName() { return tableName; }

   @Override
   public void onCreate(Bundle b)
   //---------------------------------------------
   {
      super.onCreate(b);
//      getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
      setContentView(R.layout.main);
      final ActionBar actionBar = getActionBar();
      actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
      actionBar.setDisplayShowTitleEnabled(false);
      actionBar.addTab(actionBar.newTab().setText(R.string.query_tab).setTabListener(this));
      actionBar.addTab(actionBar.newTab().setText(R.string.results_tab).setTabListener(this));
      //resultFragment = (ResultFragment) Fragment.instantiate(this, ResultFragment.class.getName());
      if (b != null)
      {
         tableName = b.getString("tableName");
         activeTab = b.getInt("activeTab");
         saveColumns = b.getStringArrayList("columns");
         String suri = b.getString("endPoint");
         if ( (suri != null) && (! suri.trim().isEmpty()) )
            try { endPoint = new URI(suri); } catch (Exception _e) { endPoint = null; }
         if (activeTab >= 0)
            getActionBar().setSelectedNavigationItem(activeTab);
//          actionBar.selectTab(actionBar.getTabAt(activeTab));
      }
   }

   @Override
   protected void onSaveInstanceState(Bundle b)
   //------------------------------------------
   {
      super.onSaveInstanceState(b);
      b.putString("tableName", tableName);
      final ActionBar actionBar = getActionBar();
      ActionBar.Tab tab = actionBar.getSelectedTab();
      if (tab != null)
         b.putInt("activeTab", tab.getPosition());
      else
         b.putInt("activeTab", -1);
      if (endPoint != null)
         b.putString("endPoint", endPoint.toString());
      //ResultFragment resultFragment = (ResultFragment) getFragmentManager().findFragmentByTag("results");
      if (queryFragment != null)
         queryFragment.onSaveInstanceState(b);
      if (resultFragment != null)
         resultFragment.onSaveInstanceState(b);
   }

   @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
   @Override
   protected void onPause()
   //----------------------
   {
      super.onPause();
      final FragmentTransaction ft = getFragmentManager().beginTransaction().disallowAddToBackStack();
      if (queryFragment != null)
         try { ft.detach(queryFragment); } catch (final Throwable e) { Log.e(LOGTAG, "", e);  }
      if (resultFragment != null)
         try { ft.detach(resultFragment); } catch (final Throwable e) { Log.e(LOGTAG, "", e);  }
      ft.commit();
   }

   @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
   @Override
   public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft)
   //------------------------------------------------------------------
   {
      activeTab = tab.getPosition();
      switch (activeTab)
      {
         case 0:
            if (queryFragment == null)
            {
               queryFragment = (QueryFragment) Fragment.instantiate(this, QueryFragment.class.getName());
               queryFragment.setRetainInstance(true);
               ft.add(R.id.tabContents, queryFragment, "query");
            }
            else
               ft.attach(queryFragment);
            ft.show(queryFragment);
            break;
         case 1:
            if (resultFragment == null)
            {
               Bundle b = new Bundle();
               if (saveColumns != null)
                  b.putStringArrayList("columns", saveColumns);
               resultFragment = (ResultFragment) Fragment.instantiate(this, ResultFragment.class.getName(), b);
               resultFragment.setRetainInstance(true);
               ft.add(R.id.tabContents, resultFragment, "results");
               ft.show(resultFragment);
            }
            else
               ft.attach(resultFragment);
            break;
      }
   }

   @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
   @Override
   public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft)
   //--------------------------------------------------------------------
   {
      switch (tab.getPosition())
      {
         case 0:
            if (queryFragment != null)
               try { ft.detach(queryFragment); } catch (final Throwable e) { Log.e(LOGTAG, "", e);  }
            break;
         case 1:
            if (resultFragment != null)
               try { ft.detach(resultFragment); } catch (final Throwable e) { Log.e(LOGTAG, "", e);  }
            break;
      }
   }

   @Override public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) { }

   protected void resultsTab() {  getActionBar().setSelectedNavigationItem(1); }

   protected void queryTab() {  getActionBar().setSelectedNavigationItem(0); }
}
