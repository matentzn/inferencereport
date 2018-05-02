package ebi.spot.inferencereport;

import org.semanticweb.owlapi.model.OWLClass;

public class Equivalence extends Subsumption {
    public Equivalence(OWLClass super_c, OWLClass sub_c) {
        super(super_c, sub_c);
    }
}
