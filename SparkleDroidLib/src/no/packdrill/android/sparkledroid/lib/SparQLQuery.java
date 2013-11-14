/*
    This file is part of SparkleDroidLib. (C) 2013 Donald Munro

    SparkleDroidLib is free software: you can redistribute it and/or modify it under
    the terms of the GNU Lesser General Public License as published by the Free Software Foundation; either version 3
    of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
    implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
    See the GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License along with this library; if not,
    write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
    or see <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>
 */
package no.packdrill.android.sparkledroid.lib;

import android.database.sqlite.*;
import android.net.*;
import android.os.*;
import android.util.*;
import no.packdrill.android.sparkledroid.lib.parse.*;
import no.packdrill.android.sparkledroid.lib.parse.csvtsv.*;
import no.packdrill.android.sparkledroid.lib.parse.xml.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.regex.*;

/**
 * Provides the SPARQL query forms SELECT, ASK and CONSTRUCT.
 * See no.packdrill.android.sparkledroid.sample.Model for an example of the use of this class WRT the use of Handlers.
 */
public class SparQLQuery
//======================
{
   static final String LOGTAG = SparQLQuery.class.getSimpleName();

   static final Pattern SELECT_PATTERN = Pattern.compile(".*SELECT[\\s\\?\\$].*");

   static final Pattern CONSTRUCT_PATTERN = Pattern.compile(".*CONSTRUCT\\s*\\{.*");

   static final Pattern ASK_PATTERN = Pattern.compile(".*ASK\\s*[FROM|WHERE|{].*");

   static final private ExecutorService THREAD_POOL = Executors.newFixedThreadPool(5,
   new ThreadFactory()
   //--------------------------------------------------------------------------------
   {
      final AtomicLong counter = new AtomicLong(0);

      @Override
      public Thread newThread(Runnable r)
      //---------------------------------
      {
         Thread t = new Thread(r);
         t.setDaemon(true);
         t.setName(String.format("SparQLQuery-%d", counter.incrementAndGet()));
         t.setPriority(Thread.MIN_PRIORITY);
         return t;
      }
   });

   final static public  String COLUMNS_NAME_KEY = "columns";

   //static public String USER_AGENT = "Mozilla/5.0 (Android; Mobile; rv:13.0) Gecko/13.0 Firefox/13.0";
   static public String USER_AGENT = "SparkleDroid/0.1 (Android; Mobile;)";

   static protected AtomicLong HANDLE = new AtomicLong(1);

   static protected Map<Long, Future<?>> THREAD_MAP = new HashMap<Long, Future<?>>(5);

   int connectionTimeout = 60000;
   public int getConnectionTimeout() { return connectionTimeout; }
   public void setConnectionTimeout(int timeout) { connectionTimeout = timeout; }

   int readTimeOut = 45000;
   public int getReadTimeOut() { return readTimeOut; }
   public void setReadTimeOut(int timeOut) { readTimeOut = timeOut; }

   URI endpoint = null;

   public enum HTTP_METHOD { GET, POST }

   public enum ACCEPT_ENCODING
   {
      XML("application/sparql-results+xml"),

//      JSON("application/sparql-results+json"),

      CSV("text/csv"),

      TSV("text/tab-separated-values"),

      Turtle("text/turtle"),

      RDFXML("application/rdf+xml"),

      NTRIPLES("text/plain"),

      ANY("ANY");

      private static final Map<String, ACCEPT_ENCODING> lookup = new HashMap<String, ACCEPT_ENCODING>();

      static
      {
         for (ACCEPT_ENCODING nm : EnumSet.allOf(ACCEPT_ENCODING.class))
            lookup.put(nm.toString(), nm);
      }

      private String mime;

      private ACCEPT_ENCODING(String mime) { this.mime = mime;  }

      @Override public String toString() { return mime; }
   };

   /**
    * Create a SparQLQuery instance for a specified endpoint.
    * @param endpoint URI of the endpoint
    * @throws java.lang.RuntimeException if endpoint is null
    */
   public SparQLQuery(URI endpoint)
   //-----------------------------------------------
   {
      if (endpoint == null)
         throw new RuntimeException(getClass().getName() + ".SparQLQuery(URI, int): endpoint URI cannot be null");
      this.endpoint = endpoint;
   }

   /**
    * Check whether a query is a SELECT query.
    * @param query The query.
    * @return <i>true</i> if it is a SELECT query else <i>false</i>.
    */
   static public boolean isSelect(String query)
   //------------------------------------------
   {
      query = query.replace('\n', ' ');
      Matcher matcher = SELECT_PATTERN.matcher(query.toUpperCase());
      return matcher.matches();
   }

   /**
    * Check whether a query is an ASK query.
    * @param query The query.
    * @return <i>true</i> if it is an ASK query else <i>false</i>.
    */
   static public boolean isAsk(String query)
   //------------------------------------------
   {
      query = query.replace('\n', ' ');
      Matcher matcher = ASK_PATTERN.matcher(query.toUpperCase());
      return matcher.matches();
   }

   /**
    * Check whether a query is a CONSTRUCT query.
    * @param query The query.
    * @return <i>true</i> if it is a CONSTRUCT query else <i>false</i>.
    */
   static public boolean isConstruct(String query)
   //------------------------------------------
   {
      query = query.replace('\n', ' ');
      Matcher matcher = CONSTRUCT_PATTERN.matcher(query.toUpperCase());
      return matcher.matches();
   }

   /**
    * Start a SPARQL SELECT query. The parsed results from the query will be communicated back to the caller via the
    * Handler callback (@see android.os.Handler.Callback) as follows:
    * Initial Message
    * Message.arg1 = 0; Message.obj = Integer object containing column count Message.data = String array of columns
    * Repeated data messages
    * Message.arg1 = 1; Message.obj = Map<String, Cell> mapping column names onto values in Cells (@see Cell)
    * Completion notification message
    * Message.arg1 = 2; Message.obj = empty String
    * If an error occurs:
    * Message.arg1 = -1; Message.obj = error String
    * @param query The SPARQL SELECT query.
    * @param defaultGraphUris The default graph URI's or null or empty if none.
    * @param namedGraphUris The named graph URI's or null or empty if none.
    * @param method The HTTP method (GET or POST).
    * @param encoding The HTTP ACCEPT encoding
    * @param handler The Handler (@see android.os.Handler)
    * @param handlerWhat The identifier to place int the Message what field.
    * @return A handle identifying the task.
    * @throws IOException
    * @throws InvalidQueryFormException if this is not a SELECT query.
    */
   public long select(String query, String[] defaultGraphUris, String[] namedGraphUris,
                      HTTP_METHOD method, ACCEPT_ENCODING encoding, Handler handler, int handlerWhat)
         throws IOException, InvalidQueryFormException
   //------------------------------------------------------------------------------------------------
   {
      return select(query, defaultGraphUris, namedGraphUris, method, encoding, handler, handlerWhat, null, null, null);
   }

   /**
    * Start a SPARQL SELECT query. The results will not be parsed but instead the raw output will be written to the
    * specified file.
    * @param query The SPARQL SELECT query.
    * @param defaultGraphUris The default graph URI's or null or empty if none.
    * @param namedGraphUris The named graph URI's or null or empty if none.
    * @param method The HTTP method (GET or POST).
    * @param encoding The HTTP ACCEPT encoding
    * @param outputFile The file into which the unparsed output will be written.
    * @return A handle identifying the task.
    * @throws IOException
    * @throws InvalidQueryFormException if this is not a SELECT query.
    */
   public long select(String query, String[] defaultGraphUris, String[] namedGraphUris,
                      HTTP_METHOD method, ACCEPT_ENCODING encoding, File outputFile)
         throws IOException, InvalidQueryFormException
   //------------------------------------------------------------------------------------------------
   {
      return select(query, defaultGraphUris, namedGraphUris, method, encoding, null, -1, outputFile, null, null);
   }

   /**
    * Start a SPARQL SELECT query. The output will be parsed and written to the specified database table.
    * @param query The SPARQL SELECT query.
    * @param defaultGraphUris The default graph URI's or null or empty if none.
    * @param namedGraphUris The named graph URI's or null or empty if none.
    * @param method The HTTP method (GET or POST).
    * @param encoding The HTTP ACCEPT encoding
    * @param database The SQLiteDatabase instance.
    * @param tableName The table name. The table will be dropped if it already exists and then recreated before
    *                  inserting the data.
    * @return A handle identifying the task.
    * @throws IOException
    * @throws InvalidQueryFormException if this is not a SELECT query.
    **/
   public long select(String query, String[] defaultGraphUris, String[] namedGraphUris,
                      HTTP_METHOD method, ACCEPT_ENCODING encoding, SQLiteDatabase database, String tableName)
         throws IOException, InvalidQueryFormException
   //-----------------------------------------------------------------------------------
   {
      return select(query, defaultGraphUris, namedGraphUris, method, encoding, null, 0, null, database, tableName);
   }

   /**
    * Start a SPARQL SELECT query. If an not null Handler is specified then the parsed results from the query will be
    * communicated back to the caller via the Handler callback (@see android.os.Handler.Callback) as follows:
    * Initial Message
    * Message.arg1 = 0; Message.obj = Integer object containing column count Message.data = String array of columns
    * Repeated data messages
    * Message.arg1 = 1; Message.obj = Map<String, Cell> mapping column names onto values in Cells (@see Cell)
    * Completion notification message
    * Message.arg1 = 2; Message.obj = empty String
    * If an error occurs:
    * Message.arg1 = -1; Message.obj = error String
    * The output will also be written to to the specified database table. UI programs using this interface can therefore
    * refresh a cursor from this table on receiving notification messages in the Handler.
    * @param query The SPARQL SELECT query.
    * @param defaultGraphUris The default graph URI's or null or empty if none.
    * @param namedGraphUris The named graph URI's or null or empty if none.
    * @param method The HTTP method (GET or POST).
    * @param encoding The HTTP ACCEPT encoding
    * @param handler The Handler (@see android.os.Handler)
    * @param handlerWhat The identifier to place int the Message what field.
    * @param database The SQLiteDatabase instance.
    * @param tableName The table name. The table will be dropped if it already exists and then recreated before
    *                  inserting the data.
    * @return A handle identifying the task.
    * @throws IOException
    * @throws InvalidQueryFormException if this is not a SELECT query.
    **/
   public long select(String query, String[] defaultGraphUris, String[] namedGraphUris,
                      HTTP_METHOD method, ACCEPT_ENCODING encoding,
                      Handler handler, int handlerWhat, SQLiteDatabase database, String tableName)
         throws IOException, InvalidQueryFormException
   //----------------------------------------------------------------------------------------------
   {
      return select(query, defaultGraphUris, namedGraphUris, method, encoding, handler, handlerWhat,  null,
                    database, tableName);
   }

   /**
    * Method that implements a SPARQL SELECT query (the public select methods delegate to this method).
    * If a not null output file is specified then results will not be parsed but instead the raw output will be written
    * to the specified file else the results will be parsed.
    * If a not null Handler is specified then the parsed results from the query will be
    * communicated back to the caller via the Handler callback (@see android.os.Handler.Callback) as follows:
    * Initial Message
    * Message.arg1 = 0; Message.obj = Integer object containing column count Message.data = String array of columns
    * Repeated data messages
    * Message.arg1 = 1; Message.obj = Map<String, Cell> mapping column names onto values in Cells (@see Cell)
    * Completion notification message
    * Message.arg1 = 2; Message.obj = empty String
    * Where the output file is not null and the handler is not null then on completion of writing the unparsed output to
    * the file a message will be sent containing:
    * Message.arg1 = 3; Message.obj = String containing the full path of the output file
    * If an error occurs:
    * Message.arg1 = -1; Message.obj = error String
    * The output will also be written to to the specified database table if the database and table name parameters are
    * not null. UI programs using this interface can therefore refresh a cursor from this table on receiving notification
    * messages in the Handler.*
    * @param query The SPARQL SELECT query.
    * @param defaultGraphUris The default graph URI's or null or empty if none.
    * @param namedGraphUris The named graph URI's or null or empty if none.
    * @param method The HTTP method (GET or POST).
    * @param encoding The HTTP ACCEPT encoding
    * @param handler The Handler (@see android.os.Handler)
    * @param handlerWhat The identifier to place int the Message what field.
    * @param outputFile The file into which the unparsed output will be written.
    * @param database The SQLiteDatabase instance.
    * @param tableName The table name. The table will be dropped if it already exists and then recreated before
    *                  inserting the data.
    * @return A handle identifying the task.
    * @throws IOException
    * @throws InvalidQueryFormException if this is not a SELECT query.
    **/
   public long select(String query, String[] defaultGraphUris, String[] namedGraphUris,
                      HTTP_METHOD method, ACCEPT_ENCODING encoding,
                      Handler handler, int handlerWhat, File outputFile, SQLiteDatabase database, String tableName)
    throws IOException, InvalidQueryFormException
   //--------------------------------------------------------------------------------------------------------------
   {
      query = checkQuery(query, SELECT_PATTERN);
      StringBuilder postBuffer = new StringBuilder();
      byte[] postData = null;
      Uri uri = makeUri(query, defaultGraphUris, namedGraphUris, method, postBuffer);
      if (method == HTTP_METHOD.POST)
         postData = postBuffer.toString().getBytes();
      Log.i("SPARQLClient", uri.toString());
      long handle = HANDLE.getAndIncrement();
      final SelectThread selectThread;
      if (outputFile != null)
         selectThread = new SelectThread(handle, uri, method, encoding, postData, connectionTimeout, readTimeOut,
                                         handler, handlerWhat, outputFile);
      else
         selectThread = new SelectThread(handle, uri, method, encoding, postData, connectionTimeout, readTimeOut,
                                         handler, handlerWhat, database, tableName);
      Future<?> f = THREAD_POOL.submit(selectThread);
      synchronized (THREAD_MAP) { THREAD_MAP.put(handle, f); }
      return handle;
   }

   /**
    * Perform a ASK query. If a not null Handler is specified then the result of the ASK will be
    * communicated back to the caller via the Handler callback (@see android.os.Handler.Callback) as follows:
    * Message.arg1 = 0; Message.obj = Boolean object containing true if the ASK returned true or false if not.
    * If the  result could not be parsed to indicate a true of false then the error will be communicated by a
    * message containing:
    * Message.arg1 = 1; Message.obj = String containing the server response.
    * If an error occurs:
    * Message.arg1 = -1; Message.obj = error String
    * @param query The SPARQL ASK query.
    * @param defaultGraphUris The default graph URI's or null or empty if none.
    * @param namedGraphUris The named graph URI's or null or empty if none.
    * @param method The HTTP method (GET or POST).
    * @param encoding The HTTP ACCEPT encoding
    * @param handler The Handler (@see android.os.Handler)
    * @param handlerWhat The identifier to place int the Message what field.
    * @return A handle identifying the task.
    * @throws IOException
    * @throws InvalidQueryFormException if this is not an ASK query.
    */
   public long ask(String query, String[] defaultGraphUris, String[] namedGraphUris,
                   HTTP_METHOD method, ACCEPT_ENCODING encoding, Handler handler, int handlerWhat)
   throws IOException, InvalidQueryFormException
   //----------------------------------------------------------------------------------------------------
   {
      return ask(query, defaultGraphUris, namedGraphUris, method, encoding, handler, handlerWhat, null);
   }

   /**
    * Perform a ASK query. The results will not be parsed but instead the raw output will be written to the
    * specified file.
    * @param query The SPARQL ASK query.
    * @param defaultGraphUris The default graph URI's or null or empty if none.
    * @param namedGraphUris The named graph URI's or null or empty if none.
    * @param method The HTTP method (GET or POST).
    * @param encoding The HTTP ACCEPT encoding
    * @param outputFile The file into which the unparsed output will be written.
    * @return A handle identifying the task.
    * @throws IOException
    * @throws InvalidQueryFormException if this is not an ASK query.
    */
   public long ask(String query, String[] defaultGraphUris, String[] namedGraphUris,
                   HTTP_METHOD method, ACCEPT_ENCODING encoding, File outputFile)
         throws IOException, InvalidQueryFormException
   //----------------------------------------------------------------------------------------------------
   {
      return ask(query, defaultGraphUris, namedGraphUris, method, encoding, null, -1, outputFile);
   }

   /**
    * Perform a ASK query. If a not null Handler is specified then the result of the ASK will be
    * communicated back to the caller via the Handler callback (@see android.os.Handler.Callback) as follows:
    * Message.arg1 = 0; Message.obj = Boolean object containing true if the ASK returned true or false if not.
    * If the  result could not be parsed to indicate a true of false then the error will be communicated by a
    * message containing:
    * Message.arg1 = 1; Message.obj = String containing the server response.
    * Where the output file is not null and the handler is not null then on completion of writing the unparsed output to
    * the file a message will be sent containing:
    * Message.arg1 = 3; Message.obj = String containing the full path of the output file
    * If an error occurs:
    * Message.arg1 = -1; Message.obj = error String
    * @param query The SPARQL ASK query.
    * @param defaultGraphUris The default graph URI's or null or empty if none.
    * @param namedGraphUris The named graph URI's or null or empty if none.
    * @param method The HTTP method (GET or POST).
    * @param encoding The HTTP ACCEPT encoding
    * @param handler The Handler (@see android.os.Handler)
    * @param handlerWhat The identifier to place int the Message what field.
    * @param outputFile The file into which the unparsed output will be written.
    * @return A handle identifying the task.
    * @throws IOException
    * @throws InvalidQueryFormException if this is not an ASK query.
    */
   public long ask(String query, String[] defaultGraphUris, String[] namedGraphUris,
                      HTTP_METHOD method, ACCEPT_ENCODING encoding, Handler handler, int handlerWhat, File outputFile)
   throws IOException, InvalidQueryFormException
   //----------------------------------------------------------------------------------------------------
   {
      query = checkQuery(query, ASK_PATTERN);
      StringBuilder postBuffer = new StringBuilder();
      byte[] postData = null;
      Uri uri = makeUri(query, defaultGraphUris, namedGraphUris, method, postBuffer);
      if (method == HTTP_METHOD.POST)
         postData = postBuffer.toString().getBytes();
      Log.i("SPARQLClient", uri.toString());
      long handle = HANDLE.getAndIncrement();
      final AskThread askThread;
      if (outputFile != null)
         askThread = new AskThread(handle, uri, method, encoding, postData, connectionTimeout, readTimeOut,
                                   handler, handlerWhat, outputFile);
      else
         askThread = new AskThread(handle, uri, method, encoding, postData, connectionTimeout, readTimeOut,
                                   handler, handlerWhat);
      Future<?> f = THREAD_POOL.submit(askThread);
      synchronized (THREAD_MAP) { THREAD_MAP.put(handle, f); }
      return handle;
   }

   /**
    * Perform a CONSTRUCT query. Currently there are no parsers defined for Turtle or RDF+XML so the output is written
    * to an output file. If the handler is not null then on completion of writing the unparsed output to
    * the file a message will be sent containing:
    * Message.arg1 = 3; Message.obj = String containing the full path of the output file
    * If an error occurs:
    * Message.arg1 = -1; Message.obj = error String
    * @param query The SPARQL CONSTRUCT query.
    * @param defaultGraphUris The default graph URI's or null or empty if none.
    * @param namedGraphUris The named graph URI's or null or empty if none.
    * @param method The HTTP method (GET or POST).
    * @param encoding The HTTP ACCEPT encoding
    * @param handler The Handler (@see android.os.Handler)
    * @param handlerWhat The identifier to place int the Message what field.
    * @param outputFile The file into which the unparsed output will be written.
    * @return A handle identifying the task.
    * @throws IOException
    * @throws InvalidQueryFormException if this is not an CONSTRUCT query.
    */
   public long construct(String query, String[] defaultGraphUris, String[] namedGraphUris,
                   HTTP_METHOD method, ACCEPT_ENCODING encoding, Handler handler, int handlerWhat,
                   File outputFile)
         throws IOException, InvalidQueryFormException
   //----------------------------------------------------------------------------------------------------
   {
      query = checkQuery(query, CONSTRUCT_PATTERN);
      StringBuilder postBuffer = new StringBuilder();
      byte[] postData = null;
      Uri uri = makeUri(query, defaultGraphUris, namedGraphUris, method, postBuffer);
      if (method == HTTP_METHOD.POST)
         postData = postBuffer.toString().getBytes();
      Log.i("SPARQLClient", uri.toString());
      long handle = HANDLE.getAndIncrement();
      Future<?> f = THREAD_POOL.submit(new ConstructThread(handle, uri, method, encoding, postData, connectionTimeout,
                                                     readTimeOut,  handler, handlerWhat, outputFile));
      synchronized (THREAD_MAP) { THREAD_MAP.put(handle, f); }
      return handle;
   }

   /**
    * Cancel a SPARQL operation identified by a specified handle.
    * @param handle Handle of operation to terminate. The handle is returned from a call to select, ask or construct.
    * @return true if the operation was terminated else false;
    */
   public boolean cancel(long handle)
   //--------------------------------
   {
      Future<?> f = null;
      synchronized (THREAD_MAP)
      {
         f = THREAD_MAP.get(handle);
         if (f == null)
            return false;
         if (f.cancel(true))
            THREAD_MAP.remove(handle);
         else
            return false;
      }
      return true;
   }

   // Could also use service provider interface using META-INF/services files (implemented in Android since API 9)
   static protected Parseable getParser(ACCEPT_ENCODING encoding)
   //-------------------------------------------------------------
   {
      switch (encoding)
      {
         case CSV:
            return new CSVParse();
         case TSV:
            return new TSVParse();
         case XML:
            return new XMLParse();
      }
      return null;
   }

   static protected void appendGET(Uri.Builder uriBuilder, String k, String[] values)
   //------------------------------------------------------------------------------------
   {
      if ( (values == null) || (values.length == 0) )
         return;
      for (String v : values)
      {
         if ( (v == null) || (v.trim().isEmpty()) )
            continue;
         uriBuilder.appendQueryParameter(k, v);
      }
   }

   static protected void appendPOST(StringBuilder data, String k, String[] values) throws UnsupportedEncodingException
   //------------------------------------------------------------------------------------
   {
      if ( (values == null) || (values.length == 0) )
         return;
      if (data.length() > 0)
         data.append('&');
      for (String v : values)
      {
         if ( (v == null) || (v.trim().isEmpty()) )
            continue;
         data.append(k).append('=').append(URLEncoder.encode(v, "UTF-8")).append('&');
      }
      if ( (data.length() > 0) && (data.charAt(data.length() - 1) == '&') )
         data.deleteCharAt(data.length() - 1);
   }

   protected Uri makeUri(String query, String[] defaultGraphUris, String[] namedGraphUris, HTTP_METHOD method,
                         StringBuilder postData)
         throws UnsupportedEncodingException
   //--------------------------------------------------------------------------------
   {
      Uri uri = null;
      Uri.Builder uriBuilder = android.net.Uri.parse(endpoint.toString()).buildUpon();
      switch (method)
      {
         case GET:
            appendGET(uriBuilder, "default-graph-uri", defaultGraphUris);
            appendGET(uriBuilder, "named-graph-uri", namedGraphUris);
            uriBuilder.appendQueryParameter("query", query);
            uri = uriBuilder.build();
            break;

         case POST:
            uri = uriBuilder.build();
            postData.append("query=").append(URLEncoder.encode(query, "UTF-8"));
            appendPOST(postData, "default-graph-uri", defaultGraphUris);
            appendPOST(postData, "named-graph-uri", namedGraphUris);
//            Log.i(LOGTAG, "POST data " + sb.toString());
            break;
      }
      return uri;
   }

   protected String checkQuery(String query, Pattern pattern) throws IOException, InvalidQueryFormException
   //-------------------------------------------------------------------------------------------------------
   {
      if ( (query == null) || (query.trim().isEmpty()) )
         throw new IOException("Query must be not null and not empty");
      query = query.replace('\n', ' ');
      Matcher matcher = pattern.matcher(query.toUpperCase());
      if (! matcher.matches())
         throw new InvalidQueryFormException(pattern.toString(), query);
      return query;
   }
}
