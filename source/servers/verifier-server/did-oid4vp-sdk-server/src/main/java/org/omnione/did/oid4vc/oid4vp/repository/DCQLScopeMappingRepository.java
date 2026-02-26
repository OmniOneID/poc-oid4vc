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

import org.omnione.did.oid4vc.oid4vp.dto.DCQLScopeMappingDto;

import java.util.List;
import java.util.Optional;

public interface DCQLScopeMappingRepository {

  /**
   * Find mapping by ID
   */
  Optional<DCQLScopeMappingDto> findById(Long id);

  /**
   * Find mapping by scope value
   */
  Optional<DCQLScopeMappingDto> findByScope(String scope);

  /**
   * Find all enabled mappings
   */
  List<DCQLScopeMappingDto> findAllEnabled();

  /**
   * Find all mappings
   */
  List<DCQLScopeMappingDto> findAll();

  /**
   * Save mapping (insert or update)
   */
  DCQLScopeMappingDto save(DCQLScopeMappingDto dto);

  /**
   * Delete mapping by id
   */
  void deleteById(Long id);

  /**
   * Delete mapping by scope
   */
  void deleteByScope(String scope);

  /**
   * Check if scope exists
   */
  boolean existsByScope(String scope);

  /**
   * Get mapping count
   */
  long count();
}