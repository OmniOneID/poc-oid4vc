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

package com.example.did.oid4vc.verifier.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "verification_session")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationSessionEntity {

    @Id
    @Column(name = "transaction_id", length = 64, nullable = false)
    private String transactionId;

    @Column(name = "state", length = 64, nullable = false)
    private String state;

    @Column(name = "nonce", length = 64, nullable = false)
    private String nonce;

    @Column(name = "dcql_query", columnDefinition = "TEXT")
    private String dcqlQuery;

    @Column(name = "response_mode", length = 32)
    private String responseMode;

    @Column(name = "request_id", length = 64)
    private String requestId;

    @Builder.Default
    @Column(name = "status", length = 32, nullable = false)
    private String status = "CREATED";

    @Column(name = "client_metadata", columnDefinition = "TEXT")
    private String clientMetadata;

    @Column(name = "request_uri_fetched_at")
    private Long requestUriFetchedAt;

    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "expires_at")
    private Long expiresAt;

    @Column(name = "vp_token", columnDefinition = "TEXT")
    private String vpToken;
}