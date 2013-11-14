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

import android.database.sqlite.*;
import android.os.*;
import android.util.*;
import no.packdrill.android.sparkledroid.lib.*;
import no.packdrill.android.sparkledroid.lib.parse.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.*;

public class Model
//================
{
   final static String LOGTAG = Model.class.getSimpleName();

   private static final class SingletonHolder
   {
      static Model singleton = new Model();
   }

   public static Model get() { return SingletonHolder.singleton; }

   Set<QueryViewable> views = new HashSet<QueryViewable>();

   static AtomicInteger HANDLER_IDS = new AtomicInteger(0);

   private Model() {  }

   public void addView(QueryViewable v) { views.add(v);}

   public boolean query(String query, String[] defaultUris, String[] namedUris, URI endPoint,
                        SparQLQuery.HTTP_METHOD method, SparQLQuery.ACCEPT_ENCODING encoding, StringBuilder errbuf)
   //--------------------------------------------------------------------------------------------------------------
   {
      return query(query, defaultUris, namedUris, endPoint, method, encoding, null, null, errbuf);
   }

   public boolean query(String query, String[] defaultUris, String[] namedUris,
                     URI endPoint, SparQLQuery.HTTP_METHOD method, SparQLQuery.ACCEPT_ENCODING encoding,
                     SQLiteDatabase db, String tableName, StringBuilder errbuf)
   //---------------------------------------------------------------------------------------------------
   {
      if ( (query == null) || (query.trim().isEmpty()) )
      {
         if (errbuf != null) errbuf.append("Query must be specified");
         return false;
      }
      final int id = HANDLER_IDS.getAndIncrement();
      Handler queryHandler = null;
      try
      {
         SparQLQuery sparQLQuery = new SparQLQuery(endPoint);
         if (SparQLQuery.isSelect(query))
         {
            queryHandler = new Handler(new SelectHandlerCallback(id));
            sparQLQuery.select(query, defaultUris, namedUris, method, encoding, queryHandler, id, db, tableName);
         }
         else if (SparQLQuery.isAsk(query))
         {
            queryHandler = new Handler(new AskHandlerCallback(id));
            sparQLQuery.ask(query, defaultUris, namedUris, method, encoding, queryHandler, id);
         }
         else
         {
            if (errbuf != null) errbuf.append("Query must be one of SELECT, ASK or CONSTRUCT");
            return false;
         }
      }
      catch (Exception e)
      {
         if (errbuf != null) errbuf.append("Exception ").append(e.getClass().getSimpleName()).append(": ").append(e.getMessage());
         Log.e(LOGTAG, e.getMessage(), e);
         for (QueryViewable view : views)
         {
            view.error(e.getMessage(), e);
            view.refresh();
         }
         return false;
      }
      return true;
   }

   public boolean save(String query, String[] defaultUris, String[] namedUris,
                       URI endPoint, SparQLQuery.HTTP_METHOD method, SparQLQuery.ACCEPT_ENCODING encoding,
                       File outputFile, StringBuilder errbuf)
   //---------------------------------------------------------------------------------------------------
   {
      if ( (query == null) || (query.trim().isEmpty()) )
      {
         if (errbuf != null) errbuf.append("Query must be specified");
         return false;
      }
      final int id = HANDLER_IDS.getAndIncrement();
      Handler queryHandler = null;
      try
      {
         SparQLQuery sparQLQuery = new SparQLQuery(endPoint);
         if (SparQLQuery.isSelect(query))
         {
            queryHandler = new Handler(new SelectHandlerCallback(id));
            sparQLQuery.select(query, defaultUris, namedUris, method, encoding, queryHandler, id, outputFile, null, null);
         }
         else if (SparQLQuery.isAsk(query))
         {
            queryHandler = new Handler(new AskHandlerCallback(id));
            sparQLQuery.ask(query, defaultUris, namedUris, method, encoding, queryHandler, id, outputFile);
         }
         else if (SparQLQuery.isConstruct(query))
         {
            queryHandler = new Handler(new ConstructHandlerCallback(id));
            sparQLQuery.construct(query, defaultUris, namedUris, method, encoding, queryHandler, id, outputFile);
         }
         else
         {
            if (errbuf != null) errbuf.append("Query must be one of SELECT, ASK or CONSTRUCT");
            return false;
         }
      }
      catch (Exception e)
      {
         if (errbuf != null) errbuf.append("Exception ").append(e.getClass().getSimpleName()).append(": ").append(e.getMessage());
         Log.e(LOGTAG, e.getMessage(), e);
         for (QueryViewable view : views)
         {
            view.error(e.getMessage(), e);
            view.refresh();
         }
         return false;
      }
      return true;
   }

   class SelectHandlerCallback implements Handler.Callback
   //=====================================================
   {
      int handlerId = -1;

      SelectHandlerCallback(int handlerId) { this.handlerId = handlerId; }

      @Override
      public boolean handleMessage(Message msg)
      //---------------------------------------
      {
         if (msg.what != handlerId) return false;
         String[] columns = null;
         int cols;
         switch (msg.arg1)
         {
            case 0:
               try { cols = ((Integer) msg.obj).intValue(); } catch (Exception _e) { return false; }
               columns = (msg.getData() == null) ? null : msg.getData().getStringArray(SparQLQuery.COLUMNS_NAME_KEY);
               ArrayList<String> collist = new ArrayList<String>(columns.length);
               for (String s : columns)
                  collist.add(s);
               for (QueryViewable view : views)
                  view.selectInit(collist);
               return true;

            case 1:
               Map<String, Cell> m = null;
               try { m = (Map<String, Cell>) msg.obj; } catch (ClassCastException _e) { m = null; }
               if (m == null)
               {
                  Log.e(LOGTAG, "Handler received invalid type");
                  return true;
               }
               Map<String, String> row = new HashMap<String, String>(m.size());
               Set<Map.Entry<String, Cell>> ss = m.entrySet();
               for (Map.Entry<String, Cell> e : ss)
               {
                  String k = e.getKey();
                  Cell ri = e.getValue();
                  if ( (k != null) && (ri != null) )
                     row.put(k, ri.getStringValue());
               }
               for (QueryViewable view : views)
                  view.selectResult(row);
               return true;

            case 2:
               for (QueryViewable view : views)
                  view.refresh();
               return true;

            case -1:
               String err = (String) msg.obj;
               for (QueryViewable view : views)
               {
                  view.error(err, null);
                  view.refresh();
               }
               return true;
         }
         return false;
      }
   }

   class AskHandlerCallback implements Handler.Callback
   //==================================================
   {
      int handlerId = -1;

      AskHandlerCallback(int handlerId) { this.handlerId = handlerId; }

      @Override
      public boolean handleMessage(Message msg)
      //---------------------------------------
      {
         if (msg.what != handlerId) return false;
         switch (msg.arg1)
         {
            case 0:
               Boolean B = null;
               try { B = (Boolean) msg.obj; } catch (Exception _e) { return false; }
               for (QueryViewable view : views)
                  view.askResult(B);
               return true;

            case -1:
               String err = (String) msg.obj;
               for (QueryViewable view : views)
               {
                  view.error(err, null);
                  view.refresh();
               }
               return true;
         }
         return false;
      }
   }

   class ConstructHandlerCallback implements Handler.Callback
   //========================================================
   {
      int handlerId = -1;

      public ConstructHandlerCallback(int handlerId) { this.handlerId = handlerId; }

      @Override
      public boolean handleMessage(Message msg)
      //---------------------------------------
      {
         if (msg.what != handlerId) return false;
         switch (msg.arg1)
         {
            case 3:
               String filename;
               try { filename = (String) msg.obj; } catch (Exception _e) { filename = null; }
               if ( (filename != null) && (! filename.trim().isEmpty()) )
               {
                  File f = new File(filename);
                  if (f.exists())
                  {
                     for (QueryViewable view : views)
                        view.constructResult(f);
                     return true;
                  }
               }
               return false;

            case -1:
               String err = (String) msg.obj;
               for (QueryViewable view : views)
               {
                  view.error(err, null);
                  view.refresh();
               }
               return true;
         }
         return false;
      }
   }
}
