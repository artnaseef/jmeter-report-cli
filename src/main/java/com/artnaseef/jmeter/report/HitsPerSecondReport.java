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
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Created by art on 4/7/15.
 */
public class HitsPerSecondReport {

  private SAXParseHandler handler = new SAXParseHandler();

  private long sampleCount;
  private XYSeriesCollection dataset;
  private JFreeChart chart;
  private XYSeries hitsSeries;
  private Map<Long, Long> hitsPerSecond;

  public static void main(String[] args) {
    HitsPerSecondReport mainObj = new HitsPerSecondReport();

    mainObj.instanceMain(args);
  }

  public void instanceMain(String[] args) {
    this.dataset = new XYSeriesCollection();

    if (args.length < 1) {
      System.err.println("Usage: HitsPerSecond <source-url>");
      System.exit(1);
    }

    String uri = args[0];
    this.hitsSeries = new XYSeries("Hits");
    this.hitsPerSecond = new TreeMap<Long, Long>();

    try {
      this.parse(uri);

      this.populateSeries();

      this.dataset.addSeries(this.hitsSeries);
      this.createChart();
      ExportUtils.writeAsPNG(this.chart, 1000, 750, new File("hitsPerSecond.png"));
    } catch (Exception exc) {
      exc.printStackTrace();
    }
  }

  protected void populateSeries () {
    for ( Map.Entry<Long, Long> hitCountSeconds : this.hitsPerSecond.entrySet() ) {
      System.out.println(Long.toString(hitCountSeconds.getKey()) + "|" +
                         hitCountSeconds.getValue());

      this.hitsSeries.add(hitCountSeconds.getKey(), hitCountSeconds.getValue());
    }
  }

  protected void createChart () {
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

//    // NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...
//    chart.setBackgroundPaint(Color.white);
//
////        final StandardLegend legend = (StandardLegend) chart.getLegend();
//    //      legend.setDisplaySeriesShapes(true);
//
//    // get a reference to the plot for further customisation...
//    final XYPlot plot = chart.getXYPlot();
//    plot.setBackgroundPaint(Color.lightGray);
//    //    plot.setAxisOffset(new Spacer(Spacer.ABSOLUTE, 5.0, 5.0, 5.0, 5.0));
//    plot.setDomainGridlinePaint(Color.white);
//    plot.setRangeGridlinePaint(Color.white);
//
//    final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
//    renderer.setSeriesLinesVisible(0, false);
//    renderer.setSeriesShapesVisible(1, false);
//    plot.setRenderer(renderer);
//
//    // change the auto tick unit selection to integer units only...
//    final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
//    rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
//    // OPTIONAL CUSTOMISATION COMPLETED.
  }

  protected void parse(String uri) throws ParserConfigurationException, SAXException, IOException {
    SAXParserFactory factory = SAXParserFactory.newInstance();
    SAXParser parser;

    parser = factory.newSAXParser();

    parser.parse(uri, this.handler);

    System.out.println("sample-count=" + this.sampleCount);
  }

  protected void addSample (long timestamp) {
    long newCount = 1;
    long timeStampSec = timestamp / 1000;
    Long existingCount = this.hitsPerSecond.get(timeStampSec);

    if ( existingCount != null ) {
      newCount += existingCount;
    }

    this.hitsPerSecond.put(timeStampSec, newCount);
    this.sampleCount++;
  }

  protected class SAXParseHandler extends DefaultHandler {
    private int level = 0;

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
        throws SAXException {

//      System.out.println("START: name=" + localName + ", qName=" + qName);

      if ( level >= 2 ) {
        if (qName.equals("sample") || qName.equals("httpSample")) {
          long timestamp = Long.valueOf(attributes.getValue("ts"));

          addSample(timestamp);
        }
      }

      this.level++;
//      this.printAttributes(attributes);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
      this.level--;
//      System.out.println("END: name=" + localName + ", qName=" + qName);
    }

    protected void printAttributes(Attributes attributes) {
      int iter = 0;
      while ( iter < attributes.getLength() ) {
        System.out.println("  > " + attributes.getQName(iter) + "=" + attributes.getValue(iter));
        iter++;
      }
    }
  }
}