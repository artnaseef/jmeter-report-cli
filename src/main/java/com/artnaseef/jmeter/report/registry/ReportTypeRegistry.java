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

import com.artnaseef.jmeter.report.LaunchableReport;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by art on 4/8/15.
 */
public class ReportTypeRegistry {
    private Map<String, LaunchableReport> registeredReportTypes;
    private Map<String, String> aliases;

    public ReportTypeRegistry() {
        this.registeredReportTypes = new HashMap<>();
        this.aliases = new HashMap<>();
    }

    public void registerReportType(String typeName, LaunchableReport report) {
        this.registeredReportTypes.put(typeName, report);
    }

    public void registerAlias(String aliasName, String typeName) {
        this.aliases.put(aliasName, typeName);
    }

    public LaunchableReport getReportType(String typeName) {
        LaunchableReport result = this.registeredReportTypes.get(typeName);

        if (result == null) {
            String unaliased = this.aliases.get(typeName);
            result = this.registeredReportTypes.get(unaliased);
        }

        return result;
    }

    public Set<String> getReportTypes() {
        return Collections.unmodifiableSet(this.registeredReportTypes.keySet());
    }

    public Set<String> getAliases() {
        return Collections.unmodifiableSet(this.aliases.keySet());
    }

    public String lookupAlias(String alias) {
        return this.aliases.get(alias);
    }
}
