package edu.sustech.cs307.optimizer;

import edu.sustech.cs307.DBEntry;
import edu.sustech.cs307.aggregate.AggregateExpression;
import edu.sustech.cs307.aggregate.AggregateParser;
import edu.sustech.cs307.exception.DBException;
import edu.sustech.cs307.exception.ExceptionTypes;
import edu.sustech.cs307.logicalOperator.*;
import edu.sustech.cs307.logicalOperator.dml.CreateTableExecutor;
import edu.sustech.cs307.logicalOperator.dml.ExplainExecutor;
import edu.sustech.cs307.logicalOperator.dml.ShowDatabaseExecutor;
import edu.sustech.cs307.physicalOperator.PhysicalOperator;
import edu.sustech.cs307.system.DBManager;
import edu.sustech.cs307.tuple.Tuple;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.ExistsExpression;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.parser.JSqlParser;
import net.sf.jsqlparser.statement.DescribeStatement;
import net.sf.jsqlparser.statement.ExplainStatement;
import net.sf.jsqlparser.statement.ShowStatement;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.drop.Drop;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.show.ShowTablesStatement;
import net.sf.jsqlparser.statement.update.Update;

public class LogicalPlanner {

    public static LogicalOperator resolveAndPlan(
            DBManager dbManager,
            String sql) throws DBException {
        JSqlParser parser = new CCJSqlParserManager();
        Statement stmt = null;
        try {
            stmt = parser.parse(new StringReader(sql));
        } catch (JSQLParserException e) {
            throw new DBException(
                    ExceptionTypes.InvalidSQL(sql, e.getMessage()));
        }
        LogicalOperator operator = null;
        // Query
        if (stmt instanceof Select selectStmt) {
            operator = handleSelect(dbManager, selectStmt);
        } else if (stmt instanceof Insert insertStmt) {
            operator = handleInsert(dbManager, insertStmt);
        } else if (stmt instanceof Update updateStmt) {
            operator = handleUpdate(dbManager, updateStmt);
        }
        // todo: add condition of handleDelete
        else if (stmt instanceof Delete deleteStmt) {
            operator = handleDelete(dbManager, deleteStmt);
        } else if (stmt instanceof Drop dropStmt) {
            // operator = handleDrop(dbManager, dropStmt);
            dbManager.dropTable(dropStmt.getName().getName());
            return null;
        }
        // functional
        else if (stmt instanceof CreateTable createTableStmt) {
            CreateTableExecutor createTable = new CreateTableExecutor(
                    createTableStmt,
                    dbManager,
                    sql);
            createTable.execute();
            return null;
        } else if (stmt instanceof ExplainStatement explainStatement) {
            ExplainExecutor explainExecutor = new ExplainExecutor(
                    explainStatement,
                    dbManager);
            explainExecutor.execute();
            return null;
        } else if (stmt instanceof ShowTablesStatement showStatement) {
            dbManager.showTables();
            return null;
        } else if (stmt instanceof DescribeStatement describeStatement) {
            dbManager.descTable(describeStatement.getTable().getName());
            return null;
        } else if (stmt instanceof ShowStatement showStatement) {
            ShowDatabaseExecutor showDatabaseExecutor = new ShowDatabaseExecutor(showStatement);
            showDatabaseExecutor.execute();
            return null;
        } else {
            throw new DBException(
                    ExceptionTypes.UnsupportedCommand((stmt.toString())));
        }
        return operator;
    }

