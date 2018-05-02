package ebi.spot.inferencereport;

import java.util.ArrayList;
import java.util.List;

public class Report {

    List<String> lines = new ArrayList<>();

    public void addLine(String s) {
        lines.add(s);
    }

    public void addEmptyLine() {
        lines.add("");
    }

    public List<String> getLines() {
        return lines;
    }

    public void addLines(List<String> linesIN) {
        lines.addAll(linesIN);
    }
}
