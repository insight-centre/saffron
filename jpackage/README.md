Platform-specific installer
===========================

To run this first locate the following files
(normally in `JAVA_HOME`) and copy them to `java-mods/`

* `java.base.jmod`
* `java.logging.jmod`
* `java.sql.jmod`
* `java.transaction.xa.jmod`
* `java.xml.jmod`

Then run:

    mvn clean install
