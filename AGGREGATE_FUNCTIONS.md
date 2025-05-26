# Aggregate Functions Implementation

## Overview
This implementation adds support for SQL aggregate functions (SUM, COUNT, AVG, MIN, MAX) to the database engine using the volcano model architecture.

## Supported Aggregate Functions

### Basic Functions
- **COUNT**: Count number of rows (supports COUNT(*) and COUNT(column))
- **SUM**: Sum of numeric values
- **AVG**: Average of numeric values  
- **MIN**: Minimum value
- **MAX**: Maximum value

### Features
- GROUP BY clause support
- DISTINCT modifier support
- Mixed aggregate and non-aggregate columns
- Proper NULL value handling

## SQL Examples

```sql
-- Basic aggregation without GROUP BY
SELECT COUNT(*) FROM student;
SELECT MIN(gpa), MAX(gpa) FROM student;

-- Aggregation with GROUP BY
SELECT department, COUNT(*), AVG(gpa) FROM student GROUP BY department;
SELECT MIN(gpa) FROM student GROUP BY department;

-- Multiple aggregates
SELECT department, MIN(gpa), MAX(gpa), COUNT(*) FROM student GROUP BY department;

-- DISTINCT aggregation
SELECT COUNT(DISTINCT department) FROM student;
SELECT SUM(DISTINCT credits) FROM course;
```

## Architecture

### Components Added

1. **AggregateFunction** (enum): Defines supported function types
2. **AggregateExpression**: Represents individual aggregate functions with target columns and aliases
3. **AggregateCalculator**: Performs actual aggregate computations
4. **AggregateParser**: Extracts aggregate functions from SELECT clauses
5. **LogicalAggregateOperator**: Logical plan representation
6. **AggregateOperator**: Physical execution operator

### Data Flow

```
SQL Query → LogicalPlanner → AggregateParser → LogicalAggregateOperator
    ↓
PhysicalPlanner → AggregateOperator → Execution
```

### Execution Process

1. **Grouping Phase**: Input tuples grouped by GROUP BY columns
2. **Aggregation Phase**: Aggregate functions computed for each group
3. **Result Phase**: One result tuple per group with GROUP BY columns + aggregate results

## Example Data Flow

**Query**: `SELECT department, MIN(gpa), COUNT(*) FROM student GROUP BY department`

**Input**:
```
[(dept=CS, gpa=3.5, name=Alice), (dept=CS, gpa=3.8, name=Bob),
 (dept=Math, gpa=3.2, name=Charlie), (dept=Math, gpa=3.9, name=David)]
```

**After Grouping**:
```
CS Group: [(dept=CS, gpa=3.5, name=Alice), (dept=CS, gpa=3.8, name=Bob)]
Math Group: [(dept=Math, gpa=3.2, name=Charlie), (dept=Math, gpa=3.9, name=David)]
```

**Final Result**:
```
[(department=CS, MIN_gpa=3.5, COUNT=2),
 (department=Math, MIN_gpa=3.2, COUNT=2)]
```

## Type System

### Result Types
- **COUNT**: Always returns INTEGER
- **AVG**: Always returns FLOAT  
- **SUM**: Preserves input type for INTEGER, returns FLOAT for others
- **MIN/MAX**: Preserve input column type

### NULL Handling
- Aggregate functions ignore NULL values
- COUNT(*) counts all rows including those with NULLs
- If all values are NULL, result is NULL (except COUNT which returns 0)

## Integration Points

### LogicalPlanner Changes
- Detects aggregate functions in SELECT clause
- Creates LogicalAggregateOperator instead of LogicalGroupByOperator when aggregates present
- Maintains backward compatibility with existing GROUP BY queries

### PhysicalPlanner Changes  
- Handles LogicalAggregateOperator → AggregateOperator conversion
- Extracts GROUP BY columns and aggregate expressions
- Builds proper output schema

## Usage Notes

1. **Mixed Queries**: Queries with both aggregate and non-aggregate columns require GROUP BY
2. **Performance**: All grouping and aggregation done in memory during Begin() phase
3. **Compatibility**: Existing GROUP BY queries without aggregates still work via LogicalGroupByOperator
4. **Schema**: Output schema includes GROUP BY columns followed by aggregate result columns

## Error Handling

- Invalid aggregate function names throw IllegalArgumentException
- Type mismatches in aggregate calculations handled gracefully
- Missing target columns in aggregate expressions return NULL results
- DBException handling for tuple access errors during aggregation