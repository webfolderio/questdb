/*******************************************************************************
 *     ___                  _   ____  ____
 *    / _ \ _   _  ___  ___| |_|  _ \| __ )
 *   | | | | | | |/ _ \/ __| __| | | |  _ \
 *   | |_| | |_| |  __/\__ \ |_| |_| | |_) |
 *    \__\_\\__,_|\___||___/\__|____/|____/
 *
 *  Copyright (c) 2014-2019 Appsicle
 *  Copyright (c) 2019-2020 QuestDB
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/

package io.questdb.griffin.engine.union;

import io.questdb.cairo.CairoConfiguration;
import io.questdb.cairo.ColumnTypes;
import io.questdb.cairo.RecordSink;
import io.questdb.cairo.map.Map;
import io.questdb.cairo.map.MapFactory;
import io.questdb.cairo.sql.RecordCursor;
import io.questdb.cairo.sql.RecordCursorFactory;
import io.questdb.cairo.sql.RecordMetadata;
import io.questdb.griffin.SqlExecutionContext;
import io.questdb.griffin.engine.LimitOverflowException;
import io.questdb.std.Misc;

public class UnionRecordCursorFactory implements RecordCursorFactory {
    private final RecordMetadata metadata;
    private final RecordCursorFactory masterFactory;
    private final RecordCursorFactory slaveFactory;
    private final UnionRecordCursor cursor;
    private final Map map;

    public UnionRecordCursorFactory(
            CairoConfiguration configuration,
            RecordCursorFactory masterFactory,
            RecordCursorFactory slaveFactory,
            RecordSink recordSink,
            ColumnTypes valueTypes
    ) {
        this.metadata = masterFactory.getMetadata();
        this.masterFactory = masterFactory;
        this.slaveFactory = slaveFactory;
        this.map = MapFactory.createMap(configuration, metadata, valueTypes);
        this.cursor = new UnionRecordCursor(map, recordSink);
    }

    @Override
    public RecordCursor getCursor(SqlExecutionContext executionContext) {
        long maxInMemoryRows = executionContext.getCairoSecurityContext().getMaxInMemoryRows();
        RecordCursor masterCursor = masterFactory.getCursor(executionContext);
        RecordCursor slaveCursor = slaveFactory.getCursor(executionContext);
        if (maxInMemoryRows > (masterCursor.size() + slaveCursor.size())) {
            map.setMaxSize(maxInMemoryRows);
            cursor.of(
                    masterCursor,
                    slaveCursor);
            return cursor;
        }
        masterCursor.close();
        slaveCursor.close();
        throw LimitOverflowException.instance(maxInMemoryRows);
    }

    @Override
    public RecordMetadata getMetadata() {
        return metadata;
    }

    @Override
    public boolean recordCursorSupportsRandomAccess() {
        return false;
    }

    @Override
    public void close() {
        Misc.free(masterFactory);
        Misc.free(slaveFactory);
        Misc.free(map);
    }
}
