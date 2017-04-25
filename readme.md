# Dissertation programming library
This is the programing repository for URN:	urn:nbn:de:hbz:38-75331

Depends on the [disslibrary Project](https://github.com/krempelra/disslibrary)


## Parts

### Chapter 3.5 _Patterns im automatischen Retrieval_

This is the description of the workflows to Analyse the connections-system in the [Arachne-Database](http://arachne.uni-koeln.de/)
Its more or less useless without the Arachne-Database, which is not completely open to the point needed here. It should emphasize the processing in principle.

[Arachne reference Analysis](src/main/java/de/rkrempel/diss/arachneanalysis/AllTwoModeAnalysisWriterv2.java)

### Chapter 5.3 _LWMap: eine Perspektive auf Zusammenhang_

This is the JAVA core source code of the LWMap Service which should be available under http://lwmap.uni-koeln.de/ .

[Retrival](src/main/java/de/rkrempel/diss/harvesting/harvesterexecs/ImportableHarvesterV7.java)

[Layout](src/main/java/de/rkrempel/diss/layout/dbpediadata/DBPediacontextLayoutScriptWebView3.java)

## Install

source is in _src_

Include all jar files in _lib_

The rest should be running with the Maven Dependencies

clone [disslibrary](https://github.com/krempelra/disslibrary) and set as dependency.

## Comments

Comments are usually in english some times they are in german and a few lines later the english version follows, sometimes its missing :-( .
