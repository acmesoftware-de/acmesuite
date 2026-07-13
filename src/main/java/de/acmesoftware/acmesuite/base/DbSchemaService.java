package de.acmesoftware.acmesuite.base;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Introspects the app's own Postgres schema for the Admin DB schema browser (ADMIN only). */
@Service
public class DbSchemaService {

    /** Row pages are capped so a careless page size can't pull an entire large table at once. */
    private static final int MAX_PAGE_SIZE = 200;

    private final JdbcTemplate jdbc;

    public DbSchemaService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record ColumnInfo(String name, String type, boolean nullable) {
    }

    public record TableInfo(String name, long rowCount, List<ColumnInfo> columns) {
    }

    public record RowPage(String table, int page, int size, long totalRows, List<String> columns,
            List<Map<String, Object>> rows) {
    }

    public List<TableInfo> introspect() {
        List<String> tables = tableNames();
        return tables.stream().map(this::describe).toList();
    }

    /** Empty when {@code table} isn't one of the app's own tables (never trust the path variable). */
    public Optional<RowPage> rows(String table, int page, int size) {
        if (!tableNames().contains(table)) {
            return Optional.empty();
        }
        int effectivePage = Math.max(page, 0);
        int effectiveSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        List<String> columns = columnNames(table);
        long total = rowCount(table);
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select * from public." + quoteIdent(table) + " limit ? offset ?",
                effectiveSize, effectivePage * effectiveSize);
        return Optional.of(new RowPage(table, effectivePage, effectiveSize, total, columns, rows));
    }

    private List<String> tableNames() {
        return jdbc.queryForList(
                "select table_name from information_schema.tables "
                        + "where table_schema = 'public' and table_type = 'BASE TABLE' "
                        + "order by table_name",
                String.class);
    }

    private TableInfo describe(String table) {
        List<ColumnInfo> columns = jdbc.query(
                "select column_name, data_type, is_nullable from information_schema.columns "
                        + "where table_schema = 'public' and table_name = ? order by ordinal_position",
                (rs, i) -> new ColumnInfo(rs.getString("column_name"), rs.getString("data_type"),
                        "YES".equals(rs.getString("is_nullable"))),
                table);
        return new TableInfo(table, rowCount(table), columns);
    }

    private List<String> columnNames(String table) {
        return jdbc.queryForList(
                "select column_name from information_schema.columns "
                        + "where table_schema = 'public' and table_name = ? order by ordinal_position",
                String.class, table);
    }

    private long rowCount(String table) {
        Long rowCount = jdbc.queryForObject("select count(*) from public." + quoteIdent(table), Long.class);
        return rowCount == null ? 0 : rowCount;
    }

    // Table names come from information_schema (trusted metadata, not user input); quoted
    // defensively since a handful of legacy tables use reserved-word names (e.g. "order").
    private static String quoteIdent(String ident) {
        return "\"" + ident.replace("\"", "\"\"") + "\"";
    }
}
