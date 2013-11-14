package no.packdrill.android.sparkledroid.sample;

import android.annotation.*;
import android.app.*;
import android.database.sqlite.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import no.packdrill.android.sparkledroid.lib.*;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class QueryFragment extends Fragment
//==========================================
{
   final static String LOGTAG = SparkleSampleActivity.class.getSimpleName();

   protected final static Map<URI, List<String>> SAMPLE_ENDPOINTS = new HashMap<URI, List<String>>();

   SparkleSampleActivity activity = null;

   AutoCompleteTextView autocompleteEndPoint;
   ArrayAdapter<String> autocompleteAdapter;
   EditText textQuery, textDefaultGraph, textNamedGraph;

   public QueryFragment() { }

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
   //-----------------------------------------------------------------------------------------------
   {
      View v = null;
      try
      {
         v = inflater.inflate(R.layout.query_fragment, container, false);
      }
      catch (final Throwable e)
      {
         Log.e(LOGTAG, "Expanding layout", e);
         return  null;
      }
      activity = (SparkleSampleActivity) getActivity();
      textQuery = (EditText) v.findViewById(R.id.editTextQuery);
      final Button buttonExecute = (Button) v.findViewById(R.id.buttonExecute);
      autocompleteEndPoint = (AutoCompleteTextView) v.findViewById(R.id.autoCompleteEndPoint);
      textDefaultGraph = (EditText) v.findViewById(R.id.textDefaultGraphUri);
      textNamedGraph = (EditText) v.findViewById(R.id.textNamedGraphUri);
      String[] endpoints = new String[SAMPLE_ENDPOINTS.keySet().size()];
      int i = 0;
      for (URI uri : SAMPLE_ENDPOINTS.keySet())
         endpoints[i++] = uri.toString();
      autocompleteAdapter = new ArrayAdapter<String>(activity, android.R.layout.simple_dropdown_item_1line, endpoints);
      autocompleteEndPoint.setAdapter(autocompleteAdapter);
      final Model model = Model.get();
      autocompleteEndPoint.setOnItemClickListener(new AdapterView.OnItemClickListener()
      //=====================================================================================
      {
         @Override
         public void onItemClick(AdapterView<?> parent, View view, int position, long id)
         //---------------------------------------------------------------------------------
         {
            endpointSelected(position);
         }
      });
      autocompleteEndPoint.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
      {
         @Override
         public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
         {
            endpointSelected(position);
         }

         @Override public void onNothingSelected(AdapterView<?> parent) {  }
      });
      autocompleteEndPoint.setOnFocusChangeListener(new View.OnFocusChangeListener()
      {
         @Override
         public void onFocusChange(View v, boolean hasFocus)
         {
            if (hasFocus)
               autocompleteEndPoint.showDropDown();
            else
               autocompleteEndPoint.dismissDropDown();
         }
      });
      final Spinner spinnerEncoding = (Spinner) v.findViewById(R.id.spinnerEncoding);
      spinnerEncoding.setSelection(0);
      final Spinner spinnerMethod = (Spinner) v.findViewById(R.id.spinnerMethod);
      spinnerEncoding.setSelection(0);
      buttonExecute.setOnClickListener(new View.OnClickListener()
      //=========================================================
      {
         @Override public void onClick(View v)
         //-----------------------------------
         {
            final String s = autocompleteEndPoint.getText().toString();
            URI endPoint = null;
            try { endPoint = new URI(s); } catch (Exception _e) { endPoint = null; }
            if (endPoint == null)
            {
               Toast.makeText(activity, "Invalid endpoint (not a URI).", Toast.LENGTH_LONG).show();
               return;
            }
            String enc = (String) spinnerEncoding.getSelectedItem();
            SparQLQuery.ACCEPT_ENCODING encoding;
            if ( (enc == null) || (enc.equals("XML")) )
               encoding = SparQLQuery.ACCEPT_ENCODING.XML;
            else if (enc.startsWith("TSV"))
               encoding = SparQLQuery.ACCEPT_ENCODING.TSV;
            else if (enc.startsWith("CSV"))
               encoding = SparQLQuery.ACCEPT_ENCODING.CSV;
            else if (enc.startsWith("TURTLE"))
               encoding = SparQLQuery.ACCEPT_ENCODING.Turtle;
            else if (enc.startsWith("RDFXML"))
               encoding = SparQLQuery.ACCEPT_ENCODING.RDFXML;
            else if (enc.startsWith("N-Triples"))
               encoding = SparQLQuery.ACCEPT_ENCODING.NTRIPLES;
            else
               encoding = SparQLQuery.ACCEPT_ENCODING.ANY;
            enc = (String) spinnerMethod.getSelectedItem();
            SparQLQuery.HTTP_METHOD method;
            if ( (enc == null) || (enc.equals("POST")) )
               method = SparQLQuery.HTTP_METHOD.POST;
            else
               method = SparQLQuery.HTTP_METHOD.GET;
            String query = textQuery.getText().toString();
            if ( (query == null) || (query.trim().isEmpty()) )
            {
               Toast.makeText(activity, "Specify a query first", Toast.LENGTH_LONG).show();
               return;
            }
            String[] defaultGraphUris = extractUris(textDefaultGraph.getText().toString(), "default graph URIs");
            if (defaultGraphUris == null)
               return;
            String[] namedGraphUris = extractUris(textNamedGraph.getText().toString(), "named graph URIs");
            if (namedGraphUris == null)
               return;

            boolean isQueryOk = false;
            StringBuilder errbuf = new StringBuilder();
            if (SparQLQuery.isConstruct(query))
            {
               File f = new File(activity.getExternalFilesDir(null), "construct.out");
               f.delete();
               isQueryOk = model.save(query, defaultGraphUris, namedGraphUris, endPoint, method, encoding, f, errbuf);
            }
            else
            if (SparkleSampleApp.USE_DATABASE)
            {
               SQLiteDatabase db = ((SparkleSampleApp) activity.getApplication()).getDatabase();
               String queryMD5;
               try { queryMD5 =  md5(query); } catch (Exception _e) { queryMD5 = null; }
               if (queryMD5 == null)
                  isQueryOk = model.query(query, defaultGraphUris, namedGraphUris, endPoint, method, encoding, errbuf);
               else
               {
                  String table = "SELECT_" + queryMD5;
                  if (activity.tableName != null)
                  {
                     StringBuilder sql = new StringBuilder("DROP TABLE IF EXISTS ").append(activity.getTableName());
                     db.execSQL(sql.toString());
                  }
                  activity.tableName = table;
                  isQueryOk = model.query(query, defaultGraphUris, namedGraphUris, endPoint, method, encoding,
                                          db, activity.tableName, errbuf);
               }
            }
            else
               isQueryOk = model.query(query, defaultGraphUris, namedGraphUris, endPoint, method, encoding, errbuf);
            if (isQueryOk)
            {
               if (! SAMPLE_ENDPOINTS.containsKey(endPoint))
                  autocompleteAdapter.add(s);
               activity.resultsTab();
            }
            else
               Toast.makeText(activity, errbuf.toString(), Toast.LENGTH_LONG).show();
         }

         private String[] extractUris(final String s, final String location)
         //-----------------------------------------------------------------
         {
            String[] uris = new String[0];
            if ( (s != null) && (! s.trim().isEmpty()) )
            {
               uris = s.split(",");
               for (String suri : uris)
               {
                  URI uri = null;
                  try { uri = new URI(suri); } catch (Exception _e) { uri = null; }
                  if (uri == null)
                  {
                     Toast.makeText(activity, String.format("%s: Invalid %s (not a URI).", suri, location),
                                    Toast.LENGTH_LONG).show();
                     return null;
                  }
               }
            }
            return uris;
         }
      });
      return v;
   }

   public static final String md5(final String s) throws NoSuchAlgorithmException
   //-----------------------------------------------------------------------------
   {
      MessageDigest digest = MessageDigest.getInstance("MD5");
      digest.update(s.getBytes());
      byte messageDigest[] = digest.digest();
      StringBuffer hexString = new StringBuffer();
      for (int i = 0; i < messageDigest.length; i++)
      {
         String h = Integer.toHexString(0xFF & messageDigest[i]);
         while (h.length() < 2)
            h = "0" + h;
         hexString.append(h);
      }
      return hexString.toString();
   }

   static
   {
      {
         String sparql =
               "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
                     "PREFIX swrc: <http://swrc.ontoware.org/ontology#>\n" +
                     "SELECT DISTINCT $name $org $person $affiliation\n" +
                     "WHERE {\n" +
                     "    $person a foaf:Person .\n" +
                     "    $person swrc:affiliation $affiliation .\n" +
                     "    $person foaf:name $name .\n" +
                     "    $affiliation foaf:name $org .\n" +
                     "}" +
                     " ORDER BY $name LIMIT 5";
         String suri = "http://data.semanticweb.org/sparql";
         URI uri;
         try
         {
            uri = new URI(suri);
            List<String> L = new ArrayList<String>();
            L.add(sparql); L.add(null); L.add(null); L.add("People at semanticweb.org");
            SAMPLE_ENDPOINTS.put(uri, L);
         }
         catch (URISyntaxException e)
         {
            Log.e(LOGTAG, suri, e);
         }

         sparql =
               "PREFIX dbo: <http://dbpedia.org/ontology/>\n" +
                     "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
                     "SELECT ?man ?name ?car ?manufacturer\n" +
                     "WHERE {\n" +
                     "?car <http://purl.org/dc/terms/subject> <http://dbpedia.org/resource/Category:Luxury_vehicles> .\n" +
                     "?car foaf:name ?name .\n" +
                     "?car dbo:manufacturer ?man .\n" +
                     "?man foaf:name ?manufacturer\n" +
                     "}\nORDER by ?man ?name LIMIT 20\n";
         suri = "http://dbpedia.org/sparql";
         try
         {
            uri = new URI(suri);
            List<String> L = new ArrayList<String>();
            L.add(sparql); L.add(null); L.add(null); L.add("dbpedia Luxury cars");
            SAMPLE_ENDPOINTS.put(uri, L);
         }
         catch (URISyntaxException e)
         {
            Log.e(LOGTAG, suri, e);
         }

         sparql =
               "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                     "PREFIX ogc: <http://www.opengis.net/ont/geosparql#>\n" +
                     "PREFIX geom: <http://geovocab.org/geometry#>\n" +
                     "PREFIX lgdo: <http://linkedgeodata.org/ontology/>\n" +
                     "SELECT * FROM <http://linkedgeodata.org>\n" +
                     "{  ?s\n" +
                     "   a lgdo:Amenity ;\n" +
                     "   rdfs:label ?l ;\n" +
                     "   geom:geometry [ ogc:asWKT ?g ] .\n" +
                     "   Filter(bif:st_intersects (?g, bif:st_point (12.372966, 51.310228), 0.1)) .\n" +
                     "}";
//         suri = "http://linkedgeodata.org/sparql?default-graph-uri=http://linkedgeodata.org";
         suri = "http://linkedgeodata.org/sparql";
         try
         {
            uri = new URI(suri);
            List<String> L = new ArrayList<String>();
            L.add(sparql); L.add("http://linkedgeodata.org"); L.add(null); L.add("All amenities 100m from Connewitz Kreuz");
            SAMPLE_ENDPOINTS.put(uri, L);
         }
         catch (URISyntaxException e)
         {
            Log.e(LOGTAG, suri, e);
         }
      };
   }

   private void endpointSelected(int position)
   //-----------------------------------------
   {
      String s = autocompleteAdapter.getItem(position);
      try
      {
         activity.endPoint = new URI(s);
      }
      catch (Exception _e)
      {
         activity.endPoint = null;
      }
      if (activity.endPoint != null)
      {
         List<String> L = SAMPLE_ENDPOINTS.get(activity.endPoint);
         if (L != null)
         {
            String sparql = L.get(0);
            String ac = (L.get(1) == null) ? null : L.get(1);
            textDefaultGraph.setText(ac);
            ac = (L.get(2) == null) ? null : L.get(2);
            textNamedGraph.setText(ac);
            String description = L.get(3);
//            if (description != null)
//               sparql = "# " + description + "\n" + sparql;
            activity.setTitle(description);
            textQuery.setText(sparql);
         }
         else
            textQuery.setText("ERROR: " + s + " not found");
      } else
         textQuery.setText("ERROR: " + s + " invalid URI");
   }

}