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

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class JPQLFilter extends Filter {

  private static final Pattern BLACKLIST_PATTERN =
      AppSettings.get().get(AvailableAppSettings.DOMAIN_BLACKLIST_PATTERN, null) == null
          ? null
          : Pattern.compile(
              AppSettings.get().get(AvailableAppSettings.DOMAIN_BLACKLIST_PATTERN).trim(),
              Pattern.CASE_INSENSITIVE);

  private String jpql;

  private Object[] params;

  public JPQLFilter(String jpql, Object... params) {
    this.jpql = jpql;
    this.params = params;
  }

  public static JPQLFilter forDomain(String jpql, Object... params) {
    if (BLACKLIST_PATTERN != null && BLACKLIST_PATTERN.matcher(jpql).find()) {
      throw new IllegalArgumentException("Invalid domain, filter uses blacklisted keywords.");
    }
    return new JPQLFilter(jpql, params);
  }

  @Override
  public String getQuery() {
    return "(" + this.jpql + ")";
  }

  @Override
  public List<Object> getParams() {
    return Arrays.asList(this.params);
  }
}
