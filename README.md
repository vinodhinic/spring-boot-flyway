# Flyway Playground

## Objective
The application code should always be compatible with the database schema and the data itself. In order to achieve zero-downtime deployment, we use rolling updates, which means that a database must be compatible with two different application versions at the same time.

--------------
## What

Database schema migration is a process of incremental changes to the relational database structure.

![database-migration](/docs/database-migration.png)

The `.sql` files are migration scripts. In effect, the database schema is a result of all subsequently executed SQL migration scripts. These migration scripts should be stored in the version control system, usually in the [same repository](/using-mybatis-xml/src/main/resources/db/migration) as the source code.
Migration metadata table represented above is `flyway_schema_history` table. And of course, `flyway` is the migration tool. 

Flyway is upgrade only tool - i.e. you can't downgrade from V2 to V1 - There are `undo` and `repair` options in Flyway but they are not under community edition.

-----------
## Constraints
- The database version needs to be compatible with the service version all the time
- The database schema migration is not reversible

Let's analyze these points in detail.

### What is Backwards compatible change

![backward-compatible-change](/docs/backward-compatible-change.png)

**Example**

Say this was the last migrated sql at `Database v10` 
```
CREATE TABLE dataset (
   dataset_id      INT NOT NULL,
   name            VARCHAR(70),
   rank            INT NOT NULL,
   PRIMARY KEY (ID)
);
```

And `Database v11` :
```
ALTER TABLE dataset
ADD created_at TIMESTAMPTZ;
```

`Service v1.2.8 release` would have Java code to support `private Timestamp createdAt;` in the Dataset model.

Since the change is backwards-compatible - because even if we revert the Java code and leave the `created_at` column in the database, everything would work perfectly fine.

If we need to roll back the `Service v1.2.8 release`, then we deploy `Service v1.2.7`, and there is no need to do anything with the database.
Database migrations are not reversible, so we keep `Database v11`. Since the schema update is backwards-compatible, `Service v.1.2.7` works perfectly fine with `Database v11`. The same applies if we need to roll back to `Service v1.2.6`, and so on. Now, suppose `Database v10` and all other migrations are backwards-compatible, then we could roll back to any service version and everything would work correctly.

There is also no problem with the downtime. If the database migration is zero-downtime itself, then we can execute it first and then use the rolling updates for the service.

What if `Database v11` is actually about dropping `rank` column. That would be a non-backward compatible change. Since rolling back to any service < v1.2.8 would break without that column being present.

### Dealing with Non Backwards compatible change

![non-backward-compatible](/docs/non-backward-compatible.png)

`Service v1.2.5` - Stop using the `rank` column in the source code without doing any database change - this makes it backwards-compatible.

`Database v11` - where `rank` is actually dropped - non-backwards-compatible update, executed after the rollback period.

All we need to do is to delay the column removal to remove the risk associated with release `v1.2.5`

The rollback period can be very long since we aren't using the column from the database anymore. This task can be treated as a cleanup task, so even though it's non-backwards-compatible, there is no associated risk.

--------------------

## Spring Boot and flyway

I have added three flavors of using flyway with Spring Boot and Mybatis :

