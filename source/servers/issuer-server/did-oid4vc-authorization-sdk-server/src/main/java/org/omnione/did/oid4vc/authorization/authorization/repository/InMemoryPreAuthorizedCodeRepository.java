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

package org.omnione.did.oid4vc.authorization.authorization.repository;

import org.omnione.did.oid4vc.authorization.authorization.oid4vci.preauthorized.dto.PreAuthorizedCode;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryPreAuthorizedCodeRepository implements PreAuthorizedCodeRepository {

    private final ConcurrentHashMap<String, PreAuthorizedCode> codes = new ConcurrentHashMap<>();

    public void save(PreAuthorizedCode preAuthorizedCode) {
        codes.put(preAuthorizedCode.getValue(), preAuthorizedCode);
    }

    public Optional<PreAuthorizedCode> findByValue(String code) {
        PreAuthorizedCode preAuthorizedCode = codes.get(code);
        if (preAuthorizedCode != null && preAuthorizedCode.isExpired()) {
            codes.remove(code);
            return Optional.empty();
        }
        return Optional.ofNullable(preAuthorizedCode);
    }

    public Optional<PreAuthorizedCode> remove(String code) {
        return Optional.ofNullable(codes.remove(code));
    }
}
