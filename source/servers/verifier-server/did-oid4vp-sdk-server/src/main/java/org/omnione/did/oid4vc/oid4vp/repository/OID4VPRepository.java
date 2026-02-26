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

package org.omnione.did.oid4vc.oid4vp.repository;

import org.omnione.did.oid4vc.oid4vp.dto.OID4VPConfigDto;

import java.util.List;
import java.util.Optional;

public interface OID4VPRepository {

  /**
   * Find OID4VP configuration by type
   */
  Optional<OID4VPConfigDto> findByType(String type);

  /**
   * Find OID4VP configuration by id
   */
  Optional<OID4VPConfigDto> findById(Long id);

  /**
   * Get all OID4VP configurations
   */
  List<OID4VPConfigDto> findAll();

  /**
   * Save OID4VP configuration
   */
  OID4VPConfigDto save(OID4VPConfigDto dto);

  /**
   * Delete OID4VP configuration by id
   */
  void deleteById(Long id);

  /**
   * Check if configuration exists by type
   */
  boolean existsByType(String type);

  /**
   * Get configuration count
   */
  long count();
}