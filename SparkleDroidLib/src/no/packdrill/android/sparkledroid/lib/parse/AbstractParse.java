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
package no.packdrill.android.sparkledroid.lib.parse;

import no.packdrill.android.sparkledroid.lib.parse.csvtsv.*;

import java.math.*;
import java.net.*;
import java.text.*;

/**
 * Convenience base class for Parseable implementations.
 */
public abstract class AbstractParse implements Parseable
//======================================================
{
   static final String LOGTAG = TSVParse.class.getSimpleName();

   protected String lastError = null;
   public String getLastError() { return lastError; }

   protected Throwable lastException = null;
   public Throwable getLastException() { return lastException; }

   /**
    * Convert RDF type to a Java Object type.
    * @param type The RDF type
    * @param v The RDF value as a String
    * @return A Java Object of the correct type.
    * @throws NumberFormatException
    * @throws ParseException
    */
   protected Object valueOf(String type, final String v) throws NumberFormatException, ParseException
   //-------------------------------------------------------------------------------------------------
   {
      if ( (type == null) || (type.trim().isEmpty()) )
         return null;
      type = type.toLowerCase().trim();
      if (type.contains("string"))
         return v;
      else if ( (type.startsWith("int")) || (type.contains("integer"))  )
         return Integer.valueOf(v.trim());
      else if (type.contains("long"))
         return Long.valueOf(v.trim());
      else if (type.contains("short"))
         return Short.valueOf(v.trim());
      else if (type.equals("byte"))
      {
         char c = (char) Integer.valueOf(v.trim()).intValue();
         return Character.valueOf(c);
      }
      else if (type.equals("unsignedbyte"))
         return Byte.valueOf(v.trim());
      else if (type.equals("double"))
         return Double.valueOf(v.trim());
      else if (type.equals("float"))
         return Float.valueOf(v.trim());
      else if (type.equals("decimal"))
         return new BigDecimal(v.trim());
      else if (type.equals("boolean"))
         return new Boolean(v.trim());
      else if (type.equals("datetime"))
      {
         SimpleDateFormat sdt = new SimpleDateFormat("yyyy'-'mm'-'dd'T'kk':'mm':'ss");
         return sdt.parse(v.trim());
      }
      else if (type.equals("time"))
      {
         SimpleDateFormat sdt = new SimpleDateFormat("kk':'mm':'ss");
         return sdt.parse(v.trim());
      }
      else if (type.equals("date"))
      {
         SimpleDateFormat sdt = new SimpleDateFormat("yyyy'-'mm'-'dd");
         return sdt.parse(v.trim());
      }
      else if (type.equals("gyearmonth"))
      {
         SimpleDateFormat sdt = new SimpleDateFormat("yyyy'-'mm");
         return sdt.parse(v.trim());
      }
      else if (type.equals("gyear"))
      {
         SimpleDateFormat sdt = new SimpleDateFormat("yyyy");
         return sdt.parse(v.trim());
      }
      else if (type.equals("gmonthday"))
      {
         SimpleDateFormat sdt = new SimpleDateFormat("'--'mm'-'dd");
         return sdt.parse(v.trim());
      }
      else if (type.equals("gMonth"))
      {
         SimpleDateFormat sdt = new SimpleDateFormat("'--'mm");
         return sdt.parse(v.trim());
      }
      else if (type.equals("gday"))
      {
         SimpleDateFormat sdt = new SimpleDateFormat("'---'dd");
         return sdt.parse(v.trim());
      }
      return null;
   }

   /**
    * Extract a RDF resource URI into a Cell class instance.
    * @param v The RDF URI as text.
    * @return A Cell instance for this resource.
    */
   protected Cell processURI(final String v)
   //-------------------------------------------
   {
      Cell ri = null;
      URI namespace;
      String name;
      boolean hasFragment = true;
      int p = v.indexOf('#');
      if (p < 0)
      {
         hasFragment = false;
         p = v.lastIndexOf('/');
         if (p < 0)
            p = v.lastIndexOf(':');
      }

      if (p >= 0)
      {
         try { namespace = new URI(v.substring(0, p)); } catch (URISyntaxException e) { namespace = null; }
         try { name = v.substring(p+1); } catch (Exception e) { name = null; }
         if ( (namespace != null) || (name != null) )
         {
            if (name == null)
            {
               name = "";
               hasFragment = false;
            }
            ri = new Cell(namespace, name, hasFragment);
            ri.setUnparsedValue(v);
         }
      }
      return ri;
   }

}
