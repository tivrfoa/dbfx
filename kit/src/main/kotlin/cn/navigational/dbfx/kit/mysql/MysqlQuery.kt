package cn.navigational.dbfx.kit.mysql

import cn.navigational.dbfx.kit.SQLExecutor
import cn.navigational.dbfx.kit.SQLQuery
import cn.navigational.dbfx.kit.config.NULL_TAG
import cn.navigational.dbfx.kit.enums.Clients
import cn.navigational.dbfx.kit.model.TableColumnMeta
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlClient
import io.vertx.sqlclient.Tuple

class MysqlQuery : SQLQuery {

    override suspend fun showDbInfo(client: SqlClient): RowSet<Row> {
        val sql = "SHOW VARIABLES"
        return SQLExecutor.executeSql(sql, Clients.MYSQL, client)
    }

    override suspend fun showDbVersion(client: SqlClient): String {
        val rowSet = showDbInfo(client)
        return MysqlHelper.getMysqlVersion(rowSet)
    }

    override suspend fun showDatabase(client: SqlClient): List<String> {
        val sql = "SHOW DATABASES"
        val rows = SQLExecutor.executeSql(sql, Clients.MYSQL, client)
        val list = arrayListOf<String>()
        for (row in rows) {
            val name = row.getString("Database")
            list.add(name)
        }
        return list
    }

    override suspend fun showCollations(client: SqlClient): List<String> {
        val sql = "SHOW COLLATION"
        val rowSet = SQLExecutor.executeSql(sql, Clients.MYSQL, client)
        val list = arrayListOf<String>()
        for (row in rowSet) {
            list.add(row.getString("Collation"))
        }
        return list
    }

    override suspend fun queryDbUser(client: SqlClient): List<String> {
        val sql = "SELECT user,host FROM mysql.user"
        val rowSet = SQLExecutor.executeSql(sql, Clients.MYSQL, client)
        val list = arrayListOf<String>()
        for (row in rowSet) {
            val user = row.getString("user")
            val host = row.getString("host")
            list.add("$user@$host")
        }
        return list
    }

    override suspend fun showTable(category: String, client: SqlClient): List<String> {
        val sql = "SELECT TABLE_NAME FROM `information_schema`.`TABLES` WHERE (`table_type` ='BASE TABLE' OR `TABLE_TYPE`='SYSTEM VIEW') AND table_schema =?"
        return _showTable(sql, Tuple.of(category), client)
    }

    override suspend fun showTableField(category: String, table: String, client: SqlClient): List<TableColumnMeta> {
        val sql = """
            SELECT * FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=? AND  TABLE_NAME=?
        """.trimIndent()
        val tuple = Tuple.of(category, table)
        val rowSet = SQLExecutor.executeSql(sql, Clients.MYSQL, client, tuple)
        val list = arrayListOf<TableColumnMeta>()
        for (row in rowSet) {
            val columnMeta = TableColumnMeta()
            val column = row.getString("COLUMN_NAME")
            val comment = row.getString("COLUMN_COMMENT")
            val length = row.getInteger("CHARACTER_MAXIMUM_LENGTH")
            val type = row.getString("DATA_TYPE")
            val pos = row.getInteger("ORDINAL_POSITION")
            val nullable = row.getString("IS_NULLABLE") == "YES"
            columnMeta.colName = column
            columnMeta.comment = comment
            columnMeta.length = length
            columnMeta.type = type
            columnMeta.position = pos
            columnMeta.isNullable = nullable
            columnMeta.dataType = getMyDataType(type)
            list.add(columnMeta)
        }
        list.sortBy { it.position }
        return list
    }

    override suspend fun pageQuery(category: String, table: String, pageIndex: Int, pageSize: Int, client: SqlClient): RowSet<Row> {
        val name = MysqlHelper.transTableName(category, table)
        val sql = "SELECT * FROM $name LIMIT ?,?"
        val offset = MysqlPageHelper.getStartNum(pageIndex, pageSize)
        val tuple = Tuple.of(offset, pageSize)

        return SQLExecutor.executeSql(sql, Clients.MYSQL, client, tuple)
    }

    override suspend fun queryTableTotal(category: String, table: String, client: SqlClient): Long {
        val name = MysqlHelper.transTableName(category, table)
        val sql = "SELECT COUNT(*) FROM $name"
        val rowSet = SQLExecutor.executeSql(sql, Clients.MYSQL, client)
        var totalNumber = 0L
        for (row in rowSet) {
            totalNumber = row.getLong(0)
        }
        return totalNumber
    }

    override suspend fun showView(category: String, client: SqlClient): List<String> {
        val sql = "SELECT TABLE_NAME FROM `information_schema`.`TABLES` WHERE (`table_type` ='VIEW' OR `TABLE_TYPE`='SYSTEM VIEW') AND table_schema =?"
        return _showTable(sql, Tuple.of(category), client)
    }

    private suspend fun _showTable(sql: String, tuple: Tuple, client: SqlClient): List<String> {
        val rowSet = SQLExecutor.executeSql(sql, Clients.MYSQL, client, tuple)
        val list = arrayListOf<String>()
        for (row in rowSet) {
            list.add(row.getString("TABLE_NAME"))
        }
        return list
    }
}