/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2021 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.rpc.filter;

import com.axelor.common.StringUtils;
import com.google.common.base.Joiner;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

class LogicalFilter extends Filter {

  private Operator operator;

  private List<Filter> filters;

  public LogicalFilter(Operator operator, List<Filter> filters) {
    this.operator = operator;
    this.filters = filters;
  }

  @Override
  public String getQuery() {
    if (filters == null || filters.isEmpty()) return "";

    final List<String> filterParts =
        filters.stream()
            .map(Filter::toString)
            .filter(StringUtils::notBlank)
            .collect(Collectors.toList());

    StringBuilder sb = new StringBuilder();

    if (operator == Operator.NOT) sb.append("NOT ");

    if (filterParts.size() > 1) sb.append("(");

    String joiner = operator == Operator.NOT ? " AND " : " " + operator.name() + " ";
    sb.append(Joiner.on(joiner).join(filterParts));

    if (filterParts.size() > 1) sb.append(")");

    return sb.toString();
  }

  @Override
  public List<Object> getParams() {
    List<Object> params = new ArrayList<Object>();
    for (Filter filter : filters) {
      params.addAll(filter.getParams());
    }
    return params;
  }
}
