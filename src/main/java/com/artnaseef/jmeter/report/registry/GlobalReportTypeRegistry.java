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

package com.artnaseef.jmeter.report.registry;

import com.artnaseef.jmeter.report.HitsPerSecondReport;
import com.artnaseef.jmeter.report.ResultCodesPerSecondReport;
import com.artnaseef.jmeter.report.SamplesByLabelStatusReport;

/**
 * Created by art on 4/8/15.
 */
public abstract class GlobalReportTypeRegistry {
    private final static ReportTypeRegistry registry;

    static {
        registry = new ReportTypeRegistry();

        //
        // Register local package reports
        //
        registry.registerReportType("HitsPerSecond", new HitsPerSecondReport());
        registry.registerAlias("hps", "HitsPerSecond");
        registry.registerAlias("hits-per-second", "HitsPerSecond");

        registry.registerReportType("ResultCodesPerSecond", new ResultCodesPerSecondReport());
        registry.registerAlias("rcps", "ResultCodesPerSecond");
        registry.registerAlias("result-codes-per-second", "ResultCodesPerSecond");

        registry.registerReportType("SamplesByLabelStatus", new SamplesByLabelStatusReport());
        registry.registerAlias("sbls", "SamplesByLabelStatus");
        registry.registerAlias("samples-by-label-status", "SamplesByLabelStatus");
    }

    public static ReportTypeRegistry get() {
        return registry;
    }
}
