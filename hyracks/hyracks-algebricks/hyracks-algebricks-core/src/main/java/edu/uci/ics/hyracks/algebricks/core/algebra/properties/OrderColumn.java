/*
 * Copyright 2009-2010 by The Regents of the University of California
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * you may obtain a copy of the License from
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.uci.ics.hyracks.algebricks.core.algebra.properties;

import edu.uci.ics.hyracks.algebricks.core.algebra.base.LogicalVariable;
import edu.uci.ics.hyracks.algebricks.core.algebra.operators.logical.OrderOperator.IOrder.OrderKind;

public final class OrderColumn {

    private LogicalVariable column;
    private OrderKind order;

    public OrderColumn(LogicalVariable column, OrderKind order) {
        this.column = column;
        this.order = order;
    }

    public LogicalVariable getColumn() {
        return column;
    }

    public OrderKind getOrder() {
        return order;
    }

    public void setColumn(LogicalVariable column) {
        this.column = column;
    }

    public void setOrder(OrderKind order) {
        this.order = order;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof OrderColumn)) {
            return false;
        } else {
            OrderColumn oc = (OrderColumn) obj;
            return column.equals(oc.getColumn()) && order == oc.getOrder();
        }
    }

    @Override
    public String toString() {
        return column.toString() + "(" + order + ")";
    }
}
