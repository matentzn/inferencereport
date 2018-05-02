package ebi.spot.inferencereport;

import org.apache.commons.io.FileUtils;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.search.EntitySearcher;

import java.io.File;
import java.io.IOException;
import java.util.*;


public class InferenceAnalyser {
    private final long start = System.currentTimeMillis();
    private final Map<IRI, OWLOntology> allAxiomsAcrossOntologies = new HashMap<>();
    private String BASE = "http://ebi.debug.owl#";
    private Map<OWLEntity, String> labels = new HashMap<>();
    private OWLDataFactory df = OWLManager.getOWLDataFactory();

    private InferenceAnalyser() {
    }

    public static void main(String[] args) {
        File ontology = new File(args[0]);
        File out = new File(args[1]);

        InferenceAnalyser p = new InferenceAnalyser();
        p.process(ontology, out);

    }

    private void print(File out, String filename, List<String> lines) {
        try {
            FileUtils.writeLines(new File(out, filename), lines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void process(File ofile, File out) {
        Report report_subsumptions = new Report();
        Report report_equivalences = new Report();
        Report report_allsubsumptions = new Report();
        Report report_allequivalences = new Report();

        try {
            OWLOntology o = processOntology(ofile);
            Set<OWLClass> signature = o.getClassesInSignature(Imports.EXCLUDED);
            signature.remove(df.getOWLNothing());
            signature.remove(df.getOWLThing());
            OWLOntology all = OWLManager.createOWLOntologyManager().createOntology(o.getAxioms(Imports.INCLUDED));
            OWLOntology root = OWLManager.createOWLOntologyManager().createOntology(o.getAxioms(Imports.EXCLUDED));
            Set<Subsumption> allSubsumptions = getSubsumptions(all, signature);
            Set<Subsumption> rootSubsumptions = getSubsumptions(root, signature);
            Set<Equivalence> allEquivalences = getEquivalences(all, signature);
            Set<Equivalence> rootEquivalences = getEquivalences(root, signature);

            List<IRI> iris = new ArrayList<>(allAxiomsAcrossOntologies.keySet());
            Collections.sort(iris);

            for (IRI f : iris) {
                log("Processing import: "+f+printTime());
                Set<OWLAxiom> axioms = new HashSet<>(o.getAxioms(Imports.EXCLUDED));
                axioms.addAll(allAxiomsAcrossOntologies.get(f).getAxioms(Imports.INCLUDED));
                OWLOntology o_plus = OWLManager.createOWLOntologyManager().createOntology(axioms);
                List<String> report_s = exportSubsumptionDiffReport(o, signature, rootSubsumptions, o_plus);
                List<String> report_e = extractEquivalenceDiffReport(o, signature, rootEquivalences, o_plus);
                Collections.sort(report_e);
                Collections.sort(report_s);
                report_allequivalences.addLine("O: "+f);
                report_allequivalences.addLines(report_e);
                report_allequivalences.addEmptyLine();
                report_allsubsumptions.addLine("O: "+f);
                report_allsubsumptions.addLines(report_s);
                report_allsubsumptions.addEmptyLine();
            }

            List<String> report_e = getEquivalenceReport(o,allEquivalences);
            Collections.sort(report_e);
            List<String> report_s = getSubsumptionReport(o,allSubsumptions);
            Collections.sort(report_s);
            report_equivalences.addLines(report_e);
            report_equivalences.addEmptyLine();
            report_subsumptions.addLines(report_s);
            report_subsumptions.addEmptyLine();

            print(out,"subsumptions_imports.txt",report_allsubsumptions.getLines());
            print(out,"equivalences_imports.txt",report_allequivalences.getLines());
            print(out,"subsumptions.txt",report_subsumptions.getLines());
            print(out,"equivalences.txt",report_equivalences.getLines());

        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }
    }

    private List<String> extractEquivalenceDiffReport(OWLOntology o, Set<OWLClass> signature, Set<Equivalence> rootEquivalences, OWLOntology o_plus) {
        Set<Equivalence> impEquivs = getEquivalences(o_plus, signature);
        return getEquivalenceReport(o, rootEquivalences, impEquivs);
    }

    private List<String> getEquivalenceReport(OWLOntology o, Set<Equivalence> rootEquivalences, Set<Equivalence> impEquivs) {
        Set<Equivalence> diff_eq = new HashSet<>(impEquivs);
        diff_eq.removeAll(rootEquivalences);
        return getEquivalenceReport(o, diff_eq);
    }

    private List<String> getEquivalenceReport(OWLOntology o, Set<Equivalence> eq) {
        List<String> output = new ArrayList<>();
        for (Equivalence s : eq) {
            output.add(s.getSub() + " " + s.getSuper() + " | " + l(s.getSub(), o) + " ~ " + l(s.getSuper(), o));
        }
        return output;
    }

    private List<String> exportSubsumptionDiffReport(OWLOntology o, Set<OWLClass> signature, Set<Subsumption> rootSubsumptions, OWLOntology o_plus) {
        Set<Subsumption> impSubs = getSubsumptions(o_plus, signature);
        Set<Subsumption> diff = new HashSet<>(impSubs);
        diff.removeAll(rootSubsumptions);
        return getSubsumptionReport(o, diff);
    }

    private List<String> getSubsumptionReport(OWLOntology o, Set<Subsumption> diff) {
        List<String> output = new ArrayList<>();
        for (Subsumption s : diff) {
            output.add(s.getSub() + " " + s.getSuper() + " | " + l(s.getSub(), o) + " ~ " + l(s.getSuper(), o));
        }
        return output;
    }

    private String l(OWLClass c, OWLOntology o) {
        if (!labels.containsKey(c)) {
            labels.put(c, getLabel(c, o).orElse("unknown"));
        }
        return labels.get(c);
    }

    private void log(String s) {
        System.out.println(s);
    }

    private Set<Subsumption> getSubsumptions(OWLOntology o, Set<OWLClass> signature) {

        Set<Subsumption> subs = new HashSet<>();
        OWLReasoner r = createELReasoner(o);
        for (OWLClass c : signature) {
            for (OWLClass sub : r.getSubClasses(c, true).getFlattened()) {
                subs.add(new Subsumption(c, sub));
            }
        }
        return subs;
    }

    private Set<Equivalence> getEquivalences(OWLOntology o, Set<OWLClass> signature) {
        Set<Equivalence> subs = new HashSet<>();
        OWLReasoner r = createELReasoner(o);
        for (OWLClass c : signature) {
            for (OWLClass sub : r.getEquivalentClasses(c)) {
                subs.add(new Equivalence(c, sub));
            }
        }
        return subs;
    }


    private OWLOntology processOntology(File ofile) {
        try {
            OWLOntology o = loadOntology(ofile);
            for (OWLOntology imp : o.getDirectImports()) {
                IRI niri = IRI.create(BASE + UUID.randomUUID());
                if (!imp.getOntologyID().getOntologyIRI().isPresent()) {
                    imp.getOWLOntologyManager().setOntologyDocumentIRI(imp, niri);
                }
                IRI iri = imp.getOntologyID().getOntologyIRI().or(niri);
                allAxiomsAcrossOntologies.put(iri, imp);
            }
            return o;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private OWLOntology loadOntology(File ofile) {
        try {
            return OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(ofile);
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }
        return null;
    }

    private OWLReasoner createELReasoner(OWLOntology o) {
        return new ElkReasonerFactory().createReasoner(o);
    }

    private String printTime() {
        long current = System.currentTimeMillis();
        long duration = current - start;
        return " (" + (duration / 1000) + " sec)";
    }

    public Optional<String> getLabel(OWLEntity c, OWLOntology o) {
        for (OWLOntology i : o.getImportsClosure()) {
            for (OWLAnnotation a : EntitySearcher.getAnnotations(c, i, df.getRDFSLabel())) {
                OWLAnnotationValue value = a.getValue();
                if (value instanceof OWLLiteral) {
                    String val = ((OWLLiteral) value).getLiteral();
                    return Optional.of(val);
                }
            }
        }
        return Optional.empty();
    }

}
