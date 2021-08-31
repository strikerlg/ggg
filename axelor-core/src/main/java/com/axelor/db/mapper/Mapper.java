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
package com.axelor.db.mapper;

import com.axelor.common.ResourceUtils;
import com.axelor.db.annotations.NameColumn;
import com.axelor.db.annotations.Sequence;
import com.axelor.internal.asm.ClassReader;
import com.axelor.internal.asm.Opcodes;
import com.axelor.internal.asm.tree.ClassNode;
import com.axelor.internal.asm.tree.FieldInsnNode;
import com.axelor.internal.asm.tree.MethodNode;
import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * This class can be used to map params to Java bean using reflection. It also provides convenient
 * methods to get/set values to a bean instance.
 */
public class Mapper {

  private static final LoadingCache<Class<?>, Mapper> MAPPER_CACHE =
      CacheBuilder.newBuilder().maximumSize(1000).weakKeys().build(CacheLoader.from(Mapper::new));

  private static final Cache<Method, Annotation[]> ANNOTATION_CACHE =
      CacheBuilder.newBuilder().maximumSize(1000).weakKeys().build();

  private static final Object[] NULL_ARGUMENTS = {};

  private static final String PREFIX_COMPUTE = "compute";
  private static final String PREFIX_SET = "set";

  private Map<String, Method> getters = new HashMap<>(); // field -> getter
  private Map<String, Method> setters = new HashMap<>(); // field -> setter
  private Map<String, String> methods = new HashMap<>(); // getter/setter/compute -> field

  private Map<String, Class<?>> types = new HashMap<>();
  private Map<String, Property> fields = new HashMap<>();

  private Map<String, Set<String>> computeDependencies;

  private Set<Property> sequenceFields = new HashSet<>();

  private Property nameField;

  private Class<?> beanClass;

  private Mapper(Class<?> beanClass) {
    Preconditions.checkNotNull(beanClass);
    this.beanClass = beanClass;
    try {
      BeanInfo info = Introspector.getBeanInfo(beanClass, Object.class);
      for (PropertyDescriptor descriptor : info.getPropertyDescriptors()) {
        String name = descriptor.getName();
        Method getter = descriptor.getReadMethod();
        Method setter = descriptor.getWriteMethod();
        Class<?> type = descriptor.getPropertyType();

        if (getter != null) {
          getters.put(name, getter);
          methods.put(getter.getName(), name);
          try {
            Property property =
                new Property(
                    beanClass,
                    name,
                    type,
                    getter.getGenericReturnType(),
                    getAnnotations(name, getter));
            fields.put(name, property);
            if (property.isSequence()) {
              sequenceFields.add(property);
            }
            if (property.isVirtual()) {
              final Method compute =
                  getMethod(
                      beanClass,
                      PREFIX_COMPUTE + name.substring(0, 1).toUpperCase() + name.substring(1));
              if (compute != null) {
                methods.put(compute.getName(), name);
              }
            }
          } catch (Exception e) {
            continue;
          }
        }
        if (setter == null) {
          setter =
              getMethod(
                  beanClass,
                  PREFIX_SET + name.substring(0, 1).toUpperCase() + name.substring(1),
                  type);
        }
        if (setter != null) {
          setter.setAccessible(true);
          setters.put(name, setter);
          methods.put(setter.getName(), name);
        }
        types.put(name, type);
      }
    } catch (IntrospectionException e) {
    }
  }

  private Annotation[] getAnnotations(String name, Method method) {
    Annotation[] found = ANNOTATION_CACHE.getIfPresent(method);
    if (found != null) {
      return found;
    }

    final List<Annotation> all = new ArrayList<>();
    try {
      final Field field = getField(beanClass, name);
      for (Annotation a : field.getAnnotations()) {
        all.add(a);
      }
    } catch (Exception e) {
    }

    for (Annotation a : method.getAnnotations()) {
      all.add(a);
    }

    found = all.toArray(new Annotation[] {});
    ANNOTATION_CACHE.put(method, found);

    return found;
  }

  private Field getField(Class<?> klass, String name) {
    if (klass == null) return null;
    try {
      return klass.getDeclaredField(name);
    } catch (NoSuchFieldException e) {
      return getField(klass.getSuperclass(), name);
    }
  }

  private Method getMethod(Class<?> klass, String name, Class<?>... parameterTypes) {
    if (klass == null) return null;
    try {
      return klass.getDeclaredMethod(name, parameterTypes);
    } catch (NoSuchMethodException e) {
      return getMethod(klass.getSuperclass(), name, parameterTypes);
    }
  }

