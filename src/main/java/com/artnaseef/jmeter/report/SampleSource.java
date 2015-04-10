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

/**
 * Source of samples that can be fed to a FeedableReport.
 *
 * Created by art on 4/10/15.
 */
public interface SampleSource {
    /**
     * Feed sample to the given FeedableReport.  The source only feeds the samples.
     *
     * @param feedable report to which to send the samples.
     * @throws Exception
     */
    void execute(Feedable feedable) throws Exception;
}
