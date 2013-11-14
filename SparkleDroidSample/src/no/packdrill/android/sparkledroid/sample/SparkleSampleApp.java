package no.packdrill.android.sparkledroid.sample;

import android.app.*;
import android.database.sqlite.*;
import android.util.*;

import java.io.*;

public class SparkleSampleApp extends Application
//===============================================
{
   static final String LOGTAG = SparkleSampleApp.class.getSimpleName();

   protected static final boolean USE_DATABASE = true;

   File databaseDir;

   SQLiteDatabase database = null;
   public SQLiteDatabase getDatabase() { return database; }

   @Override
   public void onCreate()
   //--------------------
   {
      super.onCreate();
      if (USE_DATABASE)
      {
         databaseDir = new File(getExternalFilesDir(null), "database");
         if (! databaseDir.exists())
            databaseDir.mkdirs();
         File f = new File(databaseDir, "database.sqlite");
         database = SQLiteDatabase.openOrCreateDatabase(f, null);
      }
   }

   @Override
   public void onTerminate()
   //-----------------------
   {
      super.onTerminate();
      if (USE_DATABASE)
      {
         if ( (database != null) && (database.isOpen()) )
            try { database.close(); } catch (Exception _e) { Log.e(LOGTAG, "", _e); }
         File f = new File(databaseDir, "database.sqlite");
         if (f.exists())
            f.delete();
      }
   }
}