1. Using with mybatis :

    [using mybatis annotation mappers](using-mybatis-annotations)
    
    [using mybatis xml mappers](using-mybatis-xml)
    
    Notice the usecase I have added. There is a `CacheService` which loads the data from db at application startup. It will fail only for the first time run with a table `employee` not found error.
    - R1 first run : `CacheService` bean creation fails but migration succeeds.
    - R1 restart : `CacheService` bean creation succeeds and migration not needed.
    
    Since you would never see this error again as there is an `employee` table in DB after this release, it is crucial to note that the `CacheService` is loaded **before** the migration is applied.
    Hence the dependency on `FlywayMigrationInitializer` while initializing on all DAOs.
    Fix for this is available at springboot [2.3.0 M3](https://github.com/spring-projects/spring-boot/commit/48819253eb5171b93ede2b630fdb8e64ca845e62). More details [here](https://github.com/spring-projects/spring-boot/issues/13155) 
  
1. [using flyway placeholder](using-placeholders)

    There are multiple ways to pass placeholders to the script. For example, if you want to know which client the application is running for so that you can have SQL conditions for DML,
    
    * You can add the property in `application-<env>.properties` 
    `spring.flyway.placeholders.client=local`
    * Or you can use `ApplicationContextInitializer` like [this](using-placeholders/src/main/kotlin/com/foo/app/CustomAppInitializer.kt)
    * Or you can customize spring-boot's Flyway Configuration by using `FlywayConfigurationCustomizer` like [this](using-placeholders/src/main/kotlin/com/foo/app/CustomFlywayConfiguration.kt) 

--------------------

## Guidelines

- Do not use Repeatable migrations. These have a description and a checksum, but no version. Instead of being run just once, they are (re-)applied every time their checksum changes. They are only useful for `CREATE OR REPLACE` functions/views.
- All migration files should have prefix `V` followed by the release version. Eg : `V20200101_01__insert_datasets.sql`. If there are any files that do not adhere to this, app will not startup.
This is controlled by `spring.flyway.validate-migration-naming`
- All DB statements for a release version need not be updated in single file. Each file with sub versions can have just the changeset. When we fix things at QA, UAT stages also, we will keep adding sub-versions to the script
```
V20200101_01__create_table.sql
V20200101_02__create_another_table.sql
V20200101_03__add_data.sql
V20200101_04__fixes_on_qa_week.sql
V20200101_05__fixes_on_uat_week.sql
```
- Client level DML can be applied by checking against placeholder variable `client` which is injected into the sql script via flyway placeholders.
 So there is no need to maintain separate folder structure for client level DMLs. My opinion might change after I actually see how this works well in practice.
- Always add log statements in the sql scripts. These will be printed in the application logs which is helpful for debugging. Eg :
```
DO $$
DECLARE
    client VARCHAR(50) := '${client}';
    b integer;
BEGIN
IF LOWER(client) like '%client%' THEN
    -- insert statement here
    RAISE NOTICE 'Inserting % for client : %', b, client;
ELSE 
    RAISE NOTICE 'Not Inserting % for client : %', b, client;
END IF;
END $$ LANGUAGE plpgsql;
```
- Since the versions are not repeatable by default, and we maintain strict adherence to releasing DMLs are part of application, IMO there is no needed to add `exists` check for every insert/create statements.
 In fact, I feel it is better to let the release fail if the application expects to NOT see some records but they are already inserted with different values.

    Eg : Application is released with the following DML on a table with primary key constraint on `employee_id`

    ```
    INSERT INTO foo.employee (employee_id, name, updated_at , updated_by ) VALUES
                    (109, 'vino', now(), 'foousr');
    ```
    Wouldn't we want to know if there is already a record for employee_id 109?

---------

## Brain dump 

1. I have released `Foo-R2`. What if I had to rollback to previous release version `Foo-R1` now? Wouldn't `Foo-R1` complain about new versions in `flyway_schema_history` that are not in `db/migrations` of release artifact?

    It won't if you set `flyway.ignoreFutureMigrations` - which is set to true by default.

1. What happens when my release involves 4 sql scripts and the migration failed at second file?

    Say you released version R2 with :
    ``` 
    V1_1
    V1_2 --> failed.
    V1_3
    V1_4
    ```

    Changes done at V1_2 is reverted and execution stops there. Note that V1_1 is executed and registered in the schema metadata. This is why your database changes be backwards compatible.
    
    if you set `flyway.groups=true` then all files are executed together in single transaction.

1. Does the failed migration rollback even DDL?

    It depends on the database. Since we use Postgres which supports DDL transactions, yes, even if your V1_2 executed a create table before it failed, entire changes done in V1_2 will be rolled back.

1. How to deal with the increasing size of release artifact due to growing migration files?

    - Baseline is one strategy. But it can only be applied to an existing DB WITHOUT flyway schema history. So you would have to delete `flyway_schema_history` table and merge all statements into a baseline sql file and bootstrap flyway to work for new versions. Extremely clumsy IMO.
    - Better approach could be to use `flyway.ignoreMissingMigrations=true` and ignore older files at `.gitignore`

1. What happens when multiple replicas are released for Foo in K8? i.e. if replica-1 and replica-2 gets deployed at the same time, one of them should succeed at migrating and the other replica would simply skip migration. Does this happen today? 
    
    Yes, as per the [doc]( https://flywaydb.org/documentation/faq#parallel) only one of them would get the lock and migrate.
