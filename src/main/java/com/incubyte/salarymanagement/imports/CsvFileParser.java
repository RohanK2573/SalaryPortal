package com.incubyte.salarymanagement.imports;

import com.incubyte.salarymanagement.api.ApiException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class CsvFileParser {
    private CsvFileParser() {}

    static List<List<String>> parse(byte[] bytes) {
        String input;
        try {
            input = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException exception) {
            throw ApiException.invalid("file", "CSV file must use valid UTF-8 encoding.");
        }
        if (!input.isEmpty() && input.charAt(0) == '\uFEFF') input = input.substring(1);
        if (input.isBlank()) throw ApiException.invalid("file", "CSV file must contain a header and data rows.");

        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        boolean closedQuote = false;
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if (quoted) {
                if (ch == '"') {
                    if (i + 1 < input.length() && input.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        quoted = false;
                        closedQuote = true;
                    }
                } else {
                    field.append(ch);
                }
                continue;
            }
            if (closedQuote) {
                if (ch == ',') {
                    row.add(field.toString());
                    field.setLength(0);
                    closedQuote = false;
                } else if (ch == '\n' || ch == '\r') {
                    row.add(field.toString());
                    field.setLength(0);
                    rows.add(List.copyOf(row));
                    row.clear();
                    closedQuote = false;
                    if (ch == '\r' && i + 1 < input.length() && input.charAt(i + 1) == '\n') i++;
                    checkRowLimit(rows.size());
                } else {
                    throw ApiException.invalid("file", "CSV contains characters after a closing quote.");
                }
            } else if (ch == '"') {
                if (field.length() != 0) throw ApiException.invalid("file", "CSV contains a quote inside an unquoted field.");
                quoted = true;
            } else if (ch == ',') {
                row.add(field.toString());
                field.setLength(0);
            } else if (ch == '\n' || ch == '\r') {
                row.add(field.toString());
                field.setLength(0);
                rows.add(List.copyOf(row));
                row.clear();
                if (ch == '\r' && i + 1 < input.length() && input.charAt(i + 1) == '\n') i++;
                checkRowLimit(rows.size());
            } else {
                field.append(ch);
            }
        }
        if (quoted) throw ApiException.invalid("file", "CSV contains an unterminated quoted field.");
        if (closedQuote || field.length() > 0 || !row.isEmpty()) {
            row.add(field.toString());
            rows.add(List.copyOf(row));
            checkRowLimit(rows.size());
        }
        if (rows.size() < 2) throw ApiException.invalid("file", "CSV must contain at least one data row.");
        return List.copyOf(rows);
    }

    private static void checkRowLimit(int rowCount) {
        if (rowCount > 10_001) throw ApiException.invalid("file", "CSV may contain at most 10,000 data rows.");
    }
}
