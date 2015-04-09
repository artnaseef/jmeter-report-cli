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

package com.artnaseef.jmeter.report.jtl;

import com.artnaseef.jmeter.report.jtl.model.Sample;

import java.util.List;

/**
 * Created by art on 4/8/15.
 */
public class JTLFileParserUtil {
    JTLFileParser parser = new JTLFileParser();

    public static void main (String[] args) {
        try {
            JTLFileParserUtil mainObj = new JTLFileParserUtil();
            mainObj.instanceMain(args);
        } catch ( Exception exc ) {
            exc.printStackTrace();
        }
    }

    public void instanceMain (String[] args) throws Exception {
        this.parser.setListener(new MyJtlParseListener());

        for ( String oneArg : args ) {
            this.parser.parse(oneArg);
        }
    }

    protected class MyJtlParseListener implements JTLFileParseListener {
        @Override
        public void onSample(Sample fullSample) {
            System.out.print("SAMPLE lb='");
            System.out.print(fullSample.getLabel());
            System.out.print("' ts='");
            System.out.print(fullSample.getTimestamp());
            System.out.print("' num-sub-sample=");

            List<Sample> subSamples = fullSample.getSubSamples();
            int numSubSample;
            if ( subSamples == null ) {
                numSubSample = 0;
            } else {
                numSubSample = subSamples.size();
            }
            System.out.print(numSubSample);

            if ( fullSample.isFailure() ) {
                System.out.print(" *FAIL*");
            }

            if ( fullSample.isExecError() ) {
                System.out.print(" *ERROR*");
            }

            System.out.println();
        }
    }
}
