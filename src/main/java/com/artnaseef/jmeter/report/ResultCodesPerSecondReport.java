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
import com.artnaseef.jmeter.report.jtl.model.Sample;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.util.ExportUtils;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.io.File;
import java.io.PrintStream;
import java.util.*;

import joptsimple.OptionParser;

/**
 * Created by art on 4/7/15.
 */
public class ResultCodesPerSecondReport implements FeedableReport {

    private OptionParser optionParser;

    private String outputFile = "resultCodesPerSecond.png";
    private String detailOutputFile;

    private int reportWidth = 1000;
    private int reportHeight = 750;

    private XYSeriesCollection dataset;
    private JFreeChart chart;
    private List<XYSeries> chartSeries;
    private Map<Integer, Map<Long, Long>> samplesByReportCode;

    private double secPerSample;
    private String yAxisLabel = "Seconds";

    private long timeSlotSize = 1000; // In milliseconds

    private long startTimestampSlot = -1;
    private long endTimestampSlot = -1;

    private PrintStream detailFileWriter;

    private String feedUri;

    public static void main(String[] args) {
        ResultCodesPerSecondReport mainObj = new ResultCodesPerSecondReport();

        try {
            ReportLauncher launcher = new ReportLauncher();
            launcher.launchReport(mainObj, args);
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    @Override
    public void onSample(Sample topLevelSample) throws Exception {
        List<Sample> subSamples = topLevelSample.getSubSamples();
        if ((subSamples != null) && (!subSamples.isEmpty())) {
            for (Sample oneSub : subSamples) {
                this.onSample(oneSub);
            }
        } else {
            this.addConcreteSample(topLevelSample);
        }
    }

    @Override
    public void onFeedStart(String uri, Properties reportProperties) throws Exception {
        this.feedUri = uri;

        this.extractReportProperties(reportProperties);

        this.chartSeries = new LinkedList<>();
        this.samplesByReportCode = new TreeMap<>();
        this.dataset = new XYSeriesCollection();

        if (this.detailOutputFile != null) {
            this.detailFileWriter = new PrintStream(this.detailOutputFile);
        }
    }

    @Override
    public void onFeedComplete() throws Exception {
        this.calculateTimeAdjustments();

        this.populateSeries(this.feedUri);

        for (XYSeries oneSeries : this.chartSeries) {
            this.dataset.addSeries(oneSeries);
        }

        this.createChart();

        ExportUtils.writeAsPNG(this.chart, this.reportWidth, this.reportHeight,
                new File(this.outputFile));
    }


    protected void extractReportProperties (Properties prop) {
        this.detailOutputFile = prop.getProperty(ReportLauncher.PROPERTY_DETAIL_FILE_NAME);

        String out = prop.getProperty(ReportLauncher.PROPERTY_OUTPUT_FILENAME);
        if ( out != null ) {
            this.outputFile = out;
        }

        Integer size;
        size = (Integer) prop.get(ReportLauncher.PROPERTY_CHART_HEIGHT);
        if (size != null) {
            this.reportHeight = size;
        }
        size = (Integer) prop.get(ReportLauncher.PROPERTY_CHART_WIDTH);
        if (size != null) {
            this.reportWidth = size;
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
        for (Map.Entry<Integer, Map<Long, Long>> entry : this.samplesByReportCode.entrySet()) {
            XYSeries rcSeries = new XYSeries(Integer.toString(entry.getKey()));
            this.chartSeries.add(rcSeries);

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

    protected void addConcreteSample(Sample oneSample) {
        Map<Long, Long> slotSamples = this.samplesByReportCode.get(oneSample.getResultCode());

        if (slotSamples == null) {
            slotSamples = new TreeMap<>();
            this.samplesByReportCode.put(oneSample.getResultCode(), slotSamples);
        }

        long newCount = 1;
        long timeStampSlot = normalizeTimestamp(oneSample.getTimestamp());
        Long existingCount = slotSamples.get(timeStampSlot);

        if (existingCount != null) {
            newCount += existingCount;
        }

        slotSamples.put(timeStampSlot, newCount);

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
}