  /**
   * Create a {@link Mapper} for the given Java Bean class by introspecting all it's properties.
   *
   * <p>If the {@link Mapper} class has been previously created for the given class, then the {@link
   * Mapper} class is retrieved from the cache.
   *
   * @param klass the bean class
   * @return an instance of {@link Mapper} for the given class.
   */
  public static Mapper of(Class<?> klass) {
    try {
      return MAPPER_CACHE.get(klass);
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Get all the properties.
   *
   * @return an array of {@link Property}
   */
  public Property[] getProperties() {
    return fields.values().toArray(new Property[] {});
  }

  /**
   * Get the {@link Property} of the given name.
   *
   * @param name name of the property
   * @return a Property or null if property doesn't exist.
   */
  public Property getProperty(String name) {
    return fields.get(name);
  }

  /**
   * Get {@link Property} by it's getter, setter or compute method.
   *
   * @param method the getter, setter or compute method
   * @return the property associated with the method
   */
  public Property getProperty(Method method) {
    Preconditions.checkNotNull(method);
    return getProperty(methods.get(method.getName()));
  }

  /**
   * Get the property of the name field.
   *
   * <p>A name field annotated with {@link NameColumn} or a field with name <code>name</code> is
   * considered name field.
   *
   * @return a property
   */
  public Property getNameField() {
    if (nameField != null) {
      return nameField;
    }
    for (Property property : fields.values()) {
      if (property.isNameColumn()) {
        return nameField = property;
      }
    }
    return nameField = getProperty("name");
  }

  /**
   * Get all the {@link Sequence} fields.
   *
   * @return copy of the original set of fields.
   */
  public Property[] getSequenceFields() {
    return sequenceFields.toArray(new Property[] {});
  }

  /**
   * Find the fields directly accessed by the compute method of the given computed property.
   *
   * @param property the computed property
   * @return set of fields accessed by computed property
   */
  public Set<String> getComputeDependencies(Property property) {
    Preconditions.checkNotNull(property);
    if (computeDependencies == null) {
      computeDependencies = findComputeDependencies();
    }
    return computeDependencies.computeIfAbsent(
        property.getName(),
        key ->
            Optional.ofNullable(beanClass.getSuperclass())
                .map(Mapper::of)
                .map(mapper -> mapper.getComputeDependencies(property))
                .orElse(Collections.emptySet()));
  }

  private Map<String, Set<String>> findComputeDependencies() {
    final String className = beanClass.getName().replace('.', '/');
    final ClassReader reader;
    try {
      reader = new ClassReader(ResourceUtils.getResourceStream(className + ".class"));
    } catch (IOException e) {
      return new HashMap<>();
    }

    final ClassNode node = new ClassNode();
    reader.accept(node, 0);

    return ((List<?>) node.methods)
        .stream()
            .map(m -> (MethodNode) m)
            .filter(m -> Modifier.isProtected(m.access))
            .filter(m -> m.name.startsWith(PREFIX_COMPUTE))
            .filter(m -> methods.containsKey(m.name))
            .collect(
                Collectors.toMap(
                    m -> methods.get(m.name),
                    m -> {
                      return Arrays.stream(m.instructions.toArray())
                          .filter(n -> n.getOpcode() == Opcodes.GETFIELD)
                          .filter(n -> n instanceof FieldInsnNode)
                          .map(n -> (FieldInsnNode) n)
                          .filter(n -> !n.name.equals(methods.get(m.name)))
                          .map(n -> n.name)
                          .collect(Collectors.toSet());
                    }));
  }

  /**
   * Get the bean class this mapper operates on.
   *
   * @return the bean class
   */
  public Class<?> getBeanClass() {
    return beanClass;
  }

  /**
   * Get the getter method of the given property.
   *
   * @param name name of the property
   * @return getter method or null if property is write-only
   */
  public Method getGetter(String name) {
    return getters.get(name);
  }

  /**
   * Get the setter method of the given property.
   *
   * @param name name of the property
   * @return setter method or null if property is read-only
   */
  public Method getSetter(String name) {
    return setters.get(name);
  }

  /**
   * Get the value of given property from the given bean. It returns <code>null</code> if property
   * doesn't exist.
   *
   * @param bean the bean
   * @param name name of the property
   * @return property value
   */
  public Object get(Object bean, String name) {
    Preconditions.checkNotNull(bean);
    Preconditions.checkNotNull(name);
    Preconditions.checkArgument(beanClass.isInstance(bean));
    Preconditions.checkArgument(!name.trim().equals(""));
    try {
      return getters.get((String) name).invoke(bean, NULL_ARGUMENTS);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Set the property of the given bean with the provided value.
   *
   * @param bean the bean
   * @param name name of the property
   * @param value value for the property
   * @return old value of the property
   */
  public Object set(Object bean, String name, Object value) {
    Preconditions.checkNotNull(bean);
    Preconditions.checkNotNull(name);
    Preconditions.checkArgument(beanClass.isInstance(bean));
    Preconditions.checkArgument(!name.trim().equals(""));

    final Method method = setters.get(name);
    if (method == null) {
      throw new IllegalArgumentException(
          "The bean of type: " + beanClass.getName() + " has no property called: " + name);
    }

    final Object oldValue = get(bean, name);
    final Class<?> actualType = method.getParameterTypes()[0];
    final Type genericType = method.getGenericParameterTypes()[0];
    final Annotation[] annotations = getAnnotations(name, method);
    try {
      method.invoke(bean, Adapter.adapt(value, actualType, genericType, annotations));
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
    return oldValue;
  }

  /**
   * Create an object of the given class mapping the given value map to it's properties.
   *
   * @param <T> type of the bean
   * @param klass class of the bean
   * @param values value map
   * @return an instance of the given class
   */
  public static <T> T toBean(Class<T> klass, Map<String, Object> values) {
    final T bean;
    try {
      bean = klass.newInstance();
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
    if (values == null || values.isEmpty()) {
      return bean;
    }
    final Mapper mapper = Mapper.of(klass);
    values.entrySet().stream()
        .filter(e -> mapper.setters.containsKey(e.getKey()))
        .forEach(e -> mapper.set(bean, e.getKey(), e.getValue()));
    return bean;
  }

  /**
   * Create a map from the given bean instance with property names are keys and their respective
   * values are map values.
   *
   * @param bean a bean instance
   * @return a map
   */
  public static Map<String, Object> toMap(Object bean) {
    if (bean == null) {
      return null;
    }
    final Map<String, Object> map = new HashMap<>();
    final Mapper mapper = Mapper.of(bean.getClass());
    for (Property p : mapper.getProperties()) {
      map.put(p.getName(), p.get(bean));
    }
    return map;
  }
}
