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

import com.axelor.auth.db.AuditableModel;
import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.meta.db.MetaJsonRecord;
import com.google.common.collect.Collections2;
import com.google.common.primitives.Longs;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ContextHandler} provides seamless way to access context values using proxy.
 *
 * <p>The proxy and it's fields are initialized lazily from the context value map when context
 * variable is access. Any missing value of the bean is accessed from the managed instance.
 *
 * <p>For internal use only.
 *
 * @see Context
 */
public class ContextHandler<T> {

  private static final String FIELD_ID = "id";
  private static final String FIELD_VERSION = "version";
  private static final String FIELD_SELECTED = "selected";

  private static final Logger log = LoggerFactory.getLogger(ContextHandler.class);

  private final PropertyChangeSupport changeListeners;

  private final Map<String, Object> values;
  private final Set<String> validated;

  private final Class<T> beanClass;
  private final Mapper beanMapper;

  private T managedEntity;
  private T unmanagedEntity;
  private T proxy;

  private JsonContext jsonContext;

  private boolean searched;

  ContextHandler(Class<T> beanClass, Map<String, Object> values) {
    this.values = Objects.requireNonNull(values);
    this.validated = new HashSet<>();
    this.beanClass = Objects.requireNonNull(beanClass);
    this.beanMapper = Mapper.of(beanClass);
    this.changeListeners = new PropertyChangeSupport(this);
  }

  public void addChangeListener(PropertyChangeListener listener) {
    changeListeners.addPropertyChangeListener(listener);
  }

  private Long findId(Map<String, Object> values) {
    try {
      return Long.parseLong(values.get(FIELD_ID).toString());
    } catch (Exception e) {
      return null;
    }
  }

  private T getManagedEntity() {
    if (searched) {
      return managedEntity;
    }
    final Long id = findId(values);
    if (id != null) {
      managedEntity = JPA.em().find(beanClass, id);
    }
    searched = true;
    return managedEntity;
  }

  private T getUnmanagedEntity() {
    if (unmanagedEntity == null) {
      unmanagedEntity = Mapper.toBean(beanClass, null);
    }
    return unmanagedEntity;
  }

  public T getProxy() {
    return proxy;
  }

  void setProxy(T proxy) {
    this.proxy = proxy;
  }

  private JsonContext getJsonContext() {
    if (jsonContext == null) {
      jsonContext = createJsonContext();
    }
    return jsonContext;
  }

  private JsonContext createJsonContext() {
    if (MetaJsonRecord.class.isAssignableFrom(beanClass)) {
      final MetaJsonRecord rec = (MetaJsonRecord) proxy;
      return new JsonContext(rec);
    }
    final Property p = beanMapper.getProperty(Context.KEY_JSON_ATTRS);
    final Context c = new Context(beanClass);
    return new JsonContext(c, p, (String) p.get(proxy));
  }

  @SuppressWarnings("unchecked")
  private Object createOrFind(Property property, Object item) {
    if (item == null || item instanceof Model) {
      return item;
    }
    if (item instanceof Map) {
      final Map<String, Object> map = (Map<String, Object>) item;
      final Long id = findId(map);
      // if new or updated, create proxy
      if (id == null || id <= 0 || map.containsKey(FIELD_VERSION)) {
        return ContextHandlerFactory.newHandler(property.getTarget(), map).getProxy();
      }
      // use managed instance
      final Object bean = JPA.em().find(property.getTarget(), id);
      if (map.containsKey(FIELD_SELECTED)) {
        Mapper.of(property.getTarget()).set(bean, FIELD_SELECTED, map.get(FIELD_SELECTED));
      }
      return bean;
    }
    if (item instanceof Number) {
      return JPA.em().find(property.getTarget(), item);
    }
    throw new IllegalArgumentException("Invalid collection item for field: " + property.getName());
  }

  Object validate(Property property, Object value) {
    if (property == null) {
      return value;
    }
    if (property.isCollection() && value instanceof Collection) {
      value =
          ((Collection<?>) value)
              .stream().map(item -> createOrFind(property, item)).collect(Collectors.toList());
    } else if (property.isReference()) {
      value = createOrFind(property, value);
    }
    return value;
  }

  private void validate(Property property) {
    if (property == null
        || validated.contains(property.getName())
        || !values.containsKey(property.getName())) {
      return;
    }

    final Object value = validate(property, values.get(property.getName()));
    final Object bean = getUnmanagedEntity();

    Mapper mapper = beanMapper;
    if (mapper.getSetter(property.getName()) == null && bean instanceof AuditableModel) {
      mapper = Mapper.of(AuditableModel.class);
    }

    // prevent automatic association handling
    // causing detached entity exception
    mapper.set(bean, property.getName(), value);

    validated.add(property.getName());
  }

