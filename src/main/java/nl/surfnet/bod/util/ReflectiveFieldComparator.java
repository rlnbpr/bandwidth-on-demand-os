package nl.surfnet.bod.util;

import java.lang.reflect.InvocationTargetException;
import java.util.Comparator;

public class ReflectiveFieldComparator implements Comparator<Object> {

  private final String getter;

  public ReflectiveFieldComparator(final String field) {
    this.getter = "get" + field.substring(0, 1).toUpperCase() + field.substring(1);
  }

  public int compare(Object one, Object two) {
    try {
      if (one != null && two != null) {
        one = one.getClass().getMethod(getter, new Class[0]).invoke(one, new Object[0]);
      }
    }
    catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
        | SecurityException e) {
      throw new IllegalArgumentException("Cannot compare " + one + " with " + two + ". Does the getter: " + getter
          + " exists?", e);
    }
    return (one == null) ? -1 : ((two == null) ? 1 : ((Comparable<Object>) one).compareTo(two));
  }

}
