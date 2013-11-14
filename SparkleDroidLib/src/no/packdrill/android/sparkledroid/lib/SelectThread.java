package no.packdrill.android.sparkledroid.lib;

import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import android.net.*;
import android.os.*;
import no.packdrill.android.sparkledroid.lib.parse.*;

import java.io.*;
import java.util.*;

public class SelectThread extends QueryThread implements Runnable
//================================================================
{
   SQLiteDatabase database = null;
   String tableName = null;
   Map<String, String> databaseColumnNames = null;

   SelectThread(long handle, Uri uri, SparQLQuery.HTTP_METHOD method,
                SparQLQuery.ACCEPT_ENCODING encoding, byte[] postData, int connectionTimeout, int readTimeOut,
                Handler handler, int handlerWhat)
   //---------------------------------------------------------------------------------------------------------
   {
      super(handle, uri, method, encoding, postData, connectionTimeout, readTimeOut, handler, handlerWhat, null);
   }

   SelectThread(long handle, Uri uri, SparQLQuery.HTTP_METHOD method,
                SparQLQuery.ACCEPT_ENCODING encoding, byte[] postData, int connectionTimeout, int readTimeOut,
                SQLiteDatabase database, String tableName)
   //---------------------------------------------------------------------------------------------------------
   {
      this(handle, uri, method, encoding, postData, connectionTimeout, readTimeOut, null, 0, database, tableName, null);
   }

   SelectThread(long handle, Uri uri, SparQLQuery.HTTP_METHOD method,
                SparQLQuery.ACCEPT_ENCODING encoding, byte[] postData, int connectionTimeout, int readTimeOut,
                Handler handler, int handlerWhat, SQLiteDatabase database, String tableName)
   //---------------------------------------------------------------------------------------------------------
   {
      this(handle, uri, method, encoding, postData, connectionTimeout, readTimeOut, handler, handlerWhat,
           database, tableName, null);
   }

   SelectThread(long handle, Uri uri, SparQLQuery.HTTP_METHOD method,
                SparQLQuery.ACCEPT_ENCODING encoding, byte[] postData, int connectionTimeout, int readTimeOut,
                Handler handler, int handlerWhat, SQLiteDatabase database, String tableName,
                Map<String, String> databaseColumnNames)
   //---------------------------------------------------------------------------------------------------------
   {
      super(handle, uri, method, encoding, postData, connectionTimeout, readTimeOut, handler, handlerWhat, null);
      this.database = database;
      this.tableName = tableName;
      this.databaseColumnNames = databaseColumnNames;
   }

   SelectThread(long handle, Uri uri, SparQLQuery.HTTP_METHOD method,
                SparQLQuery.ACCEPT_ENCODING encoding, byte[] postData, int connectionTimeout, int readTimeOut,
                Handler handler, int handlerWhat, File outputFile)
   //---------------------------------------------------------------------------------------------------------
   {
      super(handle, uri, method, encoding, postData, connectionTimeout, readTimeOut, handler, handlerWhat, outputFile);
   }

   @Override public void run() { super.run(); }

   @Override
   protected void parse(BufferedReader response) throws IOException, SQLException
   //------------------------------------------------------------------------------
   {

      Parseable parser = SparQLQuery.getParser(encoding);
      if (parser == null)
      {
         onError("ERROR: Could not obtain a parser for " +  encoding.toString());
         return;
      }
      try
      {
         parser.parse(response);
         Bundle b = new Bundle();
         String[] columns = parser.projectionNames();
         if (columns == null)
         {
            onError("ERROR: Could not read column heading");
            return;
         }
         if (database != null)
            createTable(database, tableName, columns, databaseColumnNames);
         if (handler != null)
         {
            b.putStringArray(SparQLQuery.COLUMNS_NAME_KEY, columns);
            Message message = Message.obtain(handler, handlerWhat, 0, 0, new Integer(columns.length));
            message.setData(b);
            handler.sendMessage(message);
         }
         ContentValues databaseRowValues = new ContentValues();
         long seq = 0;
         for (Iterator<Map<String, Cell>> it = parser.iterator(); it.hasNext();)
         {
            Map<String, Cell> m = it.next();
            if (m == null)
               break;
            boolean isEmpty = true;
            for (Cell ri : m.values())
            {
               if (ri != null)
               {
                  isEmpty = false;
                  break;
               }
            }
            if (! isEmpty)
            {
               if (database != null)
               {
                  Set<Map.Entry<String, Cell>> es = m.entrySet();
                  databaseRowValues.clear();
                  databaseRowValues.put("__SEQ__", seq++);
                  for (Map.Entry<String, Cell> e : es)
                  {
                     String col = e.getKey();
                     Cell ri = e.getValue();
                     String column = col;
                     if (databaseColumnNames != null)
                     {
                        column = databaseColumnNames.get(col);
                        if (column == null)
                           column = col;
                     }
                     databaseRowValues.put(column, ri.getStringValue());
                  }
                  database.insert(tableName, null, databaseRowValues);
               }
               if (handler != null)
               {
                  Message message = Message.obtain(handler, handlerWhat, 1, 0, m);
                  handler.sendMessage(message);
               }
            }
         }
         if (handler != null)
         {
            Message message = Message.obtain(handler, handlerWhat,  2, 0, "");
            handler.sendMessage(message);
         }
      }
      finally
      {
         try { parser.close(); } catch (Exception _e) {}
      }
   }
}
