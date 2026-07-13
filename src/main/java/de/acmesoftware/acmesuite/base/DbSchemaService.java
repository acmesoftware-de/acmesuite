package de.acmesoftware.acmesuite.base;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Introspects the app's own Postgres schema for the Admin DB schema browser (ADMIN only). */
@Service
public class DbSchemaService {

    private final JdbcTemplate jdbc;

    public DbSchemaService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record ColumnInfo(String name, String type, boolean nullable) {
    }

    public record TableInfo(String name, long rowCount, List<ColumnInfo> columns) {
    }

    public List<TableInfo> introspect() {
        List<String> tables = jdbc.queryForList(
                "select table_name from information_schema.tables "
                        + "where table_schema = 'public' and table_type = 'BASE TABLE' "
                        + "order by table_name",
                String.class);
        return tables.stream().map(this::describe).toList();
    }

    private TableInfo describe(String table) {
        List<ColumnInfo> columns = jdbc.query(
                "select column_name, data_type, is_nullable from information_schema.columns "
                        + "where table_schema = 'public' and table_name = ? order by ordinal_position",
                (rs, i) -> new ColumnInfo(rs.getString("column_name"), rs.getString("data_type"),
                        "YES".equals(rs.getString("is_nullable"))),
                table);
        Long rowCount = jdbc.queryForObject("select count(*) from public." + quoteIdent(table), Long.class);
        return new TableInfo(table, rowCount == null ? 0 : rowCount, columns);
    }

    // Table names come from information_schema (trusted metadata, not user input); quoted
    // defensively since a handful of legacy tables use reserved-word names (e.g. "order").
    private static String quoteIdent(String ident) {
        return "\"" + ident.replace("\"", "\"\"") + "\"";
    }
}
