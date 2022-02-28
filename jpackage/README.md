Platform-specific installer
===========================

To run this first locate the following files
(normally in `JAVA_HOME`) and copy them to `java-mods/`

* `java.base.jmod`
* `java.datatransfer.jmod`
* `java.desktop.jmod`
* `java.logging.jmod`
* `java.prefs.jmod`
* `java.sql.jmod`
* `java.transaction.xa.jmod`
* `java.xml.jmod`
* `jdk.crypto.cryptoki`
* `jdk.crypto.ec`

Then run:

    mvn clean install
