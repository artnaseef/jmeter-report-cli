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

package com.artnaseef.jmeter.report.jtl.model;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by art on 4/8/15.
 */
public class Sample {
    private String label;
    private int resultCode;
    private long timestamp;
    private boolean execError;
    private boolean failure;

    // Note: assertion errors mean the assertion evaluation failed, while failures mean the assertion proved incorrect
    private List<Assertion> assertions;
    private List<Sample> subSamples;

    public Sample() {
        this.assertions = new LinkedList<>();
        this.subSamples = new LinkedList<>();
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public int getResultCode() {
        return resultCode;
    }

    public void setResultCode(int resultCode) {
        this.resultCode = resultCode;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isExecError() {
        return execError;
    }

    public void setExecError(boolean execError) {
        this.execError = execError;
    }

    public boolean isFailure() {
        return failure;
    }

    public void setFailure(boolean failure) {
        this.failure = failure;
    }

    public List<Assertion> getAssertions() {
        return assertions;
    }

    public void addAssertion(Assertion assertion) {
        this.assertions.add(assertion);

        if (assertion.isAssertionError()) {
            this.execError = true;
        }

        if (assertion.isAssertionFailure()) {
            this.failure = true;
        }
    }

    public List<Sample> getSubSamples() {
        return subSamples;
    }

    public void addSubSample(Sample subSample) {
        this.subSamples.add(subSample);

        if (subSample.isExecError()) {
            this.execError = true;
        }

        if (subSample.isFailure()) {
            this.failure = true;
        }
    }
}
