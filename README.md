# inferencereport
Simple command line tool that outputs subsumptions and equivalences seperated by imports that caused them

Example use 

```
 java -jar inferencereport-0.2-jar-with-dependencies.jar fbbt-edit.obo /data/fbbt-edit
```

This will generate four files

* subsumptions.txt: All direct subsumptions (inferred and asserted), including imports
* equivalences.txt: All equivalent named classes (inferred and asserted), including imports
* equivalences_imports.txt: All equivalences between classes used in the root ontology that only appear when a particular import is added. For example A sub B, A sub C might be implied by the root ontology, but if you add in import 1, you also obtain B sub C.
* subsumptions_imports.txt: All subsumptions between classes used in the root ontology that only appear when a particular import is added.
