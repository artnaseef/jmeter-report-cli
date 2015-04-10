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

import com.artnaseef.jmeter.report.cli.ReportLauncher;
import com.artnaseef.jmeter.report.jtl.JTLFileParseListener;
import com.artnaseef.jmeter.report.jtl.JTLFileParser;
import com.artnaseef.jmeter.report.jtl.model.Sample;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Report of the result for samples, aggregated by label, including success and error counts.
 *
 * This is a text-based report.
 *
 * Created by art on 4/7/15.
 */
public class SamplesByLabelStatusReport implements FeedableReport {

    private String outputFile = "samplesByLabelStatusReport.txt";
    private Map<String, SampleStats> sampleStatsByLabel;

    private PrintStream detailFileWriter;

    private String feedUri;

    public static void main(String[] args) {
        SamplesByLabelStatusReport mainObj = new SamplesByLabelStatusReport();

        try {
            ReportLauncher launcher = new ReportLauncher();
            launcher.launchReport(mainObj, args);
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    @Override
    public void onFeedStart(String uri, Properties reportProperties) throws Exception {
        this.feedUri = uri;

        this.extractReportProperties(reportProperties);

        this.sampleStatsByLabel = new TreeMap<>();
    }

    @Override
    public void onFeedComplete() throws Exception {
        this.generateReport();
    }

    @Override
    public void onSample(Sample topLevelSample) throws Exception {
        this.addSample(topLevelSample);
    }

    protected void extractReportProperties (Properties prop) {
        String out = prop.getProperty(ReportLauncher.PROPERTY_OUTPUT_FILENAME);
        if ( out != null ) {
            this.outputFile = out;
        }
    }

    protected void generateReport () throws Exception {
        SampleStats totals = new SampleStats();
        try ( PrintWriter out = new PrintWriter(this.outputFile) ) {
            out.println(this.formatHeader());

            for (Map.Entry<String, SampleStats> statEntry : this.sampleStatsByLabel.entrySet()) {
                SampleStats stats = statEntry.getValue();

                totals.numSample += stats.numSample;
                totals.numErrorOrFailure += stats.numErrorOrFailure;
                totals.numError += stats.numError;
                totals.numFailure += stats.numFailure;

                out.println(formatStats(statEntry.getKey(), stats));
            }

            out.println(formatStats("TOTALS", totals));
        }
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

    protected void addSample(Sample oneSample) {
        boolean hasFailure = this.hasFailureSample(oneSample);

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

    protected class SampleStats {
        public long numSample;

        public long numErrorOrFailure;
        public long numError;   // Specifically, test-suite execution failures (i.e. test-suite failure)
        public long numFailure; // Specifically, validation failures (i.e. app-under-test failure)
    }
}
