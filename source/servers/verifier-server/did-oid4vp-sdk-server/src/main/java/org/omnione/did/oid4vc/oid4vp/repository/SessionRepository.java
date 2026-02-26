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

import org.omnione.did.oid4vc.oid4vp.dto.VerificationSession;

import java.util.Map;
import java.util.Optional;

public interface SessionRepository {

  /**
   * Retrieve a verification session by state
   */
  Optional<VerificationSession> findByState(String state);

  /**
   * Retrieve a verification session by request_id
   */
  Optional<VerificationSession> findByRequestId(String requestId);

  /**
   * Retrieve a verification session by transaction_id
   */
  Optional<VerificationSession> findByTransactionId(String transactionId);

  /**
   * Get all sessions
   */
  Map<String, VerificationSession> findAll();

  /**
   * Save a verification session by state
   */
  void saveByState(String state, VerificationSession session);

  /**
   * Check if session exists by state
   */
  boolean existsByState(String state);

  /**
   * Clear all sessions
   */
  void clear();

  /**
   * Get session count
   */
  int count();
}