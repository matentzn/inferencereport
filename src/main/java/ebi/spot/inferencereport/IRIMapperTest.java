package ebi.spot.inferencereport;

import org.apache.commons.io.FileUtils;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.util.AutoIRIMapper;

import java.io.File;
import java.io.IOException;
import java.util.*;


public class IRIMapperTest {

    public static void main(String[] args) {
        File ontology = new File(args[0]);
        File imports = new File(args[1]);

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        try {
            OWLOntologyIRIMapper autoIRIMapper = new AutoIRIMapper(imports, true);
            manager.addIRIMapper(autoIRIMapper);
            OWLOntology o = manager.loadOntology(IRI.create(ontology.toURI()));
            o.getDirectImports().forEach(i->System.out.println(i.getOntologyID().getOntologyIRI()));
            o.getClassesInSignature().forEach(System.out::println);
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }

    }

}