  private Object interceptComputeAccess(Callable<?> superCall, Method method, Object[] args)
      throws Exception {
    final Property computed = beanMapper.getProperty(method);
    final Set<String> depends;
    if (computed == null
        || (depends = beanMapper.getComputeDependencies(computed)) == null
        || depends.isEmpty()) {
      return superCall.call();
    }

    for (String name : depends) {
      final Property property;
      final Object managed;
      if (validated.contains(name) || (property = beanMapper.getProperty(name)) == null) {
        continue;
      }
      if (values.containsKey(name)) {
        validate(property);
      } else if ((managed = getManagedEntity()) != null) {
        beanMapper.set(getUnmanagedEntity(), name, property.get(managed));
      }
    }

    method.setAccessible(true);
    return method.invoke(getUnmanagedEntity(), args);
  }

  public Object interceptJsonAccess(Method method, Object[] args) throws Exception {
    final String name = (String) args[0];
    if ("class".equals(name)) return method.invoke(proxy, args);
    if ("get".equals(method.getName()) || "put".equals(method.getName())) {
      Method accessor = args.length == 1 ? beanMapper.getGetter(name) : beanMapper.getSetter(name);
      Object[] params = args.length == 1 ? new Object[] {} : new Object[] {args[1]};
      if (accessor != null) {
        return accessor.invoke(proxy, params);
      }
      JsonContext ctx = getJsonContext();
      if (args.length == 1) {
        if (ctx.containsKey(name) || ctx.hasField(name)) return ctx.get(name);
        if (name.startsWith("_") || name.startsWith("$")) {
          return null;
        }
        // TODO: to easy the migration, we log the error as warning but ultimately,
        //       we should throw NoSuchFieldException here
        log.warn("No such field: {}.{}", beanClass.getName(), name);
        return null;
      }
      return ctx.put(name, args[1]);
    }
    if ("containsKey".equals(method.getName())) {
      return beanMapper.getGetter(name) != null
          || getJsonContext().containsKey(name)
          || getJsonContext().hasField(name);
    }
    throw new UnsupportedOperationException("cannot call '" + method + "' on proxy object");
  }

  @RuntimeType
  public Object intercept(
      @SuperCall Callable<?> superCall, @Origin Method method, @AllArguments Object[] args)
      throws Throwable {

    // if map access (for json values)
    if (superCall == null && method.getDeclaringClass() == Map.class) {
      return interceptJsonAccess(method, args);
    }

    // handle compute method calls
    if (Modifier.isProtected(method.getModifiers())) {
      return interceptComputeAccess(superCall, method, args);
    }

    final Property property = beanMapper.getProperty(method);
    // no fields defined or is computed field
    if (property == null || property.isVirtual()) {
      return superCall.call();
    }

    final String fieldName = property.getName();
    final Object unmanaged = getUnmanagedEntity();

    // in case of setter, update context map
    final Object oldValue = args.length == 1 ? values.put(fieldName, args[0]) : null;

    // if setter or value found in context map for the getter
    if (args.length == 1 || values.containsKey(fieldName) || property.isTransient()) {
      validate(property);
      try {
        return method.invoke(unmanaged, args);
      } finally {
        if (args.length == 1 && changeListeners.hasListeners(fieldName)) {
          changeListeners.firePropertyChange(fieldName, oldValue, values.get(fieldName));
        }
      }
    }
    // else get value from managed instance
    final Object managed = getManagedEntity();

    // if managed entity not found, get default value from unmanaged entity
    if (managed == null) {
      return method.invoke(unmanaged, args);
    }

    return method.invoke(managed, args);
  }

  @RuntimeType
  public Map<String, Object> getContextMap() {
    final Map<String, Object> data = new HashMap<>();
    final Object bean = getContextEntity();

    final Function<Object, Object> transform =
        (item) -> {
          if (item instanceof ContextEntity) {
            return ((ContextEntity) item).getContextMap();
          }
          if (item instanceof Model) {
            Model m = (Model) item;
            return m.getId() == null ? Resource.toMap(m) : Resource.toMapCompact(m);
          }
          return item;
        };

    // get context data only
    Arrays.stream(beanMapper.getProperties())
        .map(Property::getName)
        .filter(validated::contains)
        .forEach(
            name -> {
              Object value = beanMapper.get(bean, name);
              if (value instanceof Collection) {
                value = Collections2.transform((Collection<?>) value, transform::apply);
              } else {
                value = transform.apply(value);
              }
              data.put(name, value);
            });

    return data;
  }

  @RuntimeType
  public Long getContextId() {
    return Longs.tryParse(values.getOrDefault("id", "").toString());
  }

  @RuntimeType
  public Object getContextEntity() {
    final Object bean = getUnmanagedEntity();
    final Object managed = getManagedEntity();

    // populate from context values
    Arrays.stream(beanMapper.getProperties()).forEach(this::validate);

    if (managed == null) {
      return bean;
    }

    // populate dependent fields of computed property
    Arrays.stream(beanMapper.getProperties())
        .filter(Property::isVirtual)
        .flatMap(p -> beanMapper.getComputeDependencies(p).stream())
        .filter(n -> !validated.contains(n))
        .distinct()
        .forEach(n -> beanMapper.set(bean, n, beanMapper.get(managed, n)));

    // make sure to have version value
    if (bean instanceof Model && !values.containsKey(FIELD_VERSION)) {
      ((Model) bean).setVersion(((Model) managed).getVersion());
    }

    return bean;
  }
}
