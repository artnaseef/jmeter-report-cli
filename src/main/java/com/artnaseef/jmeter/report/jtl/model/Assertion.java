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

/**
 * Created by art on 4/8/15.
 */
public class Assertion {
    private String name;
    private boolean assertionFailure;
    private boolean assertionError;
    private String failureMessage;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isAssertionFailure() {
        return assertionFailure;
    }

    public void setAssertionFailure(boolean assertionFailure) {
        this.assertionFailure = assertionFailure;
    }

    public boolean isAssertionError() {
        return assertionError;
    }

    public void setAssertionError(boolean assertionError) {
        this.assertionError = assertionError;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public void setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
    }
}
