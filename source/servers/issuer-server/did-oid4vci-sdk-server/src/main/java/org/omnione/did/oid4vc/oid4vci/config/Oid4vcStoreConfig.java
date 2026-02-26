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

package org.omnione.did.oid4vc.oid4vci.config;

import org.omnione.did.oid4vc.oid4vci.service.store.CNonceStore;
import org.omnione.did.oid4vc.oid4vci.service.store.CredentialOfferStore;
import org.omnione.did.oid4vc.oid4vci.service.store.SessionStore;
import org.omnione.did.oid4vc.oid4vci.service.store.memory.InMemoryCNonceStore;
import org.omnione.did.oid4vc.oid4vci.service.store.memory.InMemoryCredentialOfferStore;
import org.omnione.did.oid4vc.oid4vci.service.store.memory.InMemorySessionStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Oid4vcStoreConfig {

    @Bean
    @ConditionalOnMissingBean(CredentialOfferStore.class)
    public CredentialOfferStore credentialOfferStore() {
        return new InMemoryCredentialOfferStore();
    }

    @Bean
    @ConditionalOnMissingBean(SessionStore.class)
    public SessionStore sessionStore() {
        return new InMemorySessionStore();
    }

    @Bean
    @ConditionalOnMissingBean(CNonceStore.class)
    public CNonceStore cNonceStore() {
        return new InMemoryCNonceStore();
    }
}
