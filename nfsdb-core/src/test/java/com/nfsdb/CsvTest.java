/*
 * Copyright (c) 2014. Vlad Ilyushchenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nfsdb;

import com.nfsdb.column.ColumnType;
import com.nfsdb.exp.StringSink;
import com.nfsdb.factory.configuration.ColumnMetadata;
import com.nfsdb.imp.Csv;
import com.nfsdb.test.tools.TestUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CsvTest {

    @Test
    public void testHeaders() throws Exception {
        final List<String> names = new ArrayList<>();
        final List<String> expected = new ArrayList<String>() {{
            add("type");
            add("value");
            add("active");
            add("desc");
            add("grp");
        }};


        Csv csv = new Csv();
        csv.parse(new File(this.getClass().getResource("/csv/test-headers.csv").getFile())
                , 1024 * 1024
                , false
                , true
                , new Csv.Listener() {
                    @Override
                    public void onError(int line) {

                    }

                    @Override
                    public void onField(int index, CharSequence value, int line, boolean eol) {

                    }

                    @Override
                    public void onFieldCount(int count) {

                    }

                    @Override
                    public void onLineCount(int count) {

                    }

                    @Override
                    public void onNames(List<ColumnMetadata> meta) {
                        for (ColumnMetadata m : meta) {
                            names.add(m.name);
                        }
                    }
                }

        );

        TestUtils.assertEquals(expected.iterator(), names.iterator());

    }

    @Test
    public void testParseDosCsvSmallBuf() throws Exception {
        assertFile("/csv/test-dos.csv", 10);
    }

    @Test
    public void testParseTab() throws Exception {
        assertFile("/csv/test.txt", 1024 * 1024);
    }

    @Test
    public void testParseTabSmallBuf() throws Exception {
        assertFile("/csv/test.txt", 10);
    }

    @Test
    public void testParseUnixCsvSmallBuf() throws Exception {
        assertFile("/csv/test-unix.csv", 10);
    }

    @Test
    public void testTypeDetection() throws Exception {
        final List<ColumnMetadata> meta = new ArrayList<>();
        final List<ColumnMetadata> expected = new ArrayList<ColumnMetadata>() {{
            ColumnMetadata m;

            m = new ColumnMetadata();
            m.name = "type";
            m.type = ColumnType.STRING;
            add(m);

            m = new ColumnMetadata();
            m.name = "value";
            m.type = ColumnType.DOUBLE;
            add(m);


            m = new ColumnMetadata();
            m.name = "active";
            m.type = ColumnType.BOOLEAN;
            add(m);


            m = new ColumnMetadata();
            m.name = "desc";
            m.type = ColumnType.STRING;
            add(m);

            m = new ColumnMetadata();
            m.name = "grp";
            m.type = ColumnType.STRING;
            add(m);

        }};


        Csv csv = new Csv();
        csv.parse(
                new File(this.getClass().getResource("/csv/test-headers.csv").getFile())
                , 1024 * 1024
                , true
                , false
                , new Csv.Listener() {
                    @Override
                    public void onError(int line) {

                    }

                    @Override
                    public void onField(int index, CharSequence value, int line, boolean eol) {

                    }

                    @Override
                    public void onFieldCount(int count) {

                    }

                    @Override
                    public void onLineCount(int count) {

                    }

                    @Override
                    public void onNames(List<ColumnMetadata> m) {
                        meta.addAll(m);
                    }
                }

        );

        Assert.assertEquals(expected.size(), meta.size());
        TestUtils.assertEquals(expected.iterator(), meta.iterator());

    }

    private void assertFile(String file, long bufSize) throws Exception {
        String expected = "123,abc,2015-01-20T21:00:00.000Z,3.1415,TRUE,Lorem ipsum dolor sit amet.,122\n" +
                "124,abc,2015-01-20T21:00:00.000Z,7.342,FALSE,\"Lorem ipsum \n" +
                "\n" +
                "dolor \"\"sit\"\" amet.\",546756\n" +
                "125,abc,2015-01-20T21:00:00.000Z,9.334,,\"Lorem ipsum \"\"dolor\"\" sit amet.\",23\n" +
                "126,abc,2015-01-20T21:00:00.000Z,1.345,TRUE,\"Lorem, ipsum, dolor sit amet.\",434\n" +
                "127,abc,2015-01-20T21:00:00.000Z,1.53321,TRUE,Lorem ipsum dolor sit amet.,112\n" +
                "128,abc,2015-01-20T21:00:00.000Z,2.456,TRUE,Lorem ipsum dolor sit amet.,122\n";

        TestCsvListener listener = new TestCsvListener();

        Csv csv = new Csv();
        csv.parse(
                new File(this.getClass().getResource(file).getFile())
                , bufSize
                , false
                , false
                , listener
        );

        Assert.assertEquals(expected, listener.toString());
        Assert.assertEquals(1, listener.getErrorCounter());
        Assert.assertEquals(4, listener.getErrorLine());
        Assert.assertEquals(7, csv.getLineCount());
    }

    private static class TestCsvListener implements Csv.Listener {
        private final StringSink sink = new StringSink();
        private int errorCounter = 0;
        private int errorLine = -1;

        public int getErrorCounter() {
            return errorCounter;
        }

        public int getErrorLine() {
            return errorLine;
        }

        @Override
        public String toString() {
            return sink.toString();
        }

        @Override
        public void onError(int line) {
            errorCounter++;
            errorLine = line;
        }

        @Override
        public void onField(int index, CharSequence value, int line, boolean eol) {
            sink.put(value);
            if (eol) {
                sink.put('\n');
            } else {
                sink.put(',');
            }
        }

        @Override
        public void onNames(List<ColumnMetadata> meta) {

        }


        @Override
        public void onFieldCount(int count) {

        }

        @Override
        public void onLineCount(int count) {
        }
    }
}