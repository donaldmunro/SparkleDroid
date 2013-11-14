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

package no.packdrill.android.sparkledroid.lib.parse.xml;

import android.util.*;
import no.packdrill.android.sparkledroid.lib.parse.*;
import org.xmlpull.v1.*;

import java.io.*;
import java.net.*;
import java.util.*;

public class XMLParse extends AbstractParse implements Parseable
//==============================================================
{
   static final String LOGTAG = XMLParse.class.getSimpleName();

   XmlPullParser xpp = null;
   int eventType;
   String currentTag = "";
   String[] columns = null;
   boolean isEOF = false;

   @Override
   public void parse(Reader input) throws IOException
   //------------------------------------------------
   {
      int lineNo = -1, colNo = -1;
      try
      {
         final XmlPullParserFactory xppFactory = XmlPullParserFactory.newInstance();
         xpp = xppFactory.newPullParser();
         xpp.setInput(input);
         eventType = xpp.getEventType();
         List<String> columnList = new ArrayList<String>();
         String text;
         boolean isReadingHeading = true;
         while ( (isReadingHeading) && (eventType != XmlPullParser.END_DOCUMENT) )
         {
            lineNo = xpp.getLineNumber();
            colNo = xpp.getColumnNumber();
            switch (eventType)
            {
               case XmlPullParser.START_TAG:
                  currentTag = xpp.getName();
                  if (currentTag.equals("variable"))
                  {
                     String colName = getAttribute(xpp, "name", true);
                     if (colName != null)
                        columnList.add(colName);
                  }
                  break;

               case XmlPullParser.END_TAG:
                  final String endTag = xpp.getName();
                  if (endTag.compareTo("head") == 0)
                  {
                     columns = columnList.toArray(new String[0]);
                     isReadingHeading = false;
                  }
                  break;
            }
            eventType = xpp.next();
         }
         isEOF = (eventType == XmlPullParser.END_DOCUMENT);
      }
      catch (XmlPullParserException e)
      {
         lastError = errorMessage(lineNo, colNo, e);
         lastException = e;
         Log.e(LOGTAG, lastError, e);
         e.printStackTrace();
         throw new IOException(e);
      }
   }

   @Override public void close() throws IOException { }

   public String[] projectionNames() { return columns; }

   @Override
   public Iterator<Map<String, Cell>> iterator()
   //----------------------------------------------
   {
      return new Iterator<Map<String, Cell>>()
      //========================================
      {
         Map<String, Cell> columnMap = new HashMap<String, Cell>();

         @Override public boolean hasNext() { return (! isEOF); }

         @Override
         public Map<String, Cell> next()
         //--------------------------------
         {
            if (isEOF)
               return null;
            String currentColumn = null;
            boolean isReadingRow = true;
            for (String k : columns)
               columnMap.put(k, null);
            URI uri;
            int p;
            int lineNo = -1, colNo = -1;
            try
            {
               while ( (isReadingRow) && (eventType != XmlPullParser.END_DOCUMENT) )
               {
                  lineNo = xpp.getLineNumber();
                  colNo = xpp.getColumnNumber();
                  switch (eventType)
                  {
                     case XmlPullParser.START_TAG:
                        currentTag = xpp.getName();
                        if (currentTag.equals("binding"))
                           currentColumn = getAttribute(xpp, "name", true);
                        break;

                     case XmlPullParser.TEXT:
                        if (xpp.isWhitespace()) break;
                        final String v = xpp.getText();
                        if (currentTag.equals("literal"))
                        {
                           Cell ri = new Cell(v);
                           String lang = getAttribute(xpp, "lang", false);
                           if (lang != null)
                              ri.setLanguage(lang);
                           String dataType = getAttribute(xpp, "datatype", true);
                           if (dataType != null)
                           {
                              try  { uri = new URI(dataType); } catch (Exception _e) { uri = null; }
                              if (uri != null)
                              {
                                 Object value;
                                 try
                                 {
                                    value = XMLParse.this.valueOf(uri.getFragment(), v);
                                 }
                                 catch (Exception _e)
                                 {
                                    value = null;
                                    Log.e(LOGTAG, "Converting " + v + " to " + dataType, _e);
                                 }
                                 ri.setUnparsedValue(v);
                                 ri.setValue(value);
                              }
                           }
                           columnMap.put(currentColumn, ri);
                        } else if (currentTag.equals("uri"))
                        {
                           try  { uri = new URI(v); } catch (Exception _e) { uri = null; }
                           if (uri != null)
                           {
                              Cell ri = processURI(v);
                              if (ri != null)
                                 columnMap.put(currentColumn, ri);
                           }
                        } else if (currentTag.equals("bnode"))
                        {
                           Cell ri = new Cell(true, v);
                           ri.setUnparsedValue(v);
                           columnMap.put(currentColumn, ri);
                        }
                        break;

                     case XmlPullParser.END_TAG:
                        final String endTag = xpp.getName();
                        if (endTag.equals("binding"))
                           currentColumn = null;
                        else if (endTag.equals("result"))
                           isReadingRow = false;
                        break;
                  }
                  eventType = xpp.next();
               }
               isEOF = (eventType == XmlPullParser.END_DOCUMENT);
            }
            catch (Exception e)
            {
               lastError = errorMessage(lineNo, colNo, e);
               lastException = e;
               Log.e(LOGTAG, lastError, e);
               e.printStackTrace();
               return null;
            }
//            boolean isEmpty = true;
//            for (String k : columns)
//            {
//               if (columnMap.get(k) != null)
//               {
//                  isEmpty = false;
//                  break;
//               }
//            }
//            if (isEmpty)
//               return null;
            return columnMap;
         }

         @Override public void remove() { throw new UnsupportedOperationException("no.packdrill.android.SPARQLClient.parse.xml.iterator.remove"); }
      };
   }

   private String getAttribute(final XmlPullParser xpp, final String attribute, boolean isExact)
   //--------------------------------------------------------------------------
   {
      final int c = xpp.getAttributeCount();
      for (int attr=0; attr<c; attr++)
      {
         String attrname = xpp.getAttributeName(attr);
         if (isExact)
            if (attrname.equals(attribute))
               return xpp.getAttributeValue(attr);
         else
            if (attrname.contains(attribute))
               return xpp.getAttributeValue(attr);
      }
      return null;
   }

   private String errorMessage(int lineNo, int colno, Exception e)
   //--------------------------------------------------------------
   {
      StringBuilder msg = new StringBuilder("XMLParse ERROR: (");
      msg.append(e.getMessage()).append(") Tag").append(currentTag);
      if (e instanceof XmlPullParserException)
      {
         XmlPullParserException ex = (XmlPullParserException) e;
         msg.append(" line: ").append(ex.getLineNumber()).append("column: ").append(ex.getColumnNumber());
      }
      else
         msg.append(" line: ").append(lineNo).append("column: ").append(colno);
      Log.e(LOGTAG, msg.toString(), e);
      return msg.toString();
   }
}
