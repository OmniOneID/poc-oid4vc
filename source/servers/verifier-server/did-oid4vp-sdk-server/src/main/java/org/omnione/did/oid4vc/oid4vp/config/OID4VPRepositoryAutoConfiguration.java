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

package org.omnione.did.oid4vc.oid4vp.config;

import lombok.extern.slf4j.Slf4j;
import org.omnione.did.oid4vc.oid4vp.repository.DCQLScopeMappingRepository;
import org.omnione.did.oid4vc.oid4vp.repository.OID4VPRepository;
import org.omnione.did.oid4vc.oid4vp.repository.SessionRepository;
import org.omnione.did.oid4vc.oid4vp.repository.impl.InMemoryDCQLScopeMappingRepository;
import org.omnione.did.oid4vc.oid4vp.repository.impl.InMemoryOID4VPRepository;
import org.omnione.did.oid4vc.oid4vp.repository.impl.InMemorySessionRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for OID4VP repositories.
 * Provides in-memory implementations as defaults when no other implementations are provided.
 */
@Slf4j
@Configuration
public class OID4VPRepositoryAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(OID4VPRepository.class)
  public OID4VPRepository inMemoryOID4VPRepository() {
    log.info("No OID4VPRepository bean found. Using InMemoryOID4VPRepository as default.");
    return new InMemoryOID4VPRepository();
  }

  @Bean
  @ConditionalOnMissingBean(SessionRepository.class)
  public SessionRepository inMemorySessionRepository() {
    log.info("No SessionRepository bean found. Using InMemorySessionRepository as default.");
    return new InMemorySessionRepository();
  }

  @Bean
  @ConditionalOnMissingBean(DCQLScopeMappingRepository.class)
  public DCQLScopeMappingRepository inMemoryDCQLScopeMappingRepository() {
    log.info("No DCQLScopeMappingRepository bean found. Using InMemoryDCQLScopeMappingRepository as default.");
    return new InMemoryDCQLScopeMappingRepository();
  }
}