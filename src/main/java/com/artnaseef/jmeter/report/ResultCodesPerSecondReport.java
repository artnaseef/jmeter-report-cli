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

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.util.ExportUtils;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

/**
 * Created by art on 4/7/15.
 */
public class ResultCodesPerSecondReport implements LaunchableReport {

    private SAXParseHandler handler = new SAXParseHandler();

    private OptionParser optionParser;

    private String outputFile = "resultCodesPerSecond.png";
    private String detailOutputFile;

    private int reportWidth = 1000;
    private int reportHeight = 750;

    private long sampleCount;
    private XYSeriesCollection dataset;
    private JFreeChart chart;
    private List<XYSeries> hitsSeries;
    private Map<Integer, Map<Long, Long>> resultCodesPerSecond;

    private double secPerSample;
    private String yAxisLabel = "Seconds";

    private long timeSlotSize = 1000; // In milliseconds

    private long startTimestampSlot = -1;
    private long endTimestampSlot = -1;

    private PrintStream detailFileWriter;

    public static void main(String[] args) {
        ResultCodesPerSecondReport mainObj = new ResultCodesPerSecondReport();

        mainObj.instanceMain(args);
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
        this.hitsSeries = new LinkedList<>();
        this.resultCodesPerSecond = new TreeMap<>();

        this.parseJtlFile(uri);

        this.calculateTimeAdjustments();

        this.populateSeries(uri);

        for (XYSeries oneSeries : this.hitsSeries) {
            this.dataset.addSeries(oneSeries);
        }

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
        out.println("Usage: ResultCodesPerSecond [options] <source-url>");

        try {
            optionParser.printHelpOn(out);
        } catch (IOException e) {
            // Ignore this one - if help can't be printed, what's left to do?
        }
    }

    protected void calculateTimeAdjustments() {
        this.secPerSample = (double) this.timeSlotSize / 1000.0;

        if (Math.abs(this.secPerSample - 1.0) < 0.1) {
            this.yAxisLabel = "Second";
        } else {
            this.yAxisLabel = String.format("%01.1f Second", secPerSample);
        }
    }

    protected void populateSeries(String sourceUri) {
        for (Map.Entry<Integer, Map<Long, Long>> entry : this.resultCodesPerSecond.entrySet()) {
            XYSeries rcSeries = new XYSeries(Integer.toString(entry.getKey()));
            this.hitsSeries.add(rcSeries);

            for (Map.Entry<Long, Long> hitCountSeconds : entry.getValue().entrySet()) {
                long xPoint = this.calculateXAxisOffset(hitCountSeconds.getKey());
                double yPoint = (double) hitCountSeconds.getValue() / this.secPerSample;

                rcSeries.add(xPoint, yPoint);

                if (this.detailFileWriter != null) {
                    this.detailFileWriter.println(
                            String.format("%s|%d|%d|%d|%f", sourceUri,
                                    hitCountSeconds.getKey(), hitCountSeconds.getValue(),
                                    xPoint, yPoint));
                }
            }
        }
    }

    protected void createChart() {
        // create the chart...
        this.chart = ChartFactory.createXYLineChart(
                "Result Codes per " + this.yAxisLabel,        // chart title
                this.yAxisLabel,                 // x axis label
                "Hits",                   // y axis label
                dataset,                  // data
                PlotOrientation.VERTICAL,
                true,                     // include legend
                true,                     // tooltips
                false                     // urls
        );
    }

    protected void parseJtlFile(String uri) throws ParserConfigurationException, SAXException, IOException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser;

        parser = factory.newSAXParser();

        parser.parse(uri, this.handler);

        System.out.println("sample-count=" + this.sampleCount);
    }

    protected void addSample(int resultCode, long timestamp) {
        Map<Long, Long> slotSamples = this.resultCodesPerSecond.get(resultCode);

        if (slotSamples == null) {
            slotSamples = new TreeMap<>();
            this.resultCodesPerSecond.put(resultCode, slotSamples);
        }

        long newCount = 1;
        long timeStampSlot = normalizeTimestamp(timestamp);
        Long existingCount = slotSamples.get(timeStampSlot);

        if (existingCount != null) {
            newCount += existingCount;
        }

        slotSamples.put(timeStampSlot, newCount);
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

    protected class SAXParseHandler extends DefaultHandler {
        private int level = 0;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {

            if (level >= 2) {
                if (qName.equals("sample") || qName.equals("httpSample")) {
                    long timestamp = Long.valueOf(attributes.getValue("ts"));
                    String resultCodeStr = attributes.getValue("rc");

                    if ((resultCodeStr != null) && (!resultCodeStr.isEmpty())) {
                        int resultCode = decodeResultCodeString(resultCodeStr);

                        addSample(resultCode, timestamp);
                    }
                }
            }

            this.level++;
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            this.level--;
        }

        protected int decodeResultCodeString(String rcString) {
            int result = -1;
            try {
                result = Integer.valueOf(rcString);
            } catch (NumberFormatException nfExc) {
                // Return the default
            }

            return result;
        }
    }
}