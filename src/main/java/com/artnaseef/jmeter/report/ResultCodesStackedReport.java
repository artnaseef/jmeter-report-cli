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
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.util.ExportUtils;
import org.jfree.data.category.DefaultCategoryDataset;

import java.awt.*;
import java.io.File;
import java.io.PrintStream;
import java.util.*;
import java.util.List;

/**
 * Generate a Stacked Bar report of requests per second broken down by result code.
 *
 * Created by art on 4/7/15.
 */
public class ResultCodesStackedReport implements FeedableReport {

    private String outputFile = "resultCodesStacked.png";
    private String detailOutputFile;

    private int reportWidth = 1000;
    private int reportHeight = 750;

    private DefaultCategoryDataset dataset;
    private JFreeChart chart;
    private Map<Integer, Map<Long, Long>> samplesByReportCode;

    private double secPerSample;
    private String yAxisLabel = "Average Samples Per Second";

    private long timeSlotSize = 1000; // In milliseconds
    private int maxSlots = 25;

    private long startTimestampSlot = -1;
    private long endTimestampSlot = -1;

    private PrintStream detailFileWriter;

    private String feedUri;

    public static void main(String[] args) {
        ResultCodesStackedReport mainObj = new ResultCodesStackedReport();

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

        this.samplesByReportCode = new TreeMap<>();
        this.dataset = new DefaultCategoryDataset();

        if (this.detailOutputFile != null) {
            this.detailFileWriter = new PrintStream(this.detailOutputFile);
        }
    }

    @Override
    public void onFeedComplete() throws Exception {
        this.adjustSlots();

        this.calculateTimeCustomizations();

        this.populateSeries(this.feedUri);

        this.createChart();

        ExportUtils.writeAsPNG(this.chart, this.reportWidth, this.reportHeight,
                new File(this.outputFile));
    }

    /**
     * Extract configuration from the given report properties.
     *
     * @param prop
     */
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

        Long slotSize = (Long) prop.get(ReportLauncher.PROPERTY_TIME_SLOT_SIZE);
        if ( slotSize != null ) {
            this.timeSlotSize = slotSize;
        }

        Integer maxSlotsProperty = (Integer) prop.get(ReportLauncher.PROPERTY_MAX_SLOTS);
        if ( maxSlotsProperty != null ) {
            this.maxSlots = maxSlotsProperty;
        }
    }

    /**
     * Calculate adjustments to the report based on time settings.
     */
    protected void calculateTimeCustomizations() {
        this.secPerSample = (double) this.timeSlotSize / 1000.0;

        if (Math.abs(this.secPerSample - 1.0) < 0.1) {
            this.yAxisLabel = "Second";
        } else {
            this.yAxisLabel = String.format("%01.1f Second", secPerSample);
        }
    }

    /**
     * Populate the chart data feed from the aggregated sample data.
     *
     * @param sourceUri URI from which the sample data was collected for reporting purposes.
     */
    protected void populateSeries(String sourceUri) {
        // Initialize the dataset to force the order; the chart is drawn in order the data is added to the dataset.
        int cur = 0;
        while ( cur < ( this.endTimestampSlot - this.startTimestampSlot ) + 1 ) {
            this.dataset.addValue(0.0, Integer.valueOf(-1), Long.valueOf(cur));
            cur++;
        }

        // Iterate over all the result codes from the input.
        for (Map.Entry<Integer, Map<Long, Long>> entry : this.samplesByReportCode.entrySet()) {
            Integer resultCode = entry.getKey();

            // Iterate over every time slot sampled for this result code and add the total samples for this result
            //  code to the chart data feed.
            for (Map.Entry<Long, Long> hitCountSeconds : entry.getValue().entrySet()) {
                long timestamp = hitCountSeconds.getKey();
                long hits = hitCountSeconds.getValue();

                Long xPoint = this.calculateXAxisOffset(timestamp);     // Timestamp offset
                double yPoint = (double) hits / this.secPerSample;      // Average per second

                // Add the data point to the chart data feed.
                this.dataset.addValue(yPoint, Integer.valueOf(resultCode), xPoint);

                if (this.detailFileWriter != null) {
                    this.detailFileWriter.println(
                            String.format("%s|%d|%d|%d|%d|%f", sourceUri,
                                    resultCode,
                                    hitCountSeconds.getKey(), hitCountSeconds.getValue(),
                                    xPoint, yPoint));
                }
            }
        }
    }

    //
    // Generate the chart from the data feed.
    //
    protected void createChart() {
        // create the chart...
        this.chart = ChartFactory.createStackedBarChart(
                "Average Result Codes per Second",  // chart title
                this.yAxisLabel,                    // x axis label
                "Samples",                          // y axis label
                dataset,                            // data
                PlotOrientation.VERTICAL,
                true,                               // include legend
                true,                               // tooltips
                false                               // urls
        );

        //
        // Adjust colors for the chart.
        //
        CategoryPlot categoryPlot;
        categoryPlot = (CategoryPlot) this.chart.getPlot();
        categoryPlot.setBackgroundPaint(Color.WHITE);
        categoryPlot.setDomainGridlinePaint(Color.BLACK);
        categoryPlot.setRangeGridlinePaint(Color.BLACK);

        //
        // Customize the bar colors.
        //
        CategoryItemRenderer renderer = this.chart.getCategoryPlot().getRenderer();
        List rowKeys = this.dataset.getRowKeys();

        int cur = 0;
        Map<Integer, Integer> colorAdjustMap = new HashMap<>();
        while ( cur < rowKeys.size() ) {
            Integer resultCode = (Integer) rowKeys.get(cur);

            Color color;
            int group = resultCode / 100;
            switch ( group ) {
                case 2:
                    color = Color.GREEN;
                    break;

                case 3:
                    color = Color.BLUE;
                    break;

                case 4:
                    color = Color.ORANGE;
                    break;

                case 5:
                    color = Color.RED;
                    break;

                default:
                    color = Color.GRAY;
                    break;
            }

            color = this.adjustColor(colorAdjustMap, group, color);

            renderer.setSeriesPaint(cur, color);
            renderer.setSeriesOutlinePaint(cur, Color.BLACK);

            cur++;
        }
    }

    /**
     * Adjust one color for the chart given the map of color assignments already applied, the chart grouping
     * (i.e. category or row value), and the starting color for the group.
     *
     * @param adjustMap state of adjustments already made.
     * @param group the color grouping for which to assign a color.
     * @param startColor initial color to use in the group.
     * @return
     */
    protected Color adjustColor (Map<Integer, Integer> adjustMap, int group, Color startColor) {
        Color result = startColor;
        Integer count = adjustMap.get(group);

        if ( count == null ) {
            adjustMap.put(group, 1);
        } else {
            int cur = 0;
            while ( cur < count ) {
                result = result.darker();
                cur++;
            }

            adjustMap.put(group, count + 1);
        }

        return  result;
    }

    /**
     * Add a single, concrete sample to the accumulated data.  Samples are aggregated into totals by slot using the
     * configured slot size.
     *
     * @param oneSample
     */
    protected void addConcreteSample(Sample oneSample) {
        Map<Long, Long> slotSamples = this.samplesByReportCode.get(oneSample.getResultCode());

        if (slotSamples == null) {
            slotSamples = new TreeMap<>();
            this.samplesByReportCode.put(oneSample.getResultCode(), slotSamples);
        }

        long newCount = 1;
        long timeStampSlot = calculateTimestampSlot(oneSample.getTimestamp());
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

    /**
     * Adjust the slot size, if needed, to keep the number of slots at or below the maximum.
     */
    protected void adjustSlots () {
        long range = ( this.endTimestampSlot - this.startTimestampSlot ) + 1;

        if ( range > maxSlots) {
            long newSlotSize = ( range * this.timeSlotSize ) / maxSlots;

            this.resample(this.timeSlotSize, newSlotSize);
            this.timeSlotSize = newSlotSize;
        }
    }

    /**
     * Re-sample the averages from the original slot size given to the new slot size given.  Since the source data
     * has already been aggregated into slots, the results will not be as accurate as it would be to re-run the
     * report with the ideal slot size.  However, an effort is made to apply anti-aliasing so the resulting graph
     * should have a nearly identical overall shape to the original.
     *
     * @param origSlotSize
     * @param newSlotSize
     */
    protected void resample (long origSlotSize, long newSlotSize) {
        Map<Integer, Map<Long, Long>> updatedSamplesByReportCode = new TreeMap<>();
        long updatedStartTimeSlot = Integer.MAX_VALUE;
        long updatedEndTimeSlot = 0;

        double slotRatio = (double) origSlotSize / (double) newSlotSize;

        for ( Integer oneRc : this.samplesByReportCode.keySet() ) {
            Map<Long, Long> origSamplesOneRc = this.samplesByReportCode.get(oneRc);
            Map<Long, Long> newSamplesOneRc;

            newSamplesOneRc = new TreeMap<>();
            updatedSamplesByReportCode.put(oneRc, newSamplesOneRc);

            for ( Long origSlot : origSamplesOneRc.keySet() ) {
                // Calculate the left-side position for the re-sample, and the percentage that the old samples "cover"
                //  the left-side.  The right-side will get any remainder after populating the left side.  Remember
                //  that there is integer arithmetic here and automatic truncation of decimals.
                double newSlotTgtPt = origSlot * slotRatio;
                long newLeftSlot = (long) newSlotTgtPt;
                double leftPct = 1 - ( newSlotTgtPt - newLeftSlot );

                // Update the count on the left size, if any.
                long leftCount = (long) ( origSamplesOneRc.get(origSlot) * leftPct );
                if ( leftCount > 0 ) {
                    Long orig = newSamplesOneRc.get(newLeftSlot);
                    if ( orig == null ) {
                        newSamplesOneRc.put(newLeftSlot, leftCount);
                    } else {
                        newSamplesOneRc.put(newLeftSlot, leftCount + orig);
                    }

                    // Adjust the updated start and end slots
                    if ( newLeftSlot < updatedStartTimeSlot ) {
                        updatedStartTimeSlot = newLeftSlot;
                    }
                    if ( newLeftSlot > updatedEndTimeSlot ) {
                        updatedEndTimeSlot = newLeftSlot;
                    }
                }

                // Update the count on the right side, if anything remains.
                long rightCount = origSamplesOneRc.get(origSlot) - leftCount;
                if ( rightCount > 0 ) {
                    Long orig = newSamplesOneRc.get(newLeftSlot + 1);
                    if ( orig == null ) {
                        newSamplesOneRc.put(newLeftSlot + 1, leftCount);
                    } else {
                        newSamplesOneRc.put(newLeftSlot + 1, leftCount + orig);
                    }

                    // Adjust the updated start and end slots
                    if ( newLeftSlot < updatedStartTimeSlot ) {
                        updatedStartTimeSlot = newLeftSlot;
                    }
                    if ( newLeftSlot > updatedEndTimeSlot ) {
                        updatedEndTimeSlot = newLeftSlot;
                    }
                }
            }
        }

        // Replace the originals with the updates.
        this.samplesByReportCode = updatedSamplesByReportCode;
        this.startTimestampSlot = updatedStartTimeSlot;
        this.endTimestampSlot = updatedEndTimeSlot;
    }

    protected long calculateTimestampSlot(long timestamp) {
        return timestamp / this.timeSlotSize;
    }

    protected long calculateXAxisOffset(long timestampSlot) {
        long result = timestampSlot - this.startTimestampSlot;

        return result;
    }
}