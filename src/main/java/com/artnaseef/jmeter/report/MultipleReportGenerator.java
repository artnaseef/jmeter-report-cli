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

import com.artnaseef.jmeter.report.jtl.model.Sample;
import com.artnaseef.jmeter.report.registry.GlobalReportTypeRegistry;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * Simultaneously generate multiple reports from the same source of samples.
 *
 * Created by art on 4/10/15.
 */
public class MultipleReportGenerator implements FeedableReport {
    private List<FeedableReport> reports;

    public static final String PROPERTY_MULTI_REPORT_NAMES = "reports";

    public MultipleReportGenerator() {
        this.reports = new LinkedList<>();
    }

    @Override
    public void onFeedStart(String uri, Properties reportProperties) throws Exception {
        this.extractReportProperties(reportProperties);

        for ( FeedableReport oneReport : this.reports ) {
            oneReport.onFeedStart(uri, reportProperties);
        }
    }

    @Override
    public void onFeedComplete() throws Exception {
        for ( FeedableReport oneReport : this.reports ) {
            oneReport.onFeedComplete();
        }
    }

    @Override
    public void onSample(Sample topLevelSample) throws Exception {
        for ( FeedableReport oneReport : this.reports ) {
            oneReport.onSample(topLevelSample);
        }
    }

    protected void  extractReportProperties (Properties props) {
        String reportNameListString = props.getProperty(PROPERTY_MULTI_REPORT_NAMES);

        String[] reportNames = reportNameListString.split(",");

        for ( String oneReportName : reportNames ) {
            FeedableReport report = GlobalReportTypeRegistry.get().getReportType(oneReportName);

            if ( report == null ) {
                throw new RuntimeException("invalid report name \"" + oneReportName + "\"");
            }

            this.reports.add(report);
        }
    }
}
