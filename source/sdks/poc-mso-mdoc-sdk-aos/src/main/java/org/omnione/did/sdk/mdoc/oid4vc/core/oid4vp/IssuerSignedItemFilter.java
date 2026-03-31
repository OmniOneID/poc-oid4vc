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

package org.omnione.did.sdk.mdoc.oid4vc.core.oid4vp;

import android.util.Log;
import com.upokecenter.cbor.CBORObject;
import com.upokecenter.cbor.CBORType;
import java.util.Map;
import java.util.Set;
import org.omnione.did.sdk.mdoc.oid4vc.constant.MdocConstants;

/**
 * Filters IssuerSignedItems within nameSpaces based on requested claims for selective disclosure.
 *
 * <p>Used by {@link OID4VPHandler} to create VP Tokens containing only the claims
 * requested by the verifier. Supports filtering by namespace and element identifier,
 * preserving CBOR tag 24 wrapping on individual items.</p>
 *
 * @see OID4VPHandler
 */
public class IssuerSignedItemFilter {

  private static final String TAG = IssuerSignedItemFilter.class.getSimpleName();

  /**
   * Filters nameSpaces to include only requested claims.
   *
   * <p>Filtering behavior:</p>
   * <ul>
   *   <li>null or empty requestedClaims: returns all nameSpaces unchanged</li>
   *   <li>Namespace not in requestedClaims: namespace is excluded</li>
   *   <li>Empty set for a namespace: all elements in that namespace are included</li>
   *   <li>Non-empty set: only matching elementIdentifiers are included</li>
   * </ul>
   *
   * @param nameSpaces      CBOR map of namespace -> array of IssuerSignedItems
   * @param requestedClaims map of namespace -> set of requested element identifiers;
   *                        null or empty to include all
   * @return filtered CBOR map of nameSpaces
   */
  public static CBORObject filterNameSpaces(CBORObject nameSpaces,
      Map<String, Set<String>> requestedClaims) {
    if (nameSpaces == null || nameSpaces.getType() != CBORType.Map) {
      return CBORObject.NewMap();
    }

    // No filter provided, return all
    if (requestedClaims == null || requestedClaims.isEmpty()) {
      return nameSpaces;
    }

    CBORObject filtered = CBORObject.NewMap();

    for (CBORObject nsKey : nameSpaces.getKeys()) {
      String nameSpace = nsKey.AsString();
      CBORObject items = nameSpaces.get(nsKey);

      if (items == null || items.getType() != CBORType.Array) {
        continue;
      }

      Set<String> requestedElements = requestedClaims.get(nameSpace);

      // Skip namespaces not present in requestedClaims
      if (requestedElements == null) {
        continue;
      }

      CBORObject filteredItems = CBORObject.NewArray();

      for (int i = 0; i < items.size(); i++) {
        CBORObject itemWrapped = items.get(i);

        // Unwrap tag 24 to access elementIdentifier
        byte[] innerBytes;
        if (itemWrapped.isTagged()
            && itemWrapped.getMostInnerTag().ToInt32Checked() == 24) {
          innerBytes = itemWrapped.GetByteString();
        } else {
          innerBytes = itemWrapped.EncodeToBytes();
        }

        CBORObject item = CBORObject.DecodeFromBytes(innerBytes);
        CBORObject elementIdObj = item.get(MdocConstants.IssuerSignedItem.ELEMENT_IDENTIFIER);
        if (elementIdObj == null) {
          continue;
        }

        String elementId = elementIdObj.AsString();

        // Empty set means include all elements in this namespace
        if (requestedElements.isEmpty() || requestedElements.contains(elementId)) {
          filteredItems.Add(itemWrapped);
        }
      }

      if (filteredItems.size() > 0) {
        filtered.Add(nsKey, filteredItems);
      }
    }

    Log.d(TAG, "Filtered nameSpaces: " + filtered.getKeys().size() + " namespace(s) with items");
    return filtered;
  }
}
