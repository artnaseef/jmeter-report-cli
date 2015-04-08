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

import com.artnaseef.jmeter.report.LaunchableReport;
import com.artnaseef.jmeter.report.registry.GlobalReportTypeRegistry;
import com.artnaseef.jmeter.report.registry.ReportTypeRegistry;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

/**
 * Created by art on 4/7/15.
 */
public class ReportLauncher {
//  private OptionParser optionParser;

    private ReportTypeRegistry registry = GlobalReportTypeRegistry.get();

    private String reportType;

    public static void main(String[] args) {
        ReportLauncher mainObj = new ReportLauncher();

        mainObj.instanceMain(args);
    }

    public void instanceMain(String[] args) {
        try {
            List<?> nonOptionArgs = this.parseCommandLine(args);

            if (nonOptionArgs.size() < 1) {
                this.printUsage(System.err);
                System.exit(1);
            }

            this.reportType = nonOptionArgs.get(0).toString();

            LaunchableReport report = this.registry.getReportType(reportType);
            if (report == null) {
                this.printUsage(System.err);

                this.printKnownReportTypes(System.err);

                System.exit(1);
            }

            String[] reportArgs = new String[nonOptionArgs.size() - 1];
            int cur = 1;
            while (cur < nonOptionArgs.size()) {
                reportArgs[cur - 1] = nonOptionArgs.get(cur).toString();
                cur++;
            }

            report.launchReport(reportArgs);
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    protected List<?> parseCommandLine(String[] args) throws Exception {
        return Arrays.asList(args);
    }

    protected void printUsage(PrintStream out) {
        out.println("Usage: ReportLauncher [options] <report-type> ...");

        out.println();
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
