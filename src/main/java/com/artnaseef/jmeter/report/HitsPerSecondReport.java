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
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Created by art on 4/7/15.
 */
public class HitsPerSecondReport implements FeedableReport {

    private String outputFile = "hitsPerSecond.png";
    private String detailOutputFile;

    private int reportWidth = 1000;
    private int reportHeight = 750;

    private XYSeriesCollection dataset;
    private JFreeChart chart;
    private XYSeries chartSeries;
    private Map<Long, Long> hitsPerSecond;

    private long timeSlotSize = 1000; // In milliseconds

    private long startTimestampSlot = -1;
    private long endTimestampSlot = -1;

    private PrintStream detailFileWriter;

    private String feedUri;

    public static void main(String[] args) {
        HitsPerSecondReport mainObj = new HitsPerSecondReport();

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

        this.chartSeries = new XYSeries("Hits");
        this.dataset = new XYSeriesCollection();
        this.hitsPerSecond = new TreeMap<Long, Long>();

        if (this.detailOutputFile != null) {
            this.detailFileWriter = new PrintStream(this.detailOutputFile);
        }
    }

    @Override
    public void onFeedComplete() throws Exception {
        this.finishReport();
    }

    @Override
    public void onSample(Sample topLevelSample) {
        this.addSample(topLevelSample);
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

    protected void finishReport() throws Exception {
        this.populateSeries(this.feedUri);
        this.dataset.addSeries(this.chartSeries);
        this.createChart();

        ExportUtils.writeAsPNG(this.chart, this.reportWidth, this.reportHeight, new File(this.outputFile));
    }

    protected void populateSeries(String sourceUri) {
        for (Map.Entry<Long, Long> hitCountSeconds : this.hitsPerSecond.entrySet()) {
            long xPoint = this.calculateXAxisOffset(hitCountSeconds.getKey());
            long yPoint = hitCountSeconds.getValue();

            this.chartSeries.add(xPoint, yPoint);

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

    protected void addSample(Sample sample) {
        List<Sample> subSamples = sample.getSubSamples();
        if ( ( subSamples != null ) && ( ! subSamples.isEmpty() ) ) {
            for ( Sample oneSubSample : subSamples ) {
                this.addSample(oneSubSample);
            }
        } else {
            this.addHit(sample);
        }
    }

    protected void addHit(Sample sample) {
        long newCount = 1;
        long timeStampSlot = normalizeTimestamp(sample.getTimestamp());
        Long existingCount = this.hitsPerSecond.get(timeStampSlot);

        if (existingCount != null) {
            newCount += existingCount;
        }

        this.hitsPerSecond.put(timeStampSlot, newCount);

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
