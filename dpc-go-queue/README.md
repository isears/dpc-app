
# An HTTP API service written in Golang for que-go, an interoperable
# Golang port of the Ruby Que queuing library for PostgreSQL.


## API Semantics

```
type status = 
| COMPLETED 
| RUNNING
| FAILED

val enqueue : Random_ID -> Byte_Array -> Random_ID option
val dequeue : Random_ID -> Byte_Array option
val status : Random_ID -> Status
```
*todo*

## Starting Up PostGres Locally To Test

### Install Postgres
```shell
brew install postgresql
brew services start postgresql
psql postgres
```

then once you see the `postgres=#` prompt:

```sql
CREATE ROLE tester WITH LOGIN PASSWORD 'test123';ALTER ROLE tester CREATEDB;
exit;
```

then login as your test role
```shell
psql postgres -U tester
```
and create a test database
```sql
CREATE DATABASE queue_api_testing;
GRANT ALL PRIVILEGES ON DATABASE queue_api_testing TO tester;
```
* try running `\list queue_api_testing` to verify it's working, e.g.
```
                                   List of databases
       Name        | Owner  | Encoding |   Collate   |    Ctype    | Access privileges 
-------------------+--------+----------+-------------+-------------+-------------------
 queue_api_testing | tester | UTF8     | en_US.UTF-8 | en_US.UTF-8 | =Tc/tester       +
                   |        |          |             |             | tester=CTc/tester
(1 row)
```


## Operation

*todo*

## Testing

*todo*

