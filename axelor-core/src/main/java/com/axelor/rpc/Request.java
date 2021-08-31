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
package com.axelor.rpc;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.script.CompositeScriptHelper;
import com.axelor.script.ScriptBindings;
import com.axelor.script.ScriptHelper;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Request {

  static final ThreadLocal<Request> CURRENT = new ThreadLocal<Request>();

  private int limit;

  private int offset;

  private List<String> sortBy;

  private Map<String, Object> data;

  private List<Object> records;

  private Criteria criteria;

  private List<String> fields;

  private Map<String, List<String>> related;

  private String model;

  private Context context;

  private ScriptHelper scriptHelper;

  public static Request current() {
    return CURRENT.get();
  }

  public String getModel() {
    return model;
  }

  /**
   * Set the model class that represents the request {@link #data}.
   *
   * @param model the model class
   */
  public void setModel(String model) {
    this.model = model;
  }

  /**
   * Get the entity class on which the operation is being performed.
   *
   * @return bean class
   */
  @JsonIgnore
  public Class<?> getBeanClass() {
    try {
      return Class.forName(model);
    } catch (NullPointerException e) {
    } catch (ClassNotFoundException e) {
    }
    return null;
  }

  /**
   * Get the current session user.
   *
   * @return current session {@link User}
   */
  @JsonIgnore
  public User getUser() {
    return AuthUtils.getUser();
  }

  public int getLimit() {
    return limit;
  }

  public void setLimit(int limit) {
    this.limit = limit;
  }

  public int getOffset() {
    return offset;
  }

  public void setOffset(int offset) {
    this.offset = offset;
  }

  public List<String> getSortBy() {
    return sortBy;
  }

  public void setSortBy(List<String> sortBy) {
    this.sortBy = sortBy;
  }

  public Map<String, Object> getData() {
    return data;
  }

  public void setData(Map<String, Object> data) {
    this.data = data;
  }

  public List<Object> getRecords() {
    return records;
  }

  public void setRecords(List<Object> records) {
    this.records = records;
  }

  public List<String> getFields() {
    return fields;
  }

  public void setFields(List<String> fields) {
    this.fields = fields;
  }

  public Map<String, List<String>> getRelated() {
    return related;
  }

  public void setRelated(Map<String, List<String>> related) {
    this.related = related;
  }

  @JsonIgnore
  public Criteria getCriteria() {
    if (criteria == null && getData() != null) {
      criteria = Criteria.parse(this);
    }
    return criteria;
  }

  /**
   * Get the raw context.
   *
   * <p>The returned map is mutable so updating this map may cause some unexpected results. So use
   * this method with care.
   *
   * @return map of context values
   */
  @SuppressWarnings("all")
  public Map<String, Object> getRawContext() {

    final Map<String, Object> data = getData();
    final Map<String, Object> ctx = new HashMap<>();

    if (data == null) {
      return ctx;
    }

    if (data.get("context") != null) {
      ctx.putAll((Map) data.get("context"));
    }
    if (data.get("_domainContext") != null) {
      ctx.putAll((Map) data.get("_domainContext"));
    }

    return ctx;
  }

  /**
   * Get a {@link ScriptHelper} to evaluate expressions with current context.
   *
   * @return an instance of {@link ScriptHelper} for current context
   */
  @JsonIgnore
  public ScriptHelper getScriptHelper() {
    if (scriptHelper != null) {
      return scriptHelper;
    }
    Map<String, Object> ctx = getContext();
    if (ctx == null) {
      ctx = getRawContext();
    }
    return scriptHelper = new CompositeScriptHelper(new ScriptBindings(ctx));
  }

  /**
   * Get the domain object context.
   *
   * @return an instance of {@link Context}
   */
  @JsonIgnore
  public Context getContext() {
    if (context != null) {
      return context;
    }
    final Map<String, Object> vars = getRawContext();
    Class<?> klass;
    try {
      klass = Class.forName(vars.get("_model").toString());
    } catch (Exception e) {
      klass = getBeanClass();
    }
    if (klass == null) {
      return null;
    }
    return context = new Context(vars, klass);
  }
}
