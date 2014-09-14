---
layout: nav
---

# [2.0.1 DRAX] (https://github.com/NFSdb/nfsdb/releases/tag/2.0.1)


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
