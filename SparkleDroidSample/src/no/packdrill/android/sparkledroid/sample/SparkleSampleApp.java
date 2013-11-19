/*
   Copyright [2013] [Donald Munro]

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

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
   public File getDatabaseDir() { return databaseDir; }

   SQLiteDatabase database = null;
   public SQLiteDatabase getDatabase() { return database; }

   File storageDirectory = null;
   public File getStorageDirectory() { return storageDirectory; }

   @Override
   public void onCreate()
   //--------------------
   {
      super.onCreate();
      storageDirectory = new File(getExternalFilesDir(null), "queries");
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
