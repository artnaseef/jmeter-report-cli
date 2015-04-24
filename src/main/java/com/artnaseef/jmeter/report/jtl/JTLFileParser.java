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

import com.artnaseef.jmeter.report.jtl.model.Assertion;
import com.artnaseef.jmeter.report.jtl.model.HttpSample;
import com.artnaseef.jmeter.report.jtl.model.Sample;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

/**
 * Created by art on 4/8/15.
 */
public class JTLFileParser {
    private JTLFileParseListener listener;

    public JTLFileParseListener getListener() {
        return listener;
    }

    public void setListener(JTLFileParseListener listener) {
        this.listener = listener;
    }

    public void parse(String uri) throws ParserConfigurationException, SAXException, IOException, URISyntaxException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser;
        SAXParseHandler handler = new SAXParseHandler();

        parser = factory.newSAXParser();

        // TODO: refactor so the input filestream is not created in the parser
        // Support GZIP and ZIP files
        String lowerCaseUri = uri.toLowerCase();
        if ( lowerCaseUri.endsWith(".gz") ) {
            parser.parse(new GZIPInputStream(openUriStream(uri)), handler);
        } else if ( lowerCaseUri.endsWith(".zip") ) {
            ZipInputStream zis = new ZipInputStream(openUriStream(uri));
            zis.getNextEntry();
            parser.parse(zis, handler);
        } else {
            parser.parse(uri, handler);
        }
    }

    protected InputStream openUriStream (String uriString) throws URISyntaxException, IOException {
        URI uri = new URI(uriString);

        if ( ! uri.isAbsolute() ) {
            if ( uriString.startsWith("/") ) {
                uri = new URI("file://" + uriString);
            } else {
                uri = new URI("file:" + uriString);
            }
        }

        return  uri.toURL().openStream();
    }

    protected void notifyListenerOfSample(Sample sample) {
        this.listener.onSample(sample);
    }

    protected class SAXParseHandler extends DefaultHandler {
        private int level = 0;
        private List<Sample> samples = new LinkedList<>();
        private LinkedList<Sample> currentSampleStack = new LinkedList<>();
        private Assertion assertion;
        private int assertionLevel = Integer.MIN_VALUE;

        private boolean needCharacters;
        private StringBuilder characterBuffer = new StringBuilder();

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {

            this.characterBuffer = new StringBuilder();

            this.needCharacters = false;

            if (qName.equals("sample") || qName.equals("httpSample")) {
                Sample sample = this.decodeSample(uri, localName, qName, attributes);
                this.currentSampleStack.push(sample);
            } else if (qName.equals("assertionResult")) {
                this.assertion = new Assertion();
                this.assertionLevel = level;
            } else if (qName.equals("name") || qName.equals("failure") || qName.equals("error") ||
                    qName.equals("failureMessage")) {

                if (this.level == this.assertionLevel + 1) {
                    this.needCharacters = true;
                }
            }

            this.level++;
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (this.needCharacters) {
                this.characterBuffer.append(ch, start, length);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (qName.equals("sample") || qName.equals("httpSample")) {
                Sample finishedSample = this.currentSampleStack.pop();

                if (this.currentSampleStack.isEmpty()) {
                    // Finished a top-level sample
                    notifyListenerOfSample(finishedSample);
                } else {
                    this.currentSampleStack.peekLast().addSubSample(finishedSample);
                }
            } else if (qName.equals("assertionResult")) {
                if (this.currentSampleStack.size() > 0) {
                    currentSampleStack.getLast().addAssertion(this.assertion);
                }

                this.assertion = null;
            } else if (qName.equals("name")) {
                if (this.level == this.assertionLevel + 1) {
                    this.assertion.setName(this.characterBuffer.toString());
                }
            } else if (qName.equals("failure")) {
                if (this.level == this.assertionLevel + 2) {
                    boolean failureInd = this.decodeBoolean(this.characterBuffer.toString(),
                            this.assertion.isAssertionFailure());

                    this.assertion.setAssertionFailure(failureInd);
                }
            } else if (qName.equals("error")) {
                if (this.level == this.assertionLevel + 2) {
                    boolean failureInd = this.decodeBoolean(this.characterBuffer.toString(),
                            this.assertion.isAssertionError());

                    this.assertion.setAssertionError(failureInd);
                }
            } else if (qName.equals("failureMessage")) {
                if (this.level == this.assertionLevel + 1) {
                    this.assertion.setFailureMessage(this.characterBuffer.toString());
                }
            }

            this.characterBuffer = new StringBuilder();
            this.level--;
        }

        protected Sample decodeSample(String uri, String localName, String qName, Attributes attributes) {
            Sample result;

            if (qName.equals("httpSample")) {
                result = new HttpSample();
            } else {
                result = new Sample();
            }

            result.setLabel(attributes.getValue("lb"));
            result.setTimestamp(decodeLong(attributes.getValue("ts"), -1));
            result.setResultCode(decodeResultCodeString(attributes.getValue("rc")));

            return result;
        }

        protected int decodeResultCodeString(String rcString) {
            int result = -1;
            try {
                result = Integer.valueOf(rcString);
            } catch (NumberFormatException nfExc) {
                // Return the default
            }

            return result;
        }

        protected long decodeLong(String value, long defaultValue) {
            long result;
            try {
                result = Long.valueOf(value);
            } catch (Exception exc) {
                result = defaultValue;
            }

            return result;
        }

        protected boolean decodeBoolean(String value, boolean defaultValue) {
            boolean result;

            try {
                result = Boolean.valueOf(value);
            } catch (Exception exc) {
                result = defaultValue;
            }

            return result;
        }
    }
}
