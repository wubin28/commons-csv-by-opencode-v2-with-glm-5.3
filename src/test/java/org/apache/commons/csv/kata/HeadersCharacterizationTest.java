/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.commons.csv.kata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.DuplicateHeaderMode;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 特征测试（characterization tests）：钉住 commons-csv 当前与"表头/列名/取值"相关的行为。
 *
 * <p>这些测试只固化现有行为，为后续章节的 {@code setRequiredHeaders(String...)} 需求充当
 * 不可变的回归安全网：新功能不得改变这里断言的任何行为。场景编号（A1–D2）与
 * {@code ch02-1-2-scenarios-for-warming-up.md} 一一对应。</p>
 */
class HeadersCharacterizationTest {

    /** A2 场景的标准格式：自动探测首行为表头（{@code setHeader()} 无参）并跳过该行。 */
    private static CSVFormat headerFromFirstRecord() {
        return CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).get();
    }

    private static List<CSVRecord> parseRecords(final CSVFormat format, final String csv) throws IOException {
        try (CSVParser parser = format.parse(new StringReader(csv))) {
            return parser.getRecords();
        }
    }

    /** A 组：表头从哪来（format 配置 vs 文件内容）。 */
    @Nested
    class HeaderSources {

        /** A1：无表头 CSV，按 {@link CSVRecord#get(int)} 下标取值；按列名取值直接失败。 */
        @Test
        void noHeaderGetValueByIndexAndGetByNameThrows() throws IOException {
            final CSVFormat format = CSVFormat.DEFAULT;
            final List<CSVRecord> records = parseRecords(format, "USD,100\r\nCNY,200\r\n");
            assertEquals(2, records.size());
            assertEquals("USD", records.get(0).get(0));
            assertEquals("100", records.get(0).get(1));
            // 该格式没有 headerMap
            try (CSVParser parser = format.parse(new StringReader("USD,100\r\n"))) {
                assertNull(parser.getHeaderMap());
            }
            // 没有 headerMap 时按名取值：IllegalStateException（不是 IllegalArgumentException）
            final IllegalStateException e =
                    assertThrows(IllegalStateException.class, () -> records.get(0).get("currency"));
            assertTrue(e.getMessage().contains("No header mapping was specified"));
        }

        /** A2a：setHeader() 无参 = 自动把首行当表头，表头行不出现在数据记录里。 */
        @Test
        void autoDetectedHeaderMapsNamesAndSkipsHeaderRecord() throws IOException {
            final CSVFormat format = headerFromFirstRecord();
            final List<CSVRecord> records = parseRecords(format, "id,currency,amount\r\n1,USD,100\r\n2,CNY,200\r\n");
            assertEquals(2, records.size());
            assertEquals("1", records.get(0).get("id"));
            assertEquals("USD", records.get(0).get("currency"));
            assertEquals("100", records.get(0).get("amount"));
            try (CSVParser parser = format.parse(new StringReader("id,currency,amount\r\n1,USD,100\r\n"))) {
                assertEquals(3, parser.getHeaderMap().size());
                assertEquals(Integer.valueOf(1), parser.getHeaderMap().get("currency"));
            }
        }

        /** A2b：自动探测分支下，不设 skipHeaderRecord 也一样：首行在 createHeaders() 内已被消费。 */
        @Test
        void autoDetectedHeaderWithoutSkipFlagStillSkipsHeaderLine() throws IOException {
            final CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().get();
            final List<CSVRecord> records = parseRecords(format, "id,currency,amount\r\n1,USD,100\r\n");
            assertEquals(1, records.size());
            assertEquals("USD", records.get(0).get("currency"));
        }

        /** A3：手工指定表头，源数据本身没有表头行。 */
        @Test
        void manualHeaderOnHeaderlessSource() throws IOException {
            final CSVFormat format = CSVFormat.DEFAULT.builder().setHeader("id", "currency", "amount").get();
            final List<CSVRecord> records = parseRecords(format, "1,USD,100\r\n2,CNY,200\r\n");
            assertEquals(2, records.size());
            assertEquals("USD", records.get(0).get("currency"));
        }

        /** A4：手工表头 + 源首行也是表头 → skipHeaderRecord(true) 跳过首行。 */
        @Test
        void manualHeaderWithSkipHeaderRecordSkipsFileHeaderLine() throws IOException {
            final CSVFormat format = CSVFormat.DEFAULT.builder().setHeader("id", "currency", "amount")
                    .setSkipHeaderRecord(true).get();
            final List<CSVRecord> records = parseRecords(format, "id,currency,amount\r\n1,USD,100\r\n");
            assertEquals(1, records.size());
            assertEquals("1", records.get(0).get(0));
            assertEquals("100", records.get(0).get("amount"));
        }

        /** A4b：手工表头但忘设 skipHeaderRecord → 文件自己的表头行被当作第一条数据。 */
        @Test
        void manualHeaderWithoutSkipReadsFileHeaderLineAsData() throws IOException {
            final CSVFormat format = CSVFormat.DEFAULT.builder().setHeader("id", "currency", "amount").get();
            final List<CSVRecord> records = parseRecords(format, "id,currency,amount\r\n1,USD,100\r\n");
            assertEquals(2, records.size());
            assertEquals("id", records.get(0).get(0));
        }
    }

    /** B 组：按列名取值的边界。 */
    @Nested
    class NameAccessBoundaries {

        /** B1：取不存在的列名 → 需求原文提到的那个报错（真实的完整消息还有后半句）。 */
        @Test
        void getByNameWithUnknownNameThrowsMappingNotFound() throws IOException {
            final List<CSVRecord> records = parseRecords(headerFromFirstRecord(), "id,currency,amount\r\n1,USD,100\r\n");
            final IllegalArgumentException e =
                    assertThrows(IllegalArgumentException.class, () -> records.get(0).get("nope"));
            assertTrue(e.getMessage().contains("Mapping for nope not found"));
            assertTrue(e.getMessage().contains("expected one of"));
        }

        /** B2a：表头列数多于记录列数 → 按名取"够不到"的列报 Index 消息，isConsistent 为 false。 */
        @Test
        void headerLongerThanRecordThrowsIndexMessage() throws IOException {
            final CSVFormat format = CSVFormat.DEFAULT.builder().setHeader("id", "currency", "amount").get();
            final List<CSVRecord> records = parseRecords(format, "1,USD\r\n");
            assertEquals(1, records.size());
            assertFalse(records.get(0).isConsistent());
            final IllegalArgumentException e =
                    assertThrows(IllegalArgumentException.class, () -> records.get(0).get("amount"));
            assertTrue(e.getMessage().contains("Index for header 'amount' is 2 but CSVRecord only has 2 values"));
        }

        /** B2b：记录列数多于表头列数 → 已映射列仍可取，isConsistent 为 false。 */
        @Test
        void recordLongerThanHeaderIsInconsistentButReadable() throws IOException {
            final CSVFormat format = CSVFormat.DEFAULT.builder().setHeader("id", "currency", "amount").get();
            final List<CSVRecord> records = parseRecords(format, "1,USD,100,extra\r\n");
            assertEquals(4, records.get(0).size());
            assertEquals("100", records.get(0).get("amount"));
            assertFalse(records.get(0).isConsistent());
        }

        /** B3a：仅表头行 → 数据记录为空，headerMap 仍完整可用。 */
        @Test
        void headerOnlyFileYieldsNoRecordsButHeaderMap() throws IOException {
            try (CSVParser parser = headerFromFirstRecord().parse(new StringReader("id,currency,amount\r\n"))) {
                assertTrue(parser.getRecords().isEmpty());
                assertEquals(3, parser.getHeaderMap().size());
                assertEquals(Arrays.asList("id", "currency", "amount"), parser.getHeaderNames());
            }
        }

        /** B3b：空文件 + 自动探测 → headerMap 是空 Map（不是 null）。 */
        @Test
        void emptyFileWithAutoDetectedHeaderYieldsEmptyHeaderMap() throws IOException {
            try (CSVParser parser = headerFromFirstRecord().parse(new StringReader(""))) {
                assertTrue(parser.getRecords().isEmpty());
                assertNotNull(parser.getHeaderMap());
                assertTrue(parser.getHeaderMap().isEmpty());
            }
        }
    }

    /** C 组：重复列名 / 空列名的容错（DuplicateHeaderMode、allowMissingColumnNames）。 */
    @Nested
    class DuplicateAndBlankHeaders {

        /** C1a：手工表头重复 + DISALLOW → 在 builder.get()（构造 CSVFormat）时就抛错。 */
        @Test
        void duplicateManualHeaderWithDisallowThrowsAtFormatBuild() {
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> CSVFormat.DEFAULT.builder().setHeader("id", "currency", "id")
                            .setDuplicateHeaderMode(DuplicateHeaderMode.DISALLOW).get());
            assertTrue(e.getMessage().contains("duplicate name"));
            assertTrue(e.getMessage().contains("id"));
        }

        /** C1b：文件自动探测的表头重复 + DISALLOW → 在 format.parse()（构造 CSVParser）时抛错，早于任何数据记录。 */
        @Test
        void duplicateFileHeaderWithDisallowThrowsAtParserCreation() {
            final CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true)
                    .setDuplicateHeaderMode(DuplicateHeaderMode.DISALLOW).get();
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> format.parse(new StringReader("id,currency,id\r\n1,USD,2\r\n")));
            assertTrue(e.getMessage().contains("duplicate name"));
        }

        /** C2：默认模式（ALLOW_ALL）下重复列名不报错；按名取值命中的是"最后一个"同名列。 */
        @Test
        void duplicateNameAllowedByDefaultResolvesToLastIndex() throws IOException {
            final CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).get();
            final List<CSVRecord> records = parseRecords(format, "id,currency,id\r\n1,USD,2\r\n");
            assertEquals(1, records.size());
            assertEquals("2", records.get(0).get("id"));
            try (CSVParser parser = format.parse(new StringReader("id,currency,id\r\n1,USD,2\r\n"))) {
                assertEquals(Integer.valueOf(2), parser.getHeaderMap().get("id"));
            }
        }

        /** C3a：空列名在 allowMissingColumnNames(true) 下会被映射，"" 也是一个可用 key。 */
        @Test
        void blankHeaderNameIsMappedWhenMissingColumnNamesAllowed() throws IOException {
            final CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true)
                    .setAllowMissingColumnNames(true).get();
            final List<CSVRecord> records = parseRecords(format, "currency,,amount\r\nUSD,100,2026-01-01\r\n");
            assertEquals("100", records.get(0).get(""));
            try (CSVParser parser = format.parse(new StringReader("currency,,amount\r\nUSD,100,2026-01-01\r\n"))) {
                assertTrue(parser.getHeaderMap().containsKey(""));
                assertEquals(Integer.valueOf(1), parser.getHeaderMap().get(""));
            }
        }

        /** C3b：DISALLOW 下两个空列名也算重复 → 在 format.parse() 时抛错。 */
        @Test
        void duplicateBlankHeadersWithDisallowThrowsAtParserCreation() {
            final CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true)
                    .setAllowMissingColumnNames(true).setDuplicateHeaderMode(DuplicateHeaderMode.DISALLOW).get();
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> format.parse(new StringReader("currency,,\r\nUSD,100\r\n")));
            assertTrue(e.getMessage().contains("duplicate"));
        }
    }

    /** D 组：与 setRequiredHeaders 需求直接相邻的现状对照。 */
    @Nested
    class RequirementAdjacent {

        /** D1：表头名带前后空白 → key 原样保留，get("currency") 不会命中 " currency "。 */
        @Test
        void headerNameWithSurroundingSpacesNeedsExactMatch() throws IOException {
            final List<CSVRecord> records =
                    parseRecords(headerFromFirstRecord(), "id, currency ,amount\r\n1,USD,100\r\n");
            assertEquals("USD", records.get(0).get(" currency "));
            final IllegalArgumentException e =
                    assertThrows(IllegalArgumentException.class, () -> records.get(0).get("currency"));
            assertTrue(e.getMessage().contains("Mapping for currency not found"));
        }

        /** D2：手工表头绑定的是"位置"，与文件首行写了什么无关。 */
        @Test
        void manualHeaderBindsPositionsNotFileHeaderText() throws IOException {
            final CSVFormat format = CSVFormat.DEFAULT.builder().setHeader("colX", "currency", "amount")
                    .setSkipHeaderRecord(true).get();
            final List<CSVRecord> records = parseRecords(format, "id,currency,amount\r\n1,USD,100\r\n");
            assertEquals("1", records.get(0).get("colX"));
            assertEquals("USD", records.get(0).get("currency"));
            try (CSVParser parser = format.parse(new StringReader("id,currency,amount\r\n1,USD,100\r\n"))) {
                assertEquals(Integer.valueOf(0), parser.getHeaderMap().get("colX"));
            }
        }
    }
}
