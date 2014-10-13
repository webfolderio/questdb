---
layout: nav
---

## Release notes
---
# [2.1.0 THOR] (https://github.com/NFSdb/nfsdb/releases/tag/2.1.0) - 14 Oct 2014

### SSL encryption. 

Replication can be done over secure channel using industry-standard SSL encryption protocol. Configuring SSL is notoriously complicated, so I tried my best to make it as simple as possible for both `JournalServer` and `JournalClient`. I shall let you be the judge though. [More...] (https://github.com/NFSdb/nfsdb/wiki/Replication-over-SSL)

### SSL authentication. 

Client Certificate came almost for free after implementing SSL. I did try to make it simple for you though. [More...] (https://github.com/NFSdb/nfsdb/wiki/Authentication#client-certificate-authentication)


### Shared secret authentication. 

It is now possible for client and server to exchange shared secret for authentication and authorization purposes. Shared Secret can be anything you want it to be, there is no limit on amount of data you can send. [More...] (https://github.com/NFSdb/nfsdb/wiki/Authentication#shared-secret-authentication)

### Kerberos authentication.

Usually Kerberos is a pain, not with NFSdb, not anymore. Kerberos implementation uses Shared Secret exchange mechanism and in addition it handles generation and validation of `service tokens`, Your application deals with usernames. It is that cool! [More...] (https://github.com/NFSdb/nfsdb/wiki/Authentication#kerberos-authentication)


### Kerberos integrated Windows authentication. 

If you have `JournalClient` running on Windows platform you can generate `service token` for currently logged in user with minimum fuss. User is not prompted for password! [More...] (https://github.com/NFSdb/nfsdb/wiki/Authentication#kerberos-authentication)

###  JournalClient auto-reconnect

`JournalClient` can automatically recover after server restarts. If you have multiple clients replicating single server you can restart server without needing to restart clients afterwards. Whats cooler still is that when you shutdown server clients can failover automatically. `JournalClient` will also maintain transactional consistency during server outages.

## Compatibility warning

Replication protocol is incompatible with previous release of NFSdb. I had to change the protocol to accomodate shared secret exchange.

---
# [2.0.1 DRAX] (https://github.com/NFSdb/nfsdb/releases/tag/2.0.1) - 1 Sep 2014


- #10 Support for sublasses
- #16 Fixed defect in BulkWriter that could cause index data loss
- Reader creates new journal if one does not exist instead of throwing exception.
- #15 Unique key support
- #4 Data increment processing

## Unique key support

Unique keys can be either `String` or `int` fields. In fact it is just an indexed field that you can search.

Configure `int` "id" field as follows:

```java
JournalFactory factory = new JournalFactory(new JournalConfigurationBuilder() { {
    $(Order.class)
        .$int("id").index()
        // or .$str("id).index() for String keys
    ;
}}.build(args[0]));
```

To prepare for search by "id" create parameterisable query stream. It is analogous to PreparedStatement.

```java

Journal<Order> reader = factory.reader(Order.class);
Order order = new Order();
Q q = new QImpl();
    
StringRef col = new StringRef();
col.value = "id";
IntRef id = new IntRef();
    
DataSource<Order> ds = q.ds(
        q.forEachPartition(
                q.sourceDesc(reader)
                , q.headEquals(col, id)
        )
        , order
);
```

Execute query for given "id":
```java
id.value = i;
Order o = ds.$new().head();
```

## Data increment processing

On client have a reader for journal that is being replicated. `increment()` and `incrementBuffered()` create iterator over added data. 

This is fully featured client that measures transaction latency.

```java
public static void main(String[] args) throws Exception {
    JournalFactory factory = new JournalFactory(args[0]);
    final JournalClient client = new JournalClient(factory);

    final Journal<Price> reader = factory.bulkReader(Price.class, "price-copy");

    client.subscribe(Price.class, null, "price-copy", new TxListener() {
        @Override
        public void onCommit() {
            int count = 0;
            long t = 0;
            for (Price p : reader.incrementBuffered()) {
                if (count == 0) {
                    t = p.getTimestamp();
                }
                count++;
            }
            System.out.println("took: "
                            + (System.currentTimeMillis() - t) 
                            + ", count=" + count);
        }
    });
    client.start();
    System.out.println("Client started");
}
```
