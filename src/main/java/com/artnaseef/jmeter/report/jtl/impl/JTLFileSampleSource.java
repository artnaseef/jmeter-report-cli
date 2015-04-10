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

package com.artnaseef.jmeter.report.jtl.impl;

import com.artnaseef.jmeter.report.Feedable;
import com.artnaseef.jmeter.report.SampleSource;
import com.artnaseef.jmeter.report.jtl.JTLFileParseListener;
import com.artnaseef.jmeter.report.jtl.JTLFileParser;
import com.artnaseef.jmeter.report.jtl.model.Sample;

/**
 * Source of samples extracted from a JTL file.
 *
 * Created by art on 4/10/15.
 */
public class JTLFileSampleSource implements SampleSource {
    private final String uri;

    public JTLFileSampleSource(String uri) {
        this.uri = uri;
    }

    @Override
    public void execute(Feedable feedable) throws Exception {
        JTLFileParser parser = new JTLFileParser();

        MyJTLParseListener listener = new MyJTLParseListener(feedable);
        parser.setListener(listener);

        parser.parse(uri);
    }

    protected class MyJTLParseListener implements JTLFileParseListener {
        private Feedable target;

        public MyJTLParseListener(Feedable target) {
            this.target = target;
        }

        @Override
        public void onSample(Sample fullSample) {
            try {
                this.target.onSample(fullSample);
            } catch ( Exception exc ) {
                throw new RuntimeException("report failure", exc);
            }
        }
    }
}