    public static LogicalOperator handleSelect(
            DBManager dbManager,
            Select selectStmt) throws DBException {
        PlainSelect plainSelect = selectStmt.getPlainSelect();
        if (plainSelect.getFromItem() == null) {
            throw new DBException(
                    ExceptionTypes.UnsupportedCommand((plainSelect.toString())));
        }
        LogicalOperator root = new LogicalTableScanOperator(
                plainSelect.getFromItem().toString(),
                dbManager);

        int depth = 0;
        if (plainSelect.getJoins() != null) {
            for (Join join : plainSelect.getJoins()) {
                root = new LogicalJoinOperator(
                        root,
                        new LogicalTableScanOperator(
                                join.getRightItem().toString(),
                                dbManager),
                        join.getOnExpressions(),
                        depth);
                depth += 1;
            }
        }

        // 在 Join 之后应用 Filter，Filter 的输入是 Join 的结果 (root)
        Expression whereExpr = plainSelect.getWhere();
        if (whereExpr != null) {
            // not correct! whereExpr is In Exist Expression here!
            if (whereExpr instanceof InExpression inExpression) {
                System.out.println(inExpression.getRightExpression().getClass());
                if (inExpression.getRightExpression() instanceof ParenthesedSelect subSelect) {
                    // Create a LogicalOperator for the subselect
                    LogicalOperator subOperator = handleSelect(
                            dbManager,
                            subSelect);
                    PhysicalOperator subPhysicalOperator = PhysicalPlanner.generateOperator(dbManager, subOperator);
                    List<Tuple> subOutput = new ArrayList<>();
                    subPhysicalOperator.Begin();
                    while (subPhysicalOperator.hasNext()) {
                        subPhysicalOperator.Next();
                        subOutput.add(subPhysicalOperator.Current());
                    }
                    if (subOutput.iterator().next().getValues().length != 1) {
                        throw new DBException(
                                ExceptionTypes.InvalidTableWidth(subOutput.iterator().next().getValues().length));
                    }
                    root = new LogicalFilterOperator(root, whereExpr, subOutput, inExpression.isNot());
                }
            } else if (whereExpr instanceof ExistsExpression existsExpression) {
                throw new DBException(ExceptionTypes.NOT_SUPPORTED_OPERATION);
            } else {
                // fallback
                root = new LogicalFilterOperator(root, whereExpr);
            }
        }
        // if (plainSelect.getWhere() != null) {
        // root = new LogicalFilterOperator(root, plainSelect.getWhere());
        // }

        // Check if there are aggregate functions in SELECT clause
        List<AggregateExpression> aggregates = AggregateParser.parseAggregatesFromSelectItems(
                plainSelect.getSelectItems(), plainSelect.getFromItem().toString());

        if (aggregates.size() > 0 && plainSelect.getGroupBy() != null) {
            // Use LogicalAggregateOperator for queries with aggregates or GROUP BY
            root = new LogicalAggregateOperator(root, plainSelect.getFromItem().toString(),
                    plainSelect.getGroupBy() != null ? plainSelect.getGroupBy().getGroupByExpressions() : null,
                    aggregates);
        } else if (plainSelect.getGroupBy() != null) {
            // Fallback to old GroupBy operator for GROUP BY without aggregates
            root = new LogicalGroupByOperator(root, plainSelect.getFromItem().toString(),
                    plainSelect.getGroupBy().getGroupByExpressions());
        }
        root = new LogicalProjectOperator(root, plainSelect.getSelectItems());
        return root;
    }

    private static LogicalOperator handleInsert(
            DBManager dbManager,
            Insert insertStmt) {
        return new LogicalInsertOperator(
                insertStmt.getTable().getName(),
                insertStmt.getColumns(),
                insertStmt.getValues());
    }

    private static LogicalOperator handleUpdate(
            DBManager dbManager,
            Update updateStmt) throws DBException {
        LogicalOperator root = new LogicalTableScanOperator(
                updateStmt.getTable().getName(),
                dbManager);
        return new LogicalUpdateOperator(
                root,
                updateStmt.getTable().getName(),
                updateStmt.getUpdateSets(),
                updateStmt.getWhere());
    }

    private static LogicalOperator handleDelete(
            DBManager dbManager,
            Delete deleteStmt) throws DBException {
        LogicalOperator root = new LogicalTableScanOperator(
                deleteStmt.getTable().getName(),
                dbManager);
        if (deleteStmt.getWhere() != null) {
            root = new LogicalFilterOperator(root, deleteStmt.getWhere());
        }
        return new LogicalDeleteOperator(
                root,
                deleteStmt.getTable().getName(),
                deleteStmt.getWhere());
    }

    private static LogicalOperator handleDrop(
            DBManager dbManager,
            Drop dropStmt) throws DBException {
        return new LogicalDropOperator(dropStmt.getName().getName());
    }
}
