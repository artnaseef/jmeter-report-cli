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
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.util.ExportUtils;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by art on 4/7/15.
 */
public class HitsPerSecondReport implements LaunchableReport {

    private OptionParser optionParser;

    private String outputFile = "hitsPerSecond.png";
    private String detailOutputFile;

    private int reportWidth = 1000;
    private int reportHeight = 750;

    private long sampleCount;
    private XYSeriesCollection dataset;
    private JFreeChart chart;
    private XYSeries hitsSeries;
    private Map<Long, Long> hitsPerSecond;

    private long timeSlotSize = 1000; // In milliseconds

    private long startTimestampSlot = -1;
    private long endTimestampSlot = -1;

    private PrintStream detailFileWriter;

    private JTLFileParser jtlFileParser;

    public static void main(String[] args) {
        HitsPerSecondReport mainObj = new HitsPerSecondReport();

        try {
            mainObj.launchReport(args);
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    public HitsPerSecondReport() {
        this.jtlFileParser = new JTLFileParser();
        this.jtlFileParser.setListener(new MyJTLParseListener());
    }

    @Override
    public void launchReport(String[] args) throws Exception {
        List<?> nonOptionArgs = this.parseCommandLine(args);

        this.dataset = new XYSeriesCollection();

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
        this.hitsSeries = new XYSeries("Hits");
        this.hitsPerSecond = new TreeMap<Long, Long>();

        this.parseJtlFile(uri);

        this.populateSeries(uri);
        this.dataset.addSeries(this.hitsSeries);
        this.createChart();

        ExportUtils.writeAsPNG(this.chart, this.reportWidth, this.reportHeight,
                new File(this.outputFile));
    }

    protected List<?> parseCommandLine(String[] args) throws Exception {
        this.optionParser = new OptionParser("hd:H:o:s:W:");

        this.optionParser.accepts("h", "display this usage");

        this.optionParser.accepts("d", "generate detailed sample output")
                .withRequiredArg().ofType(String.class)
                .describedAs("filename");

        this.optionParser.accepts("H", "height of the generated report")
                .withRequiredArg().ofType(Integer.class);

        this.optionParser.accepts("o", "output report filename (default = " + this.outputFile + ")")
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

            if (options.has("d")) {
                this.detailOutputFile = (String) options.valueOf("d");
            }

            if (options.has("H")) {
                this.reportHeight = (Integer) options.valueOf("H");
            }

            if (options.has("o")) {
                this.outputFile = (String) options.valueOf("o");
            }

            if (options.has("s")) {
                this.timeSlotSize = (Long) options.valueOf("s");
            }

            if (options.has("W")) {
                this.reportWidth = (Integer) options.valueOf("W");
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
        } catch (IOException e) {
            // Ignore this one - if help can't be printed, what's left to do?
        }
    }

    protected void populateSeries(String sourceUri) {
        for (Map.Entry<Long, Long> hitCountSeconds : this.hitsPerSecond.entrySet()) {
            long xPoint = this.calculateXAxisOffset(hitCountSeconds.getKey());
            long yPoint = hitCountSeconds.getValue();

            this.hitsSeries.add(xPoint, yPoint);

            if (this.detailFileWriter != null) {
                this.detailFileWriter.println(sourceUri + "|" + hitCountSeconds.getKey() +
                        "|" + hitCountSeconds.getValue() +
                        "|" + xPoint +
                        "|" + yPoint);
            }
        }
    }

    protected void createChart() {
        // create the chart...
        this.chart = ChartFactory.createXYLineChart(
                "Hits per Second",        // chart title
                "Second",                 // x axis label
                "Hits",                   // y axis label
                dataset,                  // data
                PlotOrientation.VERTICAL,
                true,                     // include legend
                true,                     // tooltips
                false                     // urls
        );
    }

    protected void parseJtlFile(String uri) throws ParserConfigurationException, SAXException, IOException {
        this.jtlFileParser.parse(uri);

        System.out.println("sample-count=" + this.sampleCount);
    }

    protected void addSample(Sample sample) {
        long newCount = 1;
        long timeStampSlot = normalizeTimestamp(sample.getTimestamp());
        Long existingCount = this.hitsPerSecond.get(timeStampSlot);

        if (existingCount != null) {
            newCount += existingCount;
        }

        this.hitsPerSecond.put(timeStampSlot, newCount);
        this.sampleCount++;

        if ((this.startTimestampSlot == -1) || (timeStampSlot < this.startTimestampSlot)) {
            this.startTimestampSlot = timeStampSlot;
        }

        if ((this.endTimestampSlot == -1) || (timeStampSlot > this.endTimestampSlot)) {
            this.endTimestampSlot = timeStampSlot;
        }
    }

    protected long normalizeTimestamp(long timestamp) {
        return timestamp / this.timeSlotSize;
    }

    protected long calculateXAxisOffset(long timestampSlot) {
        long result = timestampSlot - this.startTimestampSlot;

        return result;
    }

    protected class MyJTLParseListener implements JTLFileParseListener {
        @Override
        public void onSample(Sample fullSample) {
            processSubSamples(fullSample);
        }

        protected long processSubSamples(Sample topLevelSample) {
            int result = 0;

            List<Sample> subSamples = topLevelSample.getSubSamples();
            if ((subSamples != null) && (!subSamples.isEmpty())) {
                for (Sample oneSub : subSamples) {
                    processSubSamples(oneSub);
                }
            } else {
                addSample(topLevelSample);
            }

            return result;
        }
    }
}
