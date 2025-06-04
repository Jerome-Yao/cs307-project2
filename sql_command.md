```sql
# 1.1
show tables;

describe t;

create table m(id int);
drop table m;

explain select t.id, t.name from t where t.age>18;

# 1.2
select t.id, t.name, t.age from t where t.age > 18;

select * from t where t.id=5 and t.age=18;
select * from t where t.id=5 or t.age>19;

select * from t where t.age > 18;
select * from t where t.age >= 18;
select * from t where t.age < 20;
select * from t where t.age <= 20;

create table delTemp (id int , name char);
insert into delTemp (id, name) values (2,'abd');
insert into delTemp (id, name) values (3,'abd');
insert into delTemp (id, name) values (4,'abd');
insert into delTemp (id, name) values (1,'abd');
insert into delTemp (id, name) values (5,'abd');
select * from delTemp;
delete from delTemp where delTemp.id >2;

# 

# aggregate, groupby, orderby
create table t( id int, name char, age int, gpa float);
insert into t (id, name, age, gpa) values (5,'abd', 18,3.6);
insert into t (id, name, age, gpa) values (6,'abd', 18,3.7);
insert into t (id, name, age, gpa) values (7,'abd', 18,3.6);
insert into t (id, name, age, gpa) values (8,'abd', 19,3.6);
insert into t (id, name, age, gpa) values (9,'abd', 19,3.9);
insert into t (id, name, age, gpa) values (10,'abd', 20,3.6);
insert into t (id, name, age, gpa) values (10,'abd', 20,4.0);
insert into t (id, name, age, gpa) values (10,'abd', 20,3.6);
select * from t;
select max(t.gpa), t.age from t group by t.age;
select min(t.gpa), t.age from t group by t.age;
select sum(t.gpa), t.age from t group by t.age;
select count(t.gpa), t.age from t group by t.age;
select count(distinct t.gpa), t.age from t group by t.age;

select * from t group by t.gpa
select * from t group by t.age order by t.id

# loop join
create table jointemp (id int , name char);
insert into jointemp (id, name) values (5,'abd');
insert into jointemp (id, name) values (6,'abd');
insert into jointemp (id, name) values (7,'abd');
insert into jointemp (id, name) values (8,'abd');
select * from t join jointemp on t.id = jointemp.id;

create table xjointemp (id int , name char);
insert into xjointemp (id, name) values (8,'cc');
insert into xjointemp (id, name) values (6,'bb');
insert into xjointemp (id, name) values (7,'dd');

select * from t join jointemp on t.id = jointemp.id join xjointemp on t.id = xjointemp.id


# in, not in 
select * from t where t.id in (5,6);
select * from t where t.id not in (5,6);
select * from t where t.id in (select jointemp.id from jointemp)
select * from t where t.id in (select xjointemp.id from xjointemp)
select jointemp.id from jointemp;


# b tree
create table b (id int, age int);
insert into b (id, age) values (5,1);
insert into b (id, age) values (6,2);
insert into b (id, age) values (7,3);
insert into b (id, age) values (8,4);
insert into b (id, age) values (9,5);
insert into b (id, age) values (10,6);
insert into b (id, age) values (11,7);
insert into b (id, age) values (12,8);
insert into b (id, age) values (13,9);
insert into b (id, age) values (14,10);
insert into b (id, age) values (15,11);
insert into b (id, age) values (16,12);
insert into b (id, age) values (17,13);
insert into b (id, age) values (18,14);
insert into b (id, age) values (19,15);
insert into b (id, age) values (20,16);
insert into b (id, age) values (21,17);
insert into b (id, age) values (22,18);
insert into b (id, age) values (23,19);
insert into b (id, age) values (24,20);
```
```
