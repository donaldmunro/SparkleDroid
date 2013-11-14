package no.packdrill.android.sparkledroid.lib.parse.csvtsv;

import android.util.*;
import no.packdrill.android.sparkledroid.lib.parse.*;
import org.apache.commons.csv.*;

import java.io.*;
import java.net.*;
import java.util.*;

public class CSVParse extends AbstractParse implements Parseable
//==============================================================
{
   static final String LOGTAG = CSVParse.class.getSimpleName();
   CSVParser parser = null;
   private Iterator<CSVRecord> it = null;
   Map<String, Integer> headerMap;
   String[] columns = null;

   @Override
   public void parse(Reader input) throws IOException
   //------------------------------------------------
   {
      CSVFormat format = CSVFormat.DEFAULT.withHeader();
      parser = new CSVParser(input, format);
      headerMap = parser.getHeaderMap();
      Set<Map.Entry<String, Integer>> ss = headerMap.entrySet();
      columns = new String[headerMap.size()];
      for (Map.Entry<String, Integer> e : ss)
         columns[e.getValue()] = e.getKey();
      it = parser.iterator();
   }

   @Override
   public void close() throws IOException
   //-------------------------------------
   {
      if (parser != null)
         parser.close();
   }

   public String[] projectionNames() { return columns; }

   @Override
   public Iterator<Map<String, Cell>> iterator()
   //--------------------------------
   {
      return new Iterator<Map<String, Cell>>()
      //========================================
      {
         Map<String, Cell> m = new HashMap<String, Cell>();

         @Override public boolean hasNext() { return it.hasNext();  }

         @Override
         public Map<String, Cell> next()
         //--------------------------------
         {
            CSVRecord record = null;
            try
            {
               record = it.next();
               m.clear();
               for (int i=0; i<record.size(); i++)
               {
                  String k = columns[i];
                  final String v = record.get(i);
                  if (v == null)
                     continue;
                  if (v.trim().startsWith("_:"))
                  {
                     int p = v.indexOf("_:");
                     String name;
                     try { name = v.substring(p+2); } catch (Exception _e) { name = ""; }
                     Cell ri = new Cell(true, name);
                     ri.setUnparsedValue(v);
                     m.put(k, ri);
                     continue;
                  }
                  if (v.trim().toLowerCase().startsWith("missing "))
                  {
                     Object o = null;
                     Cell ri = new Cell("null", o);
                     ri.setUnparsedValue(v);
                     m.put(k, ri);
                     continue;
                  }
                  URI uri = null;
                  try { uri = new URI(v); } catch (Exception _e) { uri = null; }
                  if (uri != null)
                  {
                     Cell ri = processURI(v);
                     if (ri != null)
                     {
                        m.put(k, ri);
                        continue;
                     }
                  }
                  Cell ri = new Cell(v);
                  ri.setUnparsedValue(v);
                  m.put(k, ri);
               }
            }
            catch (NoSuchElementException e)
            {
               lastError = (record == null) ? "" : record.toString();
               lastException = e;
               Log.e(LOGTAG, lastError, e);
               return null;
            }
            return m;
         }

         @Override public void remove() { throw new UnsupportedOperationException("no.packdrill.android.SPARQLClient.parse.csvtsv.iterator.remove"); }
      };
   }
}
