# SUSTech-CS307-2025-Spring-Project_2

## Before you clone 
Notice that PLAGIARISM WILL BE PUNISHED SEVERELY in SUSTech Computer Science and Engineering Department.

## Quick Start
- You can find the original framework of this project at https://github.com/CS307-Database/engine-project
- Run the engine in `DBEntry.java`.
- `sql_command.md` provides series of sql command examples.

## Features
- Support all basic operations. See `Project_2_Database_Design.pdf`
- Support aggregation functions.
- Support `GROUP BY`, `ORDER BY`.
- Support `Nested Loop Join` for equi-joins.
- Support `IN`, `NOT IN`
- Support SubSelect for `IN` in single Column situation. See example below.
```sql
select * from t where t.id in (select jointemp.id from jointemp)
```
- This project supports creating B+ trees index for `SELECT`,`DELETE`, holding the trees dynamically. However, since it's only *InMemory*, the trees will disappear as long as you exit the program.
- This project does not support `EXISTS`,'generate different query plans for different SQL statements',`ALTER`.
- This project does not support creating index *InPhysical* (save to Json), however the engine has provided a model class, you can complete this simple task yourself.

## Known Issues
- After dropping a table, you should manually exit the program to flush some unknown class within the engine. Though you can drop it successfully, the ColumnMeta will still stay in memory.