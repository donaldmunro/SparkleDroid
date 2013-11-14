package no.packdrill.android.sparkledroid.lib;

import android.database.*;
import android.database.sqlite.*;
import android.net.*;
import android.os.*;
import android.util.*;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Base class for all threads implementing SPARQL operations.
 */
abstract class QueryThread implements Runnable
//===================================
{
   static final String LOGTAG = QueryThread.class.getSimpleName();

   protected Uri uri;
   protected long handle;
   protected SparQLQuery.HTTP_METHOD method;
   protected SparQLQuery.ACCEPT_ENCODING encoding;
   protected byte[] postData = null;
   protected HttpURLConnection connection;
   protected final Handler handler;
   protected File outputFile = null;
   protected int handlerWhat;
   protected int connectionTimeout = 60000, readTimeOut = 45000;

   /**
    * Constructor for a QueryThread
    * @param handle  A handle identifying the task/thread.
    * @param uri The URI of the SPARQL server along with the query, default graph and named graph embedded in the URI.
    * @param method The HTTP method (GET or POST).
    * @param encoding The HTTP ACCEPT encoding
    * @param postData The urlencoded data to be sent in the request body if this is a POST method.
    * @param connectionTimeout The connection timeout in milliseconds. Default 60000 (1 minute)
    * @param readTimeOut The read timeout in milliseconds. Default 45000 (45 seconds)
    * @param handler The Handler (@see android.os.Handler, @see SparQLQuery)
    * @param handlerWhat The identifier to place int the Message what field (@see SparQLQuery)
    * @param outputFile File to write unparsed data from the server to. If this is not null then the data will not
    *                   be parsed.
    */
   QueryThread(long handle, Uri uri, SparQLQuery.HTTP_METHOD method, SparQLQuery.ACCEPT_ENCODING encoding, byte[] postData,
               int connectionTimeout, int readTimeOut, Handler handler, int handlerWhat, File outputFile)
   //----------------------------------------------------------------------------------------------------
   {
      this.handle = handle;
      this.method = method;
      this.encoding = encoding;
      this.handler = handler;
      this.handlerWhat = handlerWhat;
      this.postData = postData;
      this.connectionTimeout = connectionTimeout;
      this.readTimeOut = readTimeOut;
      this.uri = uri;
      this.outputFile = outputFile;
   }

   /**
    * Method that implements Per-Operation parsing.
    * @param response The Reader instance for the unparsed data from the server.
    * @throws IOException
    * @throws SQLException
    */
   abstract protected void parse(BufferedReader response) throws IOException, SQLException;

   /**
    * Default error handling code. Sends a Message with arg1 = -1 and Message.obj = A String containing the error
    * message.
    * @param errMessage The error message.
    */
   protected void onError(String errMessage) { onError(errMessage, null);}

   /**
    * Default error handling code. Sends a Message with arg1 = -1 and Message.obj = A String containing the error
    * message.
    * @param errMessage The error message.
    * @param e The exception causing the error or null
    */
   protected void onError(String errMessage, Exception e)
   //----------------------------------------------------
   {
      if (handler != null)
      {
         Message message = Message.obtain(handler, handlerWhat,  -1, 0, errMessage);
         handler.sendMessage(message);
      }
   }

   @Override
   public void run()
   //---------------
   {
      BufferedReader response = null;
      DataOutputStream dos = null;
      try
      {
         URL url = new URL(uri.toString());
         connection = (HttpURLConnection) url.openConnection();
         connection.setInstanceFollowRedirects(true);
         connection.setRequestProperty("User-Agent", SparQLQuery.USER_AGENT);
         connection.setRequestProperty("Accept-Charset", "UTF-8");
         connection.setRequestProperty("Accept", encoding.toString());
         connection.setConnectTimeout(connectionTimeout);
         connection.setReadTimeout(readTimeOut);
         Log.i(LOGTAG, url.toExternalForm());
         switch (method)
         {
            case GET:
               connection.setRequestMethod("GET");
               break;

            case POST:
               connection.setRequestMethod("POST");
               connection.setDoInput(true);
               connection.setDoOutput(true);
               connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
               connection.setRequestProperty("Content-Length", "" + Integer.toString(postData.length));
               dos = new DataOutputStream (connection.getOutputStream ());
               dos.write(postData);
               dos.flush();
               break;
         }

         InputStream is = connection.getInputStream();
         if (is == null)
         {
            onError("ERROR: Reading from server URI " + uri.toString());
            return;
         }
         response = new BufferedReader(new InputStreamReader(is));
         StringBuilder errmsg = new StringBuilder();
         if (getError(errmsg, response) != 200)
         {
            onError(errmsg.toString());
            return;
         }
         if (outputFile != null)
            save(response);
         else
            parse(response);
      }
      catch (SocketTimeoutException e)
      {
         onError("Time out (" + e.getMessage() + ")", e);
         Log.e(LOGTAG + ".QueryThread", e.getMessage(), e);
      }
      catch (Exception e)
      {
         StringBuilder errmsg = new StringBuilder();
         final int error = getError(errmsg, response);
         onError(errmsg.toString(), e);
         Log.e(LOGTAG + ".QueryThread", e.getMessage(), e);
      }
      finally
      {
         synchronized (SparQLQuery.THREAD_MAP) { SparQLQuery.THREAD_MAP.remove(handle); }
         if (response != null)
            try { response.close(); } catch (Exception _e) {}
         if (dos != null)
            try { dos.close(); } catch (Exception _e) {}
      }
   }

   /**
    * Save the unparsed data to a specified output file.
    * @param response   A Reader for the data from the server.
    * @throws IOException
    */
   protected void save(BufferedReader response) throws IOException
   //-------------------------------------------------------------
   {
      if (outputFile != null)
      {
         String line = null;
         PrintWriter pw = null;
         try
         {
            pw = new PrintWriter(new FileWriter(outputFile));
            while ( (line = response.readLine()) != null)
               pw.println(line);
         }
         finally
         {
            if (pw != null)
               try { pw.close(); } catch (Exception _e) {}
         }
         if (handler != null)
         {
            Message message = Message.obtain(handler, handlerWhat,  3, 0, outputFile.getAbsolutePath());
            handler.sendMessage(message);
         }
      }
   }

   private int getError(StringBuilder msg, BufferedReader output)
   //-------------------------------------------------------------
   {
      int code = 0;
      BufferedReader response = null;
      InputStream is = null;
      String line = null;
      try
      {
         try { code = connection.getResponseCode(); } catch (Exception _e) { code = -1; }
         switch (code)
         {
            case 400:
            case 500:
               msg.append(connection.getResponseMessage());
               try { is = connection.getErrorStream(); } catch (Exception _e) { is = null; }
               if (is != null)
               {
                  response = new BufferedReader(new InputStreamReader(is));
                  msg.append("\n");
                  while ( (line = response.readLine()) != null)
                     msg.append(line).append(' ');
               }
               break;

            case 406:
               msg.append("406: Accept encoding ").append(encoding.toString()).append(" not accepted");
               break;

            case -1:
               try
               {
                  try { is = connection.getErrorStream(); } catch (Exception _e) { is = null; }
                  if (is != null)
                  {
                     response = new BufferedReader(new InputStreamReader(is));
                     while ( (line = response.readLine()) != null)
                        msg.append(line).append(' ');
                  }
                  else if (output != null)
                  {

                     for (int lc=0; lc<5; lc++)
                     {
                        line = output.readLine();
                        if (line == null) break;
                        msg.append(line).append(' ');
                     }
                  }
                  else
                     msg.append("ERROR: Reading response status code");
               }
               catch (Exception _e)
               {
                  msg.append("ERROR: Reading error stream: ").append(_e.getMessage());
               }
               break;
         }
      }
      catch (Exception _e)
      {
         code = -1;
         msg.append(_e.getMessage());
      }
      finally
      {
         if (response != null)
            try { response.close(); } catch (Exception _e) {}
      }
      return code;
   }

   static protected void createTable(SQLiteDatabase database, String tableName, String[] columns,
                                     Map<String, String> databaseColumnNames) throws SQLException
   //----------------------------------------------------------------------------------------------------------------
   {
      StringBuilder sql = new StringBuilder("DROP TABLE IF EXISTS ").append(tableName);
      database.execSQL(sql.toString());
      sql.setLength(0);
      sql.append("CREATE TABLE ").append(tableName).append( " ( __SEQ__ INTEGER PRIMARY KEY,");
      for (String col : columns)
      {
         String column;
         if (databaseColumnNames != null)
         {
            column = databaseColumnNames.get(col);
            if (column == null)
               column = col;
         }
         else
            column = col;
         sql.append(column).append(" TEXT,");
      }
      sql.deleteCharAt(sql.length() - 1);
      sql.append(')');
      database.execSQL(sql.toString());
   }
}