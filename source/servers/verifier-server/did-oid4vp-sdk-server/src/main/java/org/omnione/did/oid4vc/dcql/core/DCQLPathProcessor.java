/*
 * Copyright 2026 OmniOne.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.omnione.did.oid4vc.dcql.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.omnione.did.oid4vc.dcql.exception.DCQLException;

public class DCQLPathProcessor {

  public static boolean isIndexBasedPath(List<Object> path) {
    if (path == null || path.isEmpty()) {
      return false;
    }

    return path.size() == 1 && path.get(0) instanceof Integer;
  }

  public static Integer extractIndex(List<Object> path) {
    if (!isIndexBasedPath(path)) {
      return null;
    }
    return (Integer) path.get(0);
  }

  public static String pathToClaimName(List<Object> path) {
    if (path == null || path.isEmpty()) {
      return null;
    }

    try {
      List<String> pathParts = new ArrayList<>();

      for (Object element : path) {
        if (element == null) {
          pathParts.add("*");
        } else if (element instanceof String) {
          pathParts.add((String) element);
        } else if (element instanceof Integer) {
          pathParts.add(String.valueOf(element));
        } else {
          return null;
        }
      }

      String claimName = String.join(".", pathParts);
      return claimName;

    } catch (DCQLException e) {
      throw e;
    }
  }

  public static boolean isValidPath(List<Object> path) {
    if (path == null || path.isEmpty()) {
      return false;
    }

    for (Object element : path) {
      if (element != null && !(element instanceof String) && !(element instanceof Integer)) {
        return false;
      }
    }

    return true;
  }

  public static Object navigatePath(Object root, List<Object> path) {
    if (root == null || path == null || path.isEmpty()) {
      return null;
    }

    return navigatePathRecursive(root, path, 0);
  }

  private static Object navigatePathRecursive(Object current, List<Object> path, int index) {
    if (index >= path.size()) {
      return current;
    }

    Object pathElement = path.get(index);

    if (pathElement == null) {
      if (!(current instanceof List)) {
        return null;
      }

      List<Object> results = new ArrayList<>();
      List<?> currentList = (List<?>) current;

      for (Object item : currentList) {
        Object result = navigatePathRecursive(item, path, index + 1);
        if (result != null) {
          if (result instanceof List) {
            results.addAll((List<?>) result);
          } else {
            results.add(result);
          }
        }
      }

      return results.isEmpty() ? null : results;
    }

    if (pathElement instanceof Integer) {
      if (!(current instanceof List)) {
        return null;
      }

      int idx = (Integer) pathElement;
      List<?> currentList = (List<?>) current;

      if (idx < 0 || idx >= currentList.size()) {
        return null;
      }

      Object element = currentList.get(idx);
      return navigatePathRecursive(element, path, index + 1);
    }

    if (pathElement instanceof String) {
      if (!(current instanceof Map)) {
        return null;
      }

      Map<?, ?> currentMap = (Map<?, ?>) current;
      String key = (String) pathElement;

      Object nextValue = currentMap.get(key);
      if (nextValue == null) {
        return null;
      }

      return navigatePathRecursive(nextValue, path, index + 1);
    }

    return null;
  }
}