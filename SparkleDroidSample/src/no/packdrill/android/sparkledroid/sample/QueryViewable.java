package no.packdrill.android.sparkledroid.sample;

import java.io.*;
import java.util.*;

public interface QueryViewable
//============================
{
   public void error(String message, Throwable exception);

   public void refresh();

   public void selectInit(ArrayList<String> columns);

   public void selectResult(Map<String, String> row);

   public void askResult(Boolean B);

   public void constructResult(File f);
}
