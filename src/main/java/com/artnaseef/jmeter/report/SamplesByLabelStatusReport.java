/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.artnaseef.jmeter.report;

import com.artnaseef.jmeter.report.jtl.JTLFileParseListener;
import com.artnaseef.jmeter.report.jtl.JTLFileParser;
import com.artnaseef.jmeter.report.jtl.model.Sample;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Report of the result for samples, aggregated by label, including success and error counts.
 *
 * This is a text-based report.
 *
 * Created by art on 4/7/15.
 */
public class SamplesByLabelStatusReport implements LaunchableReport {

    private OptionParser optionParser;

    private String outputFile = "samplesByLabelStatusReport.png";
    private String detailOutputFile;

    private int reportWidth = 1000;
    private int reportHeight = 750;

    private long sampleCount;
    private Map<String, SampleStats> sampleStatsByLabel;

    private PrintStream detailFileWriter;

    private JTLFileParser jtlFileParser;

    public static void main(String[] args) {
        SamplesByLabelStatusReport mainObj = new SamplesByLabelStatusReport();

        mainObj.instanceMain(args);
    }

    public SamplesByLabelStatusReport() {
        this.jtlFileParser = new JTLFileParser();
        this.jtlFileParser.setListener(new MyJTLParseListener());
    }

    public void instanceMain(String[] args) {
        try {
            this.launchReport(args);
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    @Override
    public void launchReport(String[] args) throws Exception {
        List<?> nonOptionArgs = this.parseCommandLine(args);

        if (nonOptionArgs.size() < 1) {
            this.printUsage(System.err);
            System.exit(1);
        }
        if (this.detailOutputFile != null) {
            this.detailFileWriter = new PrintStream(this.detailOutputFile);
        }

        for (Object oneSource : nonOptionArgs) {
            this.generateReportForSource(oneSource.toString());
        }
    }

    protected void generateReportForSource(String uri) throws Exception {
        this.sampleStatsByLabel = new TreeMap<>();

        this.parseJtlFile(uri);

        this.generateReport();
    }

    protected List<?> parseCommandLine(String[] args) throws Exception {
        this.optionParser = new OptionParser("hd:H:o:s:W:");

        this.optionParser.accepts("h", "display this usage");

        this.optionParser.accepts("d", "generate detailed sample output")
                .withRequiredArg().ofType(String.class)
                .describedAs("filename");

        this.optionParser.accepts("o", "output report filename (default = " + this.outputFile + ")")
                .withRequiredArg().ofType(String.class)
                .describedAs("filename");

        try {
            OptionSet options = optionParser.parse(args);

            if (options.has("h")) {
                this.printUsage(System.out);
                System.exit(0);
            }

            if (options.has("d")) {
                this.detailOutputFile = (String) options.valueOf("d");
            }

            if (options.has("o")) {
                this.outputFile = (String) options.valueOf("o");
            }

            return options.nonOptionArguments();
        } catch (Exception exc) {
            this.printUsage(System.err);
            System.err.println();

            throw exc;
        }
    }

    protected void printUsage(PrintStream out) {
        out.println("Usage: ResultCodesPerSecond [options] <source-url>");

        try {
            optionParser.printHelpOn(out);
        } catch (IOException e) {
            // Ignore this one - if help can't be printed, what's left to do?
        }
    }

    protected void parseJtlFile(String uri) throws ParserConfigurationException, SAXException, IOException {
        this.jtlFileParser.parse(uri);

        System.out.println("sample-count=" + this.sampleCount);
    }

    protected void generateReport () {
        SampleStats totals = new SampleStats();

        System.out.println(this.formatHeader());

        for ( Map.Entry<String, SampleStats> statEntry : this.sampleStatsByLabel.entrySet() ) {
            SampleStats stats = statEntry.getValue();

            totals.numSample += stats.numSample;
            totals.numErrorOrFailure += stats.numErrorOrFailure;
            totals.numError += stats.numError;
            totals.numFailure += stats.numFailure;

            System.out.println(formatStats(statEntry.getKey(), stats));
        }

        System.out.println(formatStats("TOTALS", totals));
    }

    protected String formatHeader () {
        return  String.format("%-40s %10s %10s %10s %10s %10s %s", "LABEL", "SAMPLES", "SUCCESS", "FAIL",
                "TESTFAIL", "APPFAIL", "FAIL %");
    }

    protected String formatStats (String label, SampleStats stats) {
        long successCount = stats.numSample - stats.numErrorOrFailure;
        double failPercentage;

        if ( stats.numSample != 0 ) {
            failPercentage = ( ( (double) stats.numErrorOrFailure) * 100 ) / ( (double) stats.numSample );
        } else {
            failPercentage = 0.0;
        }

        String result;
        result = String.format("%-40s %10d %10d %10d %10d %10d %1.2f%%", label, stats.numSample, successCount,
                stats.numErrorOrFailure, stats.numError, stats.numFailure, failPercentage);

        return  result;
    }

    protected void addSample(Sample oneSample, boolean hasFailure) {
        SampleStats sampleStats = this.sampleStatsByLabel.get(oneSample.getLabel());
        if ( sampleStats == null ) {
            sampleStats = new SampleStats();
            this.sampleStatsByLabel.put(oneSample.getLabel(), sampleStats);
        }

        sampleStats.numSample++;

        if ( oneSample.isExecError() || oneSample.isFailure() || ( hasFailure ) ) {
            sampleStats.numErrorOrFailure++;

            if ( oneSample.isExecError() ) {
                sampleStats.numError++;
            }

            if ( oneSample.isFailure() ) {
                sampleStats.numFailure++;
            }
        }
    }

    protected class MyJTLParseListener implements JTLFileParseListener {
        @Override
        public void onSample(Sample fullSample) {
            boolean hasFailure = this.hasFailureSample(fullSample);

            addSample(fullSample, hasFailure);
        }

        protected boolean hasFailureSample (Sample topLevelSample) {
            int result = 0;

            int rc = topLevelSample.getResultCode();
            if ( ( ( rc / 100 ) != 2 ) && ( ( rc / 100 ) != 3 ) ) {
                return  true;
            }

            List<Sample> subSamples = topLevelSample.getSubSamples();
            if ((subSamples != null) && (!subSamples.isEmpty())) {
                for (Sample oneSub : subSamples) {
                    if ( hasFailureSample(oneSub) ) {
                        return  true;
                    }
                }
            }

            return false;
        }
    }

    protected class SampleStats {
        public long numSample;

        public long numErrorOrFailure;
        public long numError;   // Specifically, test-suite execution failures (i.e. test-suite failure)
        public long numFailure; // Specifically, validation failures (i.e. app-under-test failure)
    }
}
