/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.util;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Condition;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ReflectionUtil {
  private static final Logger LOG = Logger.getInstance("#com.intellij.util.ReflectionUtil");

  private ReflectionUtil() { }

  @Nullable
  public static Type resolveVariable(@NotNull TypeVariable variable, @NotNull Class classType) {
    return resolveVariable(variable, classType, true);
  }

  @Nullable
  public static Type resolveVariable(@NotNull TypeVariable variable, @NotNull Class classType, boolean resolveInInterfacesOnly) {
    final Class aClass = getRawType(classType);
    int index = ArrayUtilRt.find(aClass.getTypeParameters(), variable);
    if (index >= 0) {
      return variable;
    }

    final Class[] classes = aClass.getInterfaces();
    final Type[] genericInterfaces = aClass.getGenericInterfaces();
    for (int i = 0; i <= classes.length; i++) {
      Class anInterface;
      if (i < classes.length) {
        anInterface = classes[i];
      }
      else {
        anInterface = aClass.getSuperclass();
        if (resolveInInterfacesOnly || anInterface == null) {
          continue;
        }
      }
      final Type resolved = resolveVariable(variable, anInterface);
      if (resolved instanceof Class || resolved instanceof ParameterizedType) {
        return resolved;
      }
      if (resolved instanceof TypeVariable) {
        final TypeVariable typeVariable = (TypeVariable)resolved;
        index = ArrayUtilRt.find(anInterface.getTypeParameters(), typeVariable);
        if (index < 0) {
          LOG.error("Cannot resolve type variable:\n" + "typeVariable = " + typeVariable + "\n" + "genericDeclaration = " +
                    declarationToString(typeVariable.getGenericDeclaration()) + "\n" + "searching in " + declarationToString(anInterface));
        }
        final Type type = i < genericInterfaces.length ? genericInterfaces[i] : aClass.getGenericSuperclass();
        if (type instanceof Class) {
          return Object.class;
        }
        if (type instanceof ParameterizedType) {
          return getActualTypeArguments((ParameterizedType)type)[index];
        }
        throw new AssertionError("Invalid type: " + type);
      }
    }
    return null;
  }

  @NotNull
  public static String declarationToString(@NotNull GenericDeclaration anInterface) {
    return anInterface.toString() + Arrays.asList(anInterface.getTypeParameters()) + " loaded by " + ((Class)anInterface).getClassLoader();
  }

  @NotNull
  public static Class<?> getRawType(@NotNull Type type) {
    if (type instanceof Class) {
      return (Class)type;
    }
    if (type instanceof ParameterizedType) {
      return getRawType(((ParameterizedType)type).getRawType());
    }
    if (type instanceof GenericArrayType) {
      //todo[peter] don't create new instance each time
      return Array.newInstance(getRawType(((GenericArrayType)type).getGenericComponentType()), 0).getClass();
    }
    assert false : type;
    return null;
  }

  @NotNull
  public static Type[] getActualTypeArguments(@NotNull ParameterizedType parameterizedType) {
    return parameterizedType.getActualTypeArguments();
  }

  @NotNull
  public static Field findField(@NotNull Class clazz, @Nullable final Class type, @NotNull final String name) throws NoSuchFieldException {
    Field result = processFields(clazz, new Condition<Field>() {
      @Override
      public boolean value(Field field) {
        return name.equals(field.getName()) && (type == null || field.getType().equals(type));
      }
    });
    if (result != null) return result;

    throw new NoSuchFieldException("Class: " + clazz + " name: " + name + " type: " + type);
  }

  @NotNull
  public static Field findAssignableField(@NotNull Class<?> clazz, @Nullable("null means any type") final Class<?> fieldType, @NotNull final String fieldName) throws NoSuchFieldException {
    Field result = processFields(clazz, new Condition<Field>() {
      @Override
      public boolean value(Field field) {
        return fieldName.equals(field.getName()) && (fieldType == null || fieldType.isAssignableFrom(field.getType()));
      }
    });
    if (result != null) return result;
    throw new NoSuchFieldException("Class: " + clazz + " fieldName: " + fieldName + " fieldType: " + fieldType);
  }

  private static Field processFields(@NotNull Class clazz, @NotNull Condition<Field> checker) {
    for (Field field : clazz.getDeclaredFields()) {
      if (checker.value(field)) {
        field.setAccessible(true);
        return field;
      }
    }
    final Class superClass = clazz.getSuperclass();
    if (superClass != null) {
      Field result = processFields(superClass, checker);
      if (result != null) return result;
    }
    final Class[] interfaces = clazz.getInterfaces();
    for (Class each : interfaces) {
      Field result = processFields(each, checker);
      if (result != null) return result;
    }
    return null;
  }

  @Nullable
  public static Method findMethod(@NotNull Collection<Method> methods, @NonNls @NotNull String name, @NotNull Class... parameters) {
    for (final Method method : methods) {
      if (name.equals(method.getName()) && Arrays.equals(parameters, method.getParameterTypes())) {
        method.setAccessible(true);
        return method;
      }
    }
    return null;
  }

  @Nullable
  public static Method getMethod(@NotNull Class aClass, @NonNls @NotNull String name, @NotNull Class... parameters) {
    return findMethod(getClassPublicMethods(aClass, false), name, parameters);
  }

  @Nullable
  public static Method getDeclaredMethod(@NotNull Class aClass, @NonNls @NotNull String name, @NotNull Class... parameters) {
    return findMethod(getClassDeclaredMethods(aClass, false), name, parameters);
  }

  @Nullable
  public static Field getDeclaredField(@NotNull Class aClass, @NonNls @NotNull final String name) {
    return processFields(aClass, new Condition<Field>() {
      @Override
      public boolean value(Field field) {
        return name.equals(field.getName());
      }
    });
  }

  @NotNull
  public static List<Method> getClassPublicMethods(@NotNull Class aClass, boolean includeSynthetic) {
    Method[] methods = aClass.getMethods();
    return includeSynthetic ? Arrays.asList(methods) : filterRealMethods(methods);
  }

  @NotNull
  public static List<Method> getClassDeclaredMethods(@NotNull Class aClass, boolean includeSynthetic) {
    Method[] methods = aClass.getDeclaredMethods();
    return includeSynthetic ? Arrays.asList(methods) : filterRealMethods(methods);
  }

  @NotNull
  private static List<Method> filterRealMethods(@NotNull Method[] methods) {
    List<Method> result = ContainerUtil.newArrayList();
    for (Method method : methods) {
      if (!method.isSynthetic()) {
        result.add(method);
      }
    }
    return result;
  }

  @Nullable
  public static Class getMethodDeclaringClass(@NotNull Class<?> instanceClass, @NonNls @NotNull String methodName, @NotNull Class... parameters) {
    Method method = getMethod(instanceClass, methodName, parameters);
    return method == null ? null : method.getDeclaringClass();
  }

  public static <T> T getField(@NotNull Class objectClass, @Nullable Object object, @Nullable("null means any type") Class<T> fieldType, @NotNull @NonNls String fieldName) {
    try {
      final Field field = findAssignableField(objectClass, fieldType, fieldName);
      @SuppressWarnings("unchecked") T t = (T)field.get(object);
      return t;
    }
    catch (NoSuchFieldException e) {
      LOG.debug(e);
      return null;
    }
    catch (IllegalAccessException e) {
      LOG.debug(e);
      return null;
    }
  }

  public static <T> T getStaticFieldValue(@NotNull Class objectClass, @Nullable("null means any type") Class<T> fieldType, @NotNull @NonNls String fieldName) {
    try {
      final Field field = findAssignableField(objectClass, fieldType, fieldName);
      if (!Modifier.isStatic(field.getModifiers())) {
        throw new IllegalArgumentException("Field " + objectClass + "." + fieldName + " is not static");
      }
      @SuppressWarnings("unchecked") T t = (T)field.get(null);
      return t;
    }
    catch (NoSuchFieldException e) {
      LOG.debug(e);
      return null;
    }
    catch (IllegalAccessException e) {
      LOG.debug(e);
      return null;
    }
  }

  @NotNull
  public static <T> Constructor<T> getDefaultConstructor(@NotNull Class<T> aClass) {
    try {
      final Constructor<T> constructor = aClass.getConstructor();
      constructor.setAccessible(true);
      return constructor;
    }
    catch (NoSuchMethodException e) {
      throw new RuntimeException("No default constructor in " + aClass, e);
    }
  }

  /**
   * Like { Class#newInstance()} but also handles private classes
   */
  @NotNull
  public static <T> T newInstance(@NotNull Class<T> aClass) {
    try {
      Constructor<T> constructor = aClass.getDeclaredConstructor();
      try {
        constructor.setAccessible(true);
      }
      catch (SecurityException e) {
        return aClass.newInstance();
      }
      return constructor.newInstance();
    }
    catch (Exception e) {
      // support Kotlin data classes - pass null as default value
      for (Annotation annotation : aClass.getAnnotations()) {
        String name = annotation.annotationType().getName();
        if (name.equals("kotlin.Metadata") || name.equals("kotlin.jvm.internal.KotlinClass")) {
          Constructor<?>[] constructors = aClass.getDeclaredConstructors();
          Exception exception = e;
          ctorLoop:
          for (Constructor<?> constructor1 : constructors) {
            try {
              try {
                constructor1.setAccessible(true);
              }
              catch (Throwable ignored) { }

              Class<?>[] parameterTypes = constructor1.getParameterTypes();
              for (Class<?> type : parameterTypes) {
                if (type.getName().equals("kotlin.jvm.internal.DefaultConstructorMarker")) {
                  continue ctorLoop;
                }
              }

              @SuppressWarnings("unchecked")
              T t = (T)constructor1.newInstance(new Object[parameterTypes.length]);
              return t;
            }
            catch (Exception e1) {
              exception = e1;
            }
          }
          throw new RuntimeException(exception);
        }
      }

      throw new RuntimeException(e);
    }
  }

  @NotNull
  public static <T> T createInstance(@NotNull Constructor<T> constructor, @NotNull Object... args) {
    try {
      return constructor.newInstance(args);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @NotNull
  public static Class forName(@NotNull String fqn) {
    try {
      return Class.forName(fqn);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static boolean isAssignable(@NotNull Class<?> ancestor, @NotNull Class<?> descendant) {
    return ancestor == descendant || ancestor.isAssignableFrom(descendant);
  }
}