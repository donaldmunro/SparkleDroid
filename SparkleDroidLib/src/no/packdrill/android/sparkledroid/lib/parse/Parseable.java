package no.packdrill.android.sparkledroid.lib.parse;

import java.io.*;
import java.util.*;

/**
 * An interface defining methods for parsing data returned from a SPARQL query.
 * An example usage:
 * <code>
 * Parseable parser = SparQLQuery.getParser(encoding);
 * if (parser == null) handle_error();
 * parser.parse(response);
 * String[] columns = parser.projectionNames();
 * for (Iterator<Map<String, Cell>> it = parser.iterator(); it.hasNext();) // process the data
 *</code>
 */
public interface Parseable extends Iterable<Map<String, Cell>>, Closeable
//============================================================
{
   /**
    * Initiates parsing. When parse returns the SPARQL SELECT header should have been read so
    * a call to <i>projectionNames</i> should return the columns the query will return. The next step
    * is to call iterator to iterate over the rest of the data.
    * @param input A Reader for the result data
    * @throws IOException
    */
   public void parse(Reader input) throws IOException;

   /**
    * @return The last error message that occurred during processing.
    */
   public String getLastError();

   /**
    * @return The last exception that occurred during processing.
    */
   public Throwable getLastException();

   /**
    * @return The names of the columns returned by the query.
    */
   public String[] projectionNames();

   /**
    * Creates an iterator over the data. The iterator returns a Map with the key being the column name (as
    * returned in <i>projectionNames</i> and the value being the columns value.
    * @return A Map with the key being the column name (as returned in <i>projectionNames</i> and the value
    * being the columns value.
    */
   @Override public Iterator<Map<String, Cell>> iterator();

   /**
    * Explicitly closes any resources opened by the parser.
    * @throws IOException
    */
   @Override public void close() throws IOException;
}
