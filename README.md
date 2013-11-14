SparkleDroid
============

A lightweight Android SPARQL client library for performing SPARQL queries against remote SPARQL servers using HTTP.

The raison d'être for this library is to provide a lightweight Android library for developing SPARQL client applications. 
It proved difficult to extract the client only code from Jena or Sesame and compiling the entire Jena or Sesame codebase
for Android results in somewhat heavy jars. The SparkleDroidLib library also provides a mobile device friendly async
interface using Android handlers for communication between the library and user applications with optional output to
SQLite database tables.

