package no.packdrill.android.sparkledroid.lib.parse;

import java.net.*;

public class Cell
//===============
{
   private boolean hasFragment = false;
   public boolean isHash() { return hasFragment; }

   String stringValue = "";
   public String getStringValue() { return stringValue; }
   public void setStringValue(String stringValue) {  this.stringValue = stringValue; }

   String shortString = "";
   public String getShortString() { return shortString; }
   public void setShortString(String shortString) { this.shortString = shortString; }

   String unparsedValue = null;
   public String getUnparsedValue() { return unparsedValue; }
   public void setUnparsedValue(String unparsedValue) {  this.unparsedValue = unparsedValue; }

   Object value = null;
   public Object getValue() { return value; }
   public void setValue(Object value) { this.value = value; }

   String language;
   public String getLanguage() { return language; }
   public void setLanguage(String language) { this.language = language; }

   URI nameSpace;
   public URI getNameSpace() { return nameSpace; }
   public void setNameSpace(URI nameSpace) { this.nameSpace = nameSpace; isURI = true; }

   String name;
   public String getName() { return name; }
   public void setName(String name) { this.name = name; isURI = true; }

   boolean isBlank = false;
   public boolean isBlank() { return isBlank; }

   boolean isURI = false;
   public boolean isURI() { return isURI; }

   public Cell(String value) { this(value, null); }

   public Cell(boolean isBlank, String name)
   //-----------------------------------------
   {
      this.isBlank = isBlank;
      this.name = name;
      shortString = stringValue = "[" + name + "]";
   }

   public Cell(String s, Object o) { shortString = stringValue = s; value = o; }

   public Cell(String value, String language) { shortString = stringValue = value; this.value = value; this.language = language; }

   public Cell(URI uri, String name, boolean hasFragment)
   //-----------------------------------------------------
   {
      this.nameSpace = uri;
      this.isURI = true;
      this.hasFragment = hasFragment;
      this.name = name;
      stringValue = uri.toString() + ((hasFragment) ? "#" : "/")  + name;
      shortString = name;
   }
}
