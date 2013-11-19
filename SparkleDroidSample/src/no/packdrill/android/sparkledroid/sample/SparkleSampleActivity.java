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
import android.content.*;
import android.os.*;
import android.text.*;
import android.util.*;
import android.view.*;
import android.widget.*;

import java.io.*;
import java.net.*;
import java.util.*;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class SparkleSampleActivity extends Activity implements ActionBar.TabListener, OpenDialog.DialogCloseable
//==============================================================================================================
{
   static final String LOGTAG = SparkleSampleActivity.class.getSimpleName();
   private static final String SAVE_DELIM = "||";

   QueryFragment queryFragment = null;
   ResultFragment resultFragment = null;

   private int activeTab = -1;
   private ArrayList<String> lastColumns;

   URI endPoint = null;
   public URI getEndPoint() { return endPoint;  }

   String endPointString;
   public String getEndPointString() { return endPointString; }

   String tableName = null;
   public String getTableName() { return tableName; }

   String lastQuery = null, lastDefaultGraph = null, lastNamedGraph = null;

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
         lastColumns = b.getStringArrayList("columns");
         String suri = b.getString("endPoint");
         if ( (suri != null) && (! suri.trim().isEmpty()) )
            try { endPoint = new URI(suri); } catch (Exception _e) { endPoint = null; }
         endPointString = b.getString("endPointString");
         lastQuery = b.getString("query");
         lastDefaultGraph = b.getString("defaultGraph");
         lastNamedGraph = b.getString("namedGraph");
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
      if (endPointString != null)
         b.putString("endPointString", endPointString);
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
   protected void onResume()
   //----------------------
   {
      super.onResume();
      final FragmentTransaction ft = getFragmentManager().beginTransaction().disallowAddToBackStack();
      if (queryFragment != null)
         try { ft.attach(queryFragment); } catch (final Throwable e) { Log.e(LOGTAG, "", e);  }
      if (resultFragment != null)
         try { ft.attach(resultFragment); } catch (final Throwable e) { Log.e(LOGTAG, "", e);  }
      ft.commit();
      getActionBar().setSelectedNavigationItem(0);
      if (activeTab >= 0)
         getActionBar().setSelectedNavigationItem(activeTab);
//          actionBar.selectTab(actionBar.getTabAt(activeTab));
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu)
   //-------------------------------------------
   {
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.menu, menu);
      return super.onCreateOptionsMenu(menu);
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item)
   //-------------------------------------------------
   {
      switch (item.getItemId())
      {
         case R.id.action_save:
            actionSave();
            break;

         case R.id.action_open:
            actionOpen();
            break;

         default:
            return super.onOptionsItemSelected(item);
      }
      return true;
   }

   private void actionOpen()
   //-----------------------
   {
      FragmentTransaction ft = getFragmentManager().beginTransaction();
      final SparkleSampleApp app = (SparkleSampleApp) getApplication();
      OpenDialog dialog = OpenDialog.instance(app.getStorageDirectory());
      dialog.show(ft, "OpenDialog");
   }

   private void actionSave()
   //----------------------
   {
      final EditText et = new EditText(this);
      InputFilter filter = new RestrictedCharFilter();
      et.setFilters(new InputFilter[]{filter});
      new AlertDialog.Builder(this).setTitle("Save Query").setMessage("Filename:").setView(et).
      setPositiveButton("Ok", new DialogInterface.OnClickListener()
      {
         @Override
         public void onClick(DialogInterface dialog, int which)
         //----------------------------------------------------
         {
            String filename = (et.getText() == null) ? null : et.getText().toString();
            if ((filename == null) || (filename.trim().isEmpty()))
            {
               Toast.makeText(SparkleSampleActivity.this, "Invalid or no filename specified", Toast.LENGTH_LONG).show();
               return;
            }
            filename = filename.replace('\\', '/');
            int p = filename.lastIndexOf('/');
            if (p >= 0)
               try { filename = filename.substring(p+1); } catch (Exception _e) { Toast.makeText(SparkleSampleActivity.this, "Invalid filename", Toast.LENGTH_LONG).show(); return; }
            p = filename.lastIndexOf('.');
            if (p >= 0)
               filename = filename.substring(0, p);
            filename = filename + ".sparql";
            final SparkleSampleApp app = (SparkleSampleApp) getApplication();
            final File path = new File(app.getStorageDirectory(), filename);
            if (path.exists())
            {
               new AlertDialog.Builder(SparkleSampleActivity.this).setTitle("Confirm Overwrite").
                     setMessage("Overwrite " + filename + " ?").
                     setPositiveButton("Ok", new DialogInterface.OnClickListener()
                     {
                        @Override public void onClick(DialogInterface dialog, int which) { saveQuery(path);  }
                     }).
                     setNegativeButton("Cancel", new DialogInterface.OnClickListener()
                     {
                        @Override public void onClick(DialogInterface dialog, int which) { }
                     }).show();
            } else
            {
               if (! app.getStorageDirectory().exists())
                  app.getStorageDirectory().mkdirs();
               saveQuery(path);
            }
         }
      }).
      setNegativeButton("Cancel", new DialogInterface.OnClickListener()
      {
         @Override public void onClick(DialogInterface dialog, int which) { }
      }).show();
   }

   private void saveQuery(File path)
   //------------------------------
   {
      PrintWriter pw = null;
      try
      {
         pw = new PrintWriter(new FileWriter(path));
         String query = null, endpoint = "", defaultUris = "", namedUris = "";
         Editable queryEd = null, endEd = null, uriEd = null, namedEd = null;
         if (queryFragment == null)
         {
            QueryFragment queryFrag = (QueryFragment) getFragmentManager().findFragmentByTag("query");
            if (queryFrag == null)
            {
               Toast.makeText(this, "Could not obtain query", Toast.LENGTH_LONG);
               return;
            }
            queryEd = queryFrag.textQuery.getText();
            endEd = queryFrag.autocompleteEndPoint.getText();
            uriEd = queryFrag.textDefaultGraph.getText();
            namedEd = queryFrag.textNamedGraph.getText();
         }
         else
         {
            queryEd = queryFragment.textQuery.getText();
            endEd = queryFragment.autocompleteEndPoint.getText();
            uriEd = queryFragment.textDefaultGraph.getText();
            namedEd = queryFragment.textNamedGraph.getText();
         }
         if (queryEd == null)
         {
            Toast.makeText(this, "Could not obtain query", Toast.LENGTH_LONG);
            return;
         }
         query = queryEd.toString();
         if ( (query == null) || (query.trim().isEmpty()) )
         {
            Toast.makeText(this, "Specify a query first", Toast.LENGTH_LONG);
            return;
         }
         if (endEd != null)
            endpoint = endEd.toString();
         if (uriEd != null)
            defaultUris = uriEd.toString();
         if (namedEd != null)
            namedUris = namedEd.toString();
         pw.println(query);
         pw.println(SAVE_DELIM);
         pw.println(endpoint);
         pw.println(SAVE_DELIM);
         pw.println(defaultUris);
         pw.println(SAVE_DELIM);
         pw.println(namedUris);
         pw.println(SAVE_DELIM);
      }
      catch (IOException e)
      {
         Toast.makeText(this, "Exception opening file " + path.getAbsolutePath() + " (" + e.getMessage() + ")", Toast.LENGTH_LONG);
      }
      finally
      {
         if (pw != null)
            try { pw.close(); } catch (Exception _e) {}
      }
   }

   @Override
   public void onDialogClosed(File dir, String filename, boolean isCancelled)
   //------------------------------------------------------------------------
   {
      if (! isCancelled)
      {
         File path = new File(dir, filename);
         loadQuery(path);
      }
   }

   private String loadField(BufferedReader br) throws IOException
   //------------------------------------------------------------
   {
      String line;
      StringBuilder sb = new StringBuilder();
      while ( (line = br.readLine()) != null )
      {
         if (line.trim().equals(SAVE_DELIM))
            break;
         sb.append(line).append("\n");
      }
      if (sb.length() > 0)
         sb.deleteCharAt(sb.length()-1);
      return sb.toString();
   }

   private void loadQuery(File path)
   //-------------------------------
   {
      BufferedReader br = null;
      try
      {
         br = new BufferedReader(new FileReader(path));
         String query = loadField(br);
         String endpoint = loadField(br);
         String defaultUri = loadField(br);
         String namedUri = loadField(br);
         try
         {
            endPoint = new URI(endpoint);
            endPointString = endpoint;
         }
         catch (Exception _e)
         {
            endPoint = null;
            endPointString = null;
         }
         if (queryFragment == null)
         {
            QueryFragment queryFrag = (QueryFragment) getFragmentManager().findFragmentByTag("query");
            if (queryFrag == null)
            {
               Toast.makeText(this, "Could not obtain query", Toast.LENGTH_LONG);
               return;
            }
            queryFrag.textQuery.setText(query);
            queryFrag.autocompleteEndPoint.setText(endpoint);
            queryFrag.textDefaultGraph.setText(defaultUri);
            queryFrag.textNamedGraph.setText(namedUri);
         }
         else
         {
            queryFragment.textQuery.setText(query);
            queryFragment.autocompleteEndPoint.setText(endpoint);
            queryFragment.textDefaultGraph.setText(defaultUri);
            queryFragment.textNamedGraph.setText(namedUri);
         }
      }
      catch (Exception e)
      {
         Toast.makeText(this, "Exception opening file " + path.getAbsolutePath() + " (" + e.getMessage() + ")", Toast.LENGTH_LONG);
      }
      finally
      {
         if (br != null)
            try { br.close(); } catch (Exception _e) {}
      }
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
               Bundle b = new Bundle();
               if (endPoint != null)
                  b.putString("endPoint", endPoint.toString());
               if (lastQuery != null)
                  b.putString("query", lastQuery);
               queryFragment = (QueryFragment) Fragment.instantiate(this, QueryFragment.class.getName(), b);
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
               if (lastColumns != null)
                  b.putStringArrayList("columns", lastColumns);
               resultFragment = (ResultFragment) Fragment.instantiate(this, ResultFragment.class.getName(), b);
               ft.add(R.id.tabContents, resultFragment, "results");
            }
            else
               ft.attach(resultFragment);
            ft.show(resultFragment);
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

   protected class RestrictedCharFilter implements InputFilter
   //=========================================================
   {
      public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend)
      //-----------------------------------------------------------------------------------------------------
      {
         for (int i = start; i < end; i++)
         {
            char ch = source.charAt(i);
            if ( (! Character.isLetterOrDigit(ch)) && (ch != '_') && (ch != '-') && (ch != ' ') )
               return "";
         }
         return null;
      }
   }
}
