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
package com.artnaseef.jmeter.report.cli;

import com.artnaseef.jmeter.report.FeedableReport;
import com.artnaseef.jmeter.report.LaunchableReport;
import com.artnaseef.jmeter.report.SampleSource;
import com.artnaseef.jmeter.report.jtl.impl.JTLFileSampleSource;
import com.artnaseef.jmeter.report.registry.GlobalReportTypeRegistry;
import com.artnaseef.jmeter.report.registry.ReportTypeRegistry;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;

/**
 * Created by art on 4/7/15.
 */
public class ReportLauncher {
    private ReportTypeRegistry registry = GlobalReportTypeRegistry.get();

    public static final String PROPERTY_DETAIL_FILE_NAME = "detailFileName";
    public static final String PROPERTY_CHART_WIDTH = "chartWidth";
    public static final String PROPERTY_CHART_HEIGHT = "chartHeight";
    public static final String PROPERTY_TIME_SLOT_SIZE = "timeSlotSize";
    public static final String PROPERTY_OUTPUT_FILENAME = "outputFilename";
    public static final String PROPERTY_MAX_SLOTS = "maxSlots";

    private OptionParser optionParser;

    private String reportType;
    private Properties reportProperties;

    private SampleSource sampleSource;

    public static void main(String[] args) {
        ReportLauncher mainObj = new ReportLauncher();

        mainObj.instanceMain(args);
    }

    public void instanceMain(String[] args) {
        try {
            this.reportProperties = new Properties();

            List<?> nonOptionArgs = this.parseCommandLine(args);

            if (nonOptionArgs.size() < 1) {
                this.printUsage(System.err);
                System.exit(1);
            }

            this.reportType = nonOptionArgs.get(0).toString();

            FeedableReport report = this.registry.getReportType(reportType);

            if (report == null) {
                this.printUsage(System.err);
                System.exit(1);
            }

            int cur = 1;
            while (cur < nonOptionArgs.size()) {
                String uri = nonOptionArgs.get(cur).toString();
                this.launchConfiguredReport(reportType, uri);

                cur++;
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    public void launchReport (String reportType, String[] args) throws Exception {
        FeedableReport report = this.registry.getReportType(reportType);

        if (report == null) {
            this.printUsage(System.err);
            System.exit(1);
        }

        this.launchReport(report, args);
    }

    public void launchReport (FeedableReport report, String[] args) throws Exception {
        List<?> nonOptionArgs = this.parseCommandLine(args);

        int cur = 1;
        while (cur < nonOptionArgs.size()) {
            String uri = nonOptionArgs.get(cur).toString();
            this.launchConfiguredReport(reportType, uri);

            cur++;
        }
    }

    protected void launchConfiguredReport (String reportType, String uri) throws Exception {
        FeedableReport report = this.registry.getReportType(reportType);

        if (report == null) {
            this.printUsage(System.err);
            System.exit(1);
        }

        this.sampleSource = new JTLFileSampleSource(uri);

        report.onFeedStart(uri, reportProperties);
        this.sampleSource.execute(report);
        report.onFeedComplete();
    }

    protected List<?> parseCommandLine(String[] args) throws Exception {
        this.optionParser = new OptionParser("hD:d:H:o:s:W:");

        this.optionParser.accepts("h", "display this usage");

        this.optionParser.accepts("D", "report property")
                .withRequiredArg().ofType(String.class)
                .describedAs("property=value");

        this.optionParser.accepts("d", "generate detailed sample output")
                .withRequiredArg().ofType(String.class)
                .describedAs("filename");

        this.optionParser.accepts("H", "height of the generated report")
                .withRequiredArg().ofType(Integer.class);

        this.optionParser.accepts("M", "maximum slots")
                .withRequiredArg().ofType(Integer.class);

        this.optionParser.accepts("o", "output report filename")
                .withRequiredArg().ofType(String.class)
                .describedAs("filename");

        this.optionParser.accepts("s", "slot size, in milliseconds")
                .withRequiredArg().ofType(Long.class);

        this.optionParser.accepts("W", "width of the generated report")
                .withRequiredArg().ofType(Integer.class);

        try {
            OptionSet options = optionParser.parse(args);

            if (options.has("h")) {
                this.printUsage(System.out);
                System.exit(0);
            }

            if (options.has("D")) {
                for ( Object oneValue : options.valuesOf("D") ) {
                    String valueString = oneValue.toString();
                    String[] split = valueString.split("=", 2);

                    if ( split.length == 2 ) {
                        this.reportProperties.put(split[0], split[1]);
                    } else {
                        this.reportProperties.put(valueString, "");
                    }
                }
            }

            if (options.has("d")) {
                this.reportProperties.put(PROPERTY_DETAIL_FILE_NAME, (String) options.valueOf("d"));
            }

            if (options.has("H")) {
                this.reportProperties.put(PROPERTY_CHART_HEIGHT, (Integer) options.valueOf("H"));
            }

            if ( options.has("M") ) {
                this.reportProperties.put(PROPERTY_MAX_SLOTS, (Integer) options.valueOf("M"));
            }

            if (options.has("o")) {
                this.reportProperties.put(PROPERTY_OUTPUT_FILENAME, (String) options.valueOf("o"));
            }

            if (options.has("s")) {
                this.reportProperties.put(PROPERTY_TIME_SLOT_SIZE, (Long) options.valueOf("s"));
            }

            if (options.has("W")) {
                this.reportProperties.put(PROPERTY_CHART_WIDTH, (Integer) options.valueOf("W"));
            }

            return options.nonOptionArguments();
        } catch (Exception exc) {
            this.printUsage(System.err);
            System.err.println();

            throw exc;
        }
    }

    protected void printUsage(PrintStream out) {
        out.println("Usage: HitsPerSecond [options] <source-url>");

        try {
            optionParser.printHelpOn(out);

            out.println();
            this.printKnownReportTypes(out);
        } catch (IOException e) {
            // Ignore this one - if help can't be printed, what's left to do?
        }
    }

    protected void printKnownReportTypes(PrintStream out) {
        // Print the known report types in sorted order (hence the TreeSet)
        out.println("Available Report Types:");
        for (String reportType : new TreeSet<>(this.registry.getReportTypes())) {
            out.println("  " + reportType);
        }

        // Print the aliases in sorted order (hence the TreeSet)
        out.println();
        out.println("Available Report Aliases:");
        for (String alias : new TreeSet<>(this.registry.getAliases())) {
            out.println("  " + alias + " for " + this.registry.lookupAlias(alias));
        }
    }
}
