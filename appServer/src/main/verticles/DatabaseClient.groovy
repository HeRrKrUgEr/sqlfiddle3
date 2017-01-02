import io.vertx.groovy.ext.jdbc.JDBCClient
import io.vertx.core.impl.FutureImpl

class DatabaseClient {
    static private def jdbcConfig = [
        url: "jdbc:postgresql://appDatabase:5432/sqlfiddle",
        driver_class: "org.postgresql.Driver",
        user: "postgres",
        password: "password"
    ]
    static def getConnection(vertx, fn) {
        JDBCClient
            .createShared(vertx, jdbcConfig, "SQLFiddle")
            .getConnection({ dbConnectionHandler ->
                if (dbConnectionHandler.succeeded()) {
                    fn(dbConnectionHandler.result())
                } else {
                    throw "Unable to get connection: " +
                        dbConnectionHandler.cause().getMessage()
                }
            })
    }

    static def singleRead(vertx, query, params, fn) {
        getConnection(vertx, {connection ->
            connection.queryWithParams(query, params, {
                def queryObj = queryResultAsBasicObj(it)
                if (queryObj.result && queryObj.result.size() == 1) {
                    fn(queryObj.result[0])
                } else {
                    fn(null)
                }
                connection.close()
            })
        })
    }

    static def queryResultAsBasicObj(FutureImpl queryResult) {
        if (queryResult.succeeded()) {
            def columnNames = queryResult.result().columnNames
            return [
                    result: queryResult.result().results.collect { row ->
                        def valueMap = [:]
                        columnNames.eachWithIndex { col, pos ->
                            valueMap[col] = row[pos]
                        }
                        return valueMap
                    }
                ]
        } else {
            return [
                message: queryResult.cause().getMessage()
            ]
        }
    }

}
