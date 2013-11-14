package no.packdrill.android.sparkledroid.sample;

import android.annotation.*;
import android.app.*;
import android.database.*;
import android.database.sqlite.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.widget.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.*;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class ResultFragment extends Fragment implements QueryViewable
//====================================================================
{
   static final String LOGTAG = SparkleSampleActivity.class.getSimpleName();
   static final int REFRESH_SIZE = 10;

   SparkleSampleActivity activity = null;

   Cursor cursor = null;
   private ExpandableListView listView = null;
   private QueryListAdapter queryListAdapter = null;

   public ResultFragment() { }

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle b)
   //------------------------------------------------------------------------------
   {
      View v = null;
      try
      {
         v = inflater.inflate(R.layout.result_fragment, container, false);
      }
      catch (final Throwable e)
      {
         Log.e(LOGTAG, "Expanding layout", e);
         return  null;
      }
      Model.get().addView(this);
      listView = (ExpandableListView) v.findViewById(R.id.expandableListViewResults);
      return v;
   }

   @Override
   public void onActivityCreated(Bundle b)
   //-------------------------------------
   {
      super.onActivityCreated(b);
      activity = (SparkleSampleActivity) getActivity();
      QueryFragment queryFragment = (QueryFragment) getFragmentManager().findFragmentByTag("query");
      if (queryFragment != null)
      {
         List<String> L = QueryFragment.SAMPLE_ENDPOINTS.get(activity.getEndPoint());
         if (L != null)
         {
            String description = L.get(3);
            if (description != null)
            {
               TextView textHeader = new TextView(activity);
               textHeader.setText(description);;
               listView.addHeaderView(textHeader);
            }
         }
      }
      if (queryListAdapter == null)
      {
         queryListAdapter = new QueryListAdapter();
         listView.setAdapter(queryListAdapter);
         if (b == null)
            b = getArguments();
         if (b != null)
         {
            ArrayList<String> al = b.getStringArrayList("columns");
            if (al != null)
            {
               queryListAdapter.useDatabase = SparkleSampleApp.USE_DATABASE;
               queryListAdapter.columns = al;
               if (SparkleSampleApp.USE_DATABASE)
                  queryListAdapter.init(al);
               else
               {
                  int rowcount = b.getInt("rowcount");
                  Map<String, String> m = new HashMap<String, String>(rowcount);
                  for (int i=0; i<rowcount; i++)
                  {
                     String[] pair = b.getStringArray("row" + i);
                     m.put(pair[0], pair[1]);
                  }
                  queryListAdapter.rows.add(m);
                  queryListAdapter.notifyDataSetChanged();
               }
            }
         }
      }
      else
         listView.setAdapter(queryListAdapter);
//      getView().requestLayout();
//      listView.requestLayout();
//      listView.invalidate();
//      getView().invalidate();
   }

   @Override
   public void onSaveInstanceState(Bundle b)
   //---------------------------------------
   {
//      super.onSaveInstanceState(b);
      if (queryListAdapter != null)
         b.putStringArrayList("columns", queryListAdapter.columns);
      if (! SparkleSampleApp.USE_DATABASE)
      {
         b.putInt("rowcount", queryListAdapter.rows.size());
         int i = 0;
         for (Map<String, String> m : queryListAdapter.rows)
         {
            Set<Map.Entry<String, String>> es = m.entrySet();
            int j = 0;
            for (Map.Entry<String, String> e : es)
            {
               String[] pair = new String[2];
               pair[0] = e.getKey();
               pair[1] = e.getValue();
               b.putStringArray("row" + i + "-" + j, pair);
            }
         }
      }
   }


   @Override
   public void onDestroy()
   //---------------------
   {
      super.onDestroy();
      if ( (cursor != null) && (! cursor.isClosed()) )
         try { cursor.close(); } catch (Exception _e) {}
   }

   @Override
   public void error(String message, Throwable exception)
   //----------------------------------------------------
   {
      if (queryListAdapter != null)
         queryListAdapter.error(message, exception);
   }

   @Override
   public void selectInit(ArrayList<String> columns)
   //-----------------------------------------
   {
      if (queryListAdapter != null)
      {
         listView.setAdapter((ExpandableListAdapter) null);
         queryListAdapter.close();
         queryListAdapter.init(columns);
         listView.setAdapter(queryListAdapter);
      }
   }

   int refreshCount = 0;

   @Override
   public void selectResult(Map<String, String> row)
   //-----------------------------------------
   {
      if (queryListAdapter != null)
      {
         if (! SparkleSampleApp.USE_DATABASE)
            queryListAdapter.add(row);
         if (refreshCount++ > REFRESH_SIZE)
         {
            if (SparkleSampleApp.USE_DATABASE)
               new Requery().execute(activity.getTableName());

            queryListAdapter.notifyDataSetChanged();
            refreshCount = 0;
         }
      }
   }

   @Override
   public void askResult(Boolean B)
   //------------------------------
   {
      ArrayList<String> columns = new ArrayList<String>();
      columns.add("Result");
      if (queryListAdapter != null)
      {
         listView.setAdapter((ExpandableListAdapter) null);
         queryListAdapter.close();
         queryListAdapter.useDatabase = false;
         queryListAdapter.rows.clear();
         queryListAdapter.columns = columns;
         Map<String, String> row = new HashMap<String, String>(1);
         row.put("Result", B.toString());
         queryListAdapter.rows.add(row);
         listView.setAdapter(queryListAdapter);
         queryListAdapter.notifyDataSetChanged();
      }
   }

   public void constructResult(File f)
   //---------------------------------
   {
      ArrayList<String> columns = new ArrayList<String>();
      columns.add("File");
      if (queryListAdapter != null)
      {
         listView.setAdapter((ExpandableListAdapter) null);
         queryListAdapter.close();
         queryListAdapter.useDatabase = false;
         queryListAdapter.rows.clear();
         queryListAdapter.columns = columns;
         Map<String, String> row = new HashMap<String, String>(1);
         final String s = f.getAbsolutePath() + " created from CONSTRUCT";
         row.put("File", s);
         queryListAdapter.rows.add(row);
         listView.setAdapter(queryListAdapter);
         queryListAdapter.notifyDataSetChanged();
         Toast.makeText(activity, s, Toast.LENGTH_LONG).show();
      }
   }

   @Override public void refresh()
   //-----------------------------
   {
      if (SparkleSampleApp.USE_DATABASE)
         if (activity != null)
            new Requery().execute(activity.getTableName());
      if (queryListAdapter != null)
         queryListAdapter.notifyDataSetChanged();
   }

   @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
   class QueryListAdapter extends BaseExpandableListAdapter implements ExpandableListAdapter
   //=======================================================================================
   {
      ArrayList<String> columns = new ArrayList<String>();
      List<Map<String, String>> rows = new ArrayList<Map<String, String>>();
      boolean useDatabase = SparkleSampleApp.USE_DATABASE;
      LruCache<Integer, Map<String, String>> dbCache = new LruCache<Integer, Map<String, String>>(200);
      String errorMessage = null;
      Throwable exception = null;
      private int lastCount = 0;

      public QueryListAdapter()
      {
         Log.i(LOGTAG, "QueryListAdapter constructed");
      }

      @Override public void onGroupExpanded(int groupPosition) { super.onGroupExpanded(groupPosition); }

      public void init(ArrayList<String> columns)
      //------------------------------------
      {
         this.rows.clear();
         this.columns = columns;
         useDatabase = SparkleSampleApp.USE_DATABASE;
         if (useDatabase)
            new Requery().execute(activity.getTableName());
      }

      public void close()
      //-----------------
      {
         if ( (cursor != null) && (! cursor.isClosed()) )
            try { cursor.close(); } catch (Exception _e) {}
      }

      public void add(Map<String, String> row) { rows.add(row);}

      public void error(String message, Throwable exception)
      //----------------------------------------------------
      {
         if (useDatabase)
         {
            useDatabase = false;
            if (cursor != null)
               try { cursor.close(); } catch (Exception _e) {}
            cursor = null;
         }
         errorMessage = message;
         this.exception = exception;
         columns.clear();
         columns.add("Error");
         rows.clear();
         Map<String, String> row = new HashMap<String, String>(1);
         row.put("Error", message);
         rows.add(row);
      }

      @Override
      public int getGroupCount()
      //------------------------
      {
         if (useDatabase)
         {
            if ( (cursor == null) || (cursor.isClosed()) )
               return 0;
            cursor.moveToLast();
            lastCount = cursor.getCount();
            return lastCount;
         }
         else
            return rows.size();
      }

      @Override
      public int getChildrenCount(int groupPosition)
      //--------------------------------------------
      {
         if (useDatabase)
         {
            int c = 0;
            Map<String, String> row = dbCache.get(groupPosition);
            if (row != null)
            {
               for (String column : columns)
                  if (row.get(column) != null)
                     c++;
            }
            else if ( (cursor != null) && (! cursor.isClosed()) && (cursor.moveToPosition(groupPosition)) )
            {
               for (String column : columns)
               {
                  int col = cursor.getColumnIndex(column);
                  String v = null;
                  if (col > 0)
                     v = cursor.getString(col);
                  if (v != null)
                     c++;
               }
            }
            return c;
         }
         else
            return (groupPosition >= rows.size()) ? 0 : rows.get(groupPosition).size();
      }

      @Override
      public Object getGroup(int groupPosition)
      //---------------------------------------
      {
         StringBuilder sb = new StringBuilder();
         Map<String, String> row;
         if (useDatabase)
         {
            row = dbCache.get(groupPosition);
            if (row == null)
            {
               if ( (cursor == null) || (cursor.isClosed()) )
                  return "Please wait....";
               row = new HashMap<String, String>();
               if (cursor.moveToPosition(groupPosition))
               {
                  for (String column : columns)
                  {
                     int col = cursor.getColumnIndex(column);
                     String v = null;
                     if (col > 0)
                        v = cursor.getString(col);
                     if (v != null)
                        row.put(column, v);
                  }
                  dbCache.put(groupPosition, row);
               }
            }
         }
         else
            row = rows.get(groupPosition);
         if ( (row == null) || (row.size() == 0) ) return "";
         for (String column : columns)
         {
            String v = row.get(column);
            if (v == null) continue;
            v = v.trim();
            if (v.indexOf("://") > 0)
            {
               int p = v.lastIndexOf('#');
               if (p < 0)
                  p = v.lastIndexOf('/');
               if ( (p >= 0) && (v.length() > (p+1)) )
                  v = v.substring(p+1);
            }
            if (v.length() > 60)
               v = v.substring(0, 60);
            sb.append(v).append(" | ");
         }
         return sb.toString();
      }

      @Override
      public Object getChild(int groupPosition, int childPosition)
      //----------------------------------------------------------
      {
         if (columns.size() >= childPosition) return "";
         String column = columns.get(childPosition);
         Map<String, String> row = null;
         String v = null;
         if (useDatabase)
         {
            row = dbCache.get(groupPosition);
            if (row == null)
            {
               if ( (cursor == null) || (cursor.isClosed()) )return "";
               if (cursor.moveToPosition(groupPosition))
               {
                  int col = cursor.getColumnIndex(column);
                  if (col > 0)
                     v = cursor.getString(col);
               }
            }
            else
               v = row.get(column);
         }
         else
         {
            if (rows.size() >= groupPosition)
               return "";
            row = rows.get(groupPosition);
            if (row == null) return "";
            v = row.get(column);
         }
         if (v == null)
            return "";
         return v.trim();
      }

      @Override public long getGroupId(int groupPosition) { return groupPosition;  }

      @Override public long getChildId(int groupPosition, int childPosition)
      //--------------------------------------------------------------------
      {
         return groupPosition*columns.size() + childPosition;
      }

      @Override public boolean hasStableIds() { return false; }

      @Override
      public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent)
      //-------------------------------------------------------------------------------------------------
      {
         if (convertView == null)
         {
            LayoutInflater inflater = activity.getLayoutInflater();
            convertView = inflater.inflate(R.layout.list_group_item, null);
         }
         TextView tv = (TextView) (convertView != null ? convertView.findViewById(R.id.textViewRow) : null);
         if (tv != null)
            tv.setText((CharSequence) getGroup(groupPosition));
         return convertView;
      }

      Map<Integer, String> childIndexCache = new HashMap<Integer, String>();

      @Override
      public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView,
                               ViewGroup parent)
      //---------------------------------------------------------------------------------------------------
      {
         Map<String, String> row;
         if (useDatabase)
         {
            row = dbCache.get(groupPosition);
            if (row == null)
            {
               if (cursor == null) return new TextView(activity);
               row = new HashMap<String, String>();
               if (cursor.moveToPosition(groupPosition))
               {
                  for (String column : columns)
                  {
                     int col = cursor.getColumnIndex(column);
                     String v = null;
                     if (col > 0)
                        v = cursor.getString(col);
                     if (v != null)
                        row.put(column, v);
                  }
                  dbCache.put(groupPosition, row);
               }
            }
         }
         else
         {
            row = rows.get(groupPosition);
            if (row == null)
               return new TextView(activity);
         }
         final LinearLayout layout;
         if (convertView == null)
         {
            LayoutInflater inflater = activity.getLayoutInflater();
            layout = (LinearLayout) inflater.inflate(R.layout.list_child_item, null);
         }
         else
            layout = (LinearLayout) convertView;
         String column = childIndexCache.get(childPosition);
         if (column == null)
         {
            int i = 0;
            Set<Map.Entry<String, String>> ss = row.entrySet();
            for (Map.Entry<String, String> se : ss)
            {
               if (i == childPosition)
               {
                  column = se.getKey();
                  childIndexCache.put(childPosition, column);
                  break;
               }
               i++;
            }
         }
         if (column != null)
         {
            TextView tv = (TextView) layout.findViewById(R.id.textViewColumnName);
            tv.setText(column);
            String v = row.get(column);
            tv = (TextView) layout.findViewById(R.id.textViewColumnValue);
            if (v != null)
               tv.setText(v);
            else
               tv.setText("null");
         }
         return layout;
      }

      @Override public boolean isChildSelectable(int groupPosition, int childPosition) { return true;  }
   }

   class Requery extends AsyncTask<String, Void, Cursor>
   //=================================================
   {
      @Override
      protected Cursor doInBackground(String... params)
      //-----------------------------------------------
      {
         try
         {
            SQLiteDatabase db = ((SparkleSampleApp) activity.getApplication()).getDatabase();
            if (db != null)
               return db.query(params[0], null, null, null, null, null, "__SEQ__");
         }
         catch (Exception e)
         {
            Log.e(LOGTAG, params[0], e);
            return null;
         }
         return null;
      }

      @Override protected void onPostExecute(Cursor C)
      //----------------------------------------------
      {
         if (cursor != null)
            try { cursor.close(); } catch (Exception _e) {}
         Log.i(LOGTAG, "onPostExecute " + C);
         if (C != null)
            cursor = C;
         else
            Log.e(LOGTAG, "C null");
         if (queryListAdapter != null)
            queryListAdapter.notifyDataSetChanged();
      }
   }
}
