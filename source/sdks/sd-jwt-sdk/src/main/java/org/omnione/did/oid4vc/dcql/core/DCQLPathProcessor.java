/*
 * Copyright 2025 OmniOne.
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

import org.omnione.did.oid4vc.exception.OID4VCException;
import java.util.ArrayList;
import java.util.List;

//TODO: Multi-depth support must be implemented.
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

    } catch (OID4VCException e) {
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
}