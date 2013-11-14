package no.packdrill.android.sparkledroid.lib;

import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import android.net.*;
import android.os.*;
import no.packdrill.android.sparkledroid.lib.parse.*;

import java.io.*;
import java.util.*;

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
