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

import java.util.Properties;

/**
 * Report which can be fed pushed samples.
 *
 * Created by art on 4/9/15.
 */
public interface FeedableReport extends Feedable {
    /**
     * Initialize the report on the feed of a new source of samples.
     *
     * @param uri location of the source of samples.
     * @param reportProperties configuration details for the report.
     * @throws Exception
     */
    void onFeedStart(String uri, Properties reportProperties) throws Exception;

    /**
     * Finalize the report given the completion of samples from the current source of samples.
     *
     * @throws Exception
     */
    void onFeedComplete() throws Exception;
}
