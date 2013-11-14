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

import android.database.*;
import android.net.*;
import android.os.*;

import java.io.*;

public class ConstructThread extends QueryThread implements Runnable
//==================================================================
{
   ConstructThread(long handle, Uri uri, SparQLQuery.HTTP_METHOD method,
                   SparQLQuery.ACCEPT_ENCODING encoding, byte[] postData, int connectionTimeout, int readTimeOut,
                   Handler handler, int handlerWhat, File outputFile)
   //---------------------------------------------------------------------------------------------------------
   {
      super(handle, uri, method, encoding, postData, connectionTimeout, readTimeOut, handler, handlerWhat, outputFile);
   }

   @Override public void run() { super.run(); }

   @Override protected void parse(BufferedReader response) throws IOException, SQLException {}
}
