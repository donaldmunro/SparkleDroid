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

public class InvalidQueryFormException extends Exception
//======================================================
{
   public InvalidQueryFormException()
   {
      super("Invalid SPARQL query form. Expected to find one of SELECT, CONSTRUCT, DESCRIBE, ASK or UPDATE in query");
   }

   public InvalidQueryFormException(String expected, String query)
   {
      super("Invalid SPARQL query form. Expected to find " + expected + " in " + query);
   }
}
