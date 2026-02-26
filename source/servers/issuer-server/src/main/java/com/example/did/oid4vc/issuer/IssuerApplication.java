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

package com.example.did.oid4vc.issuer;

import org.omnione.did.oid4vc.oid4vci.property.IssuerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
		"com.example.did.oid4vc.issuer",
		"org.omnione.did.oid4vc.oid4vci"
})
@EnableFeignClients(basePackages = {
		"com.example.did.oid4vc.issuer",
		"org.omnione.did.oid4vc.oid4vci"
})
//@EnableJpaAuditing
//@EnableJpaRepositories(basePackages = {
//		"com.example.did.oid4vc.issuer.db.repository",
//		"org.omnione.did.oid4vc.oid4vci"
//})
//@EntityScan(basePackages = {
//		"com.example.did.oid4vc.issuer.db.entity",
//		"org.omnione.did.oid4vc.oid4vci"
//})
@EnableConfigurationProperties(IssuerProperties.class)
public class IssuerApplication {

	public static void main(String[] args) {
		SpringApplication.run(IssuerApplication.class, args);
	}

}
