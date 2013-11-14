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
