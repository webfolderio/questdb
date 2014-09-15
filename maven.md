---
layout: nav
---

##Maven
---

NFSdb requires minimum of Java 7 and stable release is available from maven Central

```xml
<dependency>
    <groupId>com.nfsdb</groupId>
    <artifactId>nfsdb-core</artifactId>
    <version>2.0.1</version>
</dependency>
```
Check out [release notes](http://github.nfsdb.org/release-notes/) for details of the release.

Snapshot releases are also available from Maven central. To get hold of those add the following lines to pom.xml:

```xml
<repositories>
    <repository>
        <id>sonatype-snapshots</id>
        <url>https://oss.sonatype.org/content/repositories/snapshots</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.nfsdb</groupId>
    <artifactId>nfsdb-core</artifactId>
    <version>2.0.2-SNAPSHOT</version>
</dependency>
```
