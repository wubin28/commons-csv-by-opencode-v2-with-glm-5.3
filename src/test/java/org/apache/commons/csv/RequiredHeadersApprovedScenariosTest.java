/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.csv;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Acceptance suite for {@link CSVFormat.Builder#setRequiredHeaders(String...)}, driven by approved markdown fixtures
 * (Approved Scenarios pattern): each {@code *.approved.md} file under {@code src/test/resources/org/apache/commons/csv/requiredheaders/} is both
 * the review artifact and the only test input. Design source: {@code ch03-3-decision-tables.md} (11 rules / 21 cases), decisions D1-D10 in
 * {@code ch03-2-adr.md}.
 *
 * <p>
 * The three entry points mirror the three decision tables of the design document:
 * </p>
 * <ul>
 * <li>{@link #tableA_buildPhase(RequiredHeadersScenario)}: table A, the build-time decision point ({@code Builder.get()} / {@code validate()}).</li>
 * <li>{@link #tableB_parsePhase(RequiredHeadersScenario)}: table B, the parse-time decision point ({@code CSVParser} construction / {@code createHeaders()}).</li>
 * <li>{@link #tableC_printPhase(RequiredHeadersScenario)}: table C, the print-time decision point ({@code CSVPrinter}).</li>
 * </ul>
 *
 * <p>
 * <strong>VSCode note</strong>: JUnit 5 computes parameterized invocations at run time, so the Testing panel only lists the per-fixture child nodes
 * after the enclosing method has been Run once as a whole. After that, every fixture is an independent node that can be re-run or
 * right-clicked "Debug Test" individually.
 * </p>
 */
class RequiredHeadersApprovedScenariosTest {

    private static final String FIXTURE_ROOT = "/org/apache/commons/csv/requiredheaders/";

    @ParameterizedTest(name = "{0}")
    @MethodSource("tableAFixtures")
    void tableA_buildPhase(final RequiredHeadersScenario scenario) throws Exception {
        scenario.execute();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tableBFixtures")
    void tableB_parsePhase(final RequiredHeadersScenario scenario) throws Exception {
        scenario.execute();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tableCFixtures")
    void tableC_printPhase(final RequiredHeadersScenario scenario) throws Exception {
        scenario.execute();
    }

    static Stream<Named<RequiredHeadersScenario>> tableAFixtures() throws IOException, URISyntaxException {
        return fixtures("table-a");
    }

    static Stream<Named<RequiredHeadersScenario>> tableBFixtures() throws IOException, URISyntaxException {
        return fixtures("table-b");
    }

    static Stream<Named<RequiredHeadersScenario>> tableCFixtures() throws IOException, URISyntaxException {
        return fixtures("table-c");
    }

    private static Stream<Named<RequiredHeadersScenario>> fixtures(final String tableDir) throws IOException, URISyntaxException {
        final Path dir = Paths.get(RequiredHeadersApprovedScenariosTest.class.getResource(FIXTURE_ROOT + tableDir).toURI());
        try (Stream<Path> files = Files.list(dir)) {
            final List<Named<RequiredHeadersScenario>> named = files
                    .filter(path -> path.getFileName().toString().endsWith(".approved.md"))
                    .sorted()
                    .map(RequiredHeadersApprovedScenariosTest::parseUnchecked)
                    .map(scenario -> Named.of(scenario.displayName(), scenario))
                    .collect(Collectors.toList());
            return named.stream();
        }
    }

    private static RequiredHeadersScenario parseUnchecked(final Path path) {
        try {
            return RequiredHeadersScenario.parse(path);
        } catch (final IOException e) {
            throw new UncheckedIOException("Cannot parse fixture: " + path, e);
        }
    }
}
