package com.nibbs.dao;

import org.springframework.jdbc.core.ResultSetExtractor;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * Streams a ResultSet as CSV.
 */
public class StreamingCsvResultSetExtractor
        implements ResultSetExtractor<Void> {

    private static char DELIMITER = ',';

    private final OutputStream os;

    /**
     * @param os the OutputStream to stream the CSV to
     */
    public StreamingCsvResultSetExtractor(final OutputStream os) {
        this.os = os;
    }

    @Override
    public Void extractData(final ResultSet rs) {
        try (PrintWriter pw = new PrintWriter(os, true)) {
            final ResultSetMetaData  rsmd = rs.getMetaData();
            final int columnCount = rsmd.getColumnCount();
            writeHeader(rsmd, columnCount, pw);
            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    final Object value = rs.getObject(i);
                    pw.write(value == null ? "" : value.toString());
                    if (i != columnCount) {
                        pw.append(DELIMITER);
                    }
                }
                pw.println();
            }
            pw.flush();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private static void writeHeader(final ResultSetMetaData rsmd,
                                    final int columnCount, final PrintWriter pw) throws SQLException {
        for (int i = 1; i <= columnCount; i++) {
            pw.write(rsmd.getColumnName(i));
            if (i != columnCount) {
                pw.append(DELIMITER);
            }
        }
        pw.println();
    }
}