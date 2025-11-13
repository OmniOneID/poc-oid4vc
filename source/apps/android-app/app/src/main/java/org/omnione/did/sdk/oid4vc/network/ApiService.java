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

package org.omnione.did.sdk.oid4vc.network;

import org.omnione.did.sdk.oid4vc.data.dto.CredentialOfferRequest;
import org.omnione.did.sdk.oid4vc.data.dto.CredentialRequest;

import okhttp3.ResponseBody; // Change import to ResponseBody
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST; // POST method example
import retrofit2.http.Url;

public interface ApiService {

    //oid4vci

    // Credential offer request
    @GET("credential-offer/test")
    Call<ResponseBody> getCredentialOfferForTest();
    // Issuer information query
    @GET(".well-known/openid-credential-issuer")
    Call<ResponseBody> getIssuerInfo();

    // POST method for Credential request
    @POST("credential")
    Call<ResponseBody> getCredential(
            @Header("Authorization") String authorization,
            @Body CredentialRequest body
    );
    // Authorize
    @GET("authorize")
    Call<ResponseBody> getAuthorize();

    // Token POST request (x-www-form-urlencoded) - pre-authorized-code
    @FormUrlEncoded
    @POST("oauth2/token")
    Call<ResponseBody> getTokenByPreAuthCode(
            @Header("Authorization") String authorization,
            @Field("grant_type") String grantType,
            @Field("pre-authorized_code") String preAuthorizedCode,
            @Field("tx_code") String txCode,
            @Field("authorization_details") String authorizationDetailsJson
    );

    // Token POST request (x-www-form-urlencoded) - authorization-code
    @FormUrlEncoded
    @POST("oauth2/token")
    Call<ResponseBody> getTokenByAuthCode(
            @Header("Authorization") String authorization,
            @Field("grant_type") String grantType,
            @Field("code") String code,
            @Field("code_verifier") String codeVerifier,
            @Field("redirect_uri") String redirectUri,
            @Field("client_id") String clientId
//            @Field("client_assertion_type") String clientAssertionType,
//            @Field("client_assertion") String clientAssertion

    );
//    grant_type=authorization_code
//&code=SplxlOBeZQQYbYS6WxSbIA
//&code_verifier=dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk
//&redirect_uri=https%3A%2F%2Fwallet.example.org%2Fcb
//&client_assertion_type=urn%3Aietf%3Aparams%3Aoauth%3Aclient-assertion-type%3Ajwt-bearer
//&client_assertion=eyJhbGciOiJSU...
    //oid4vp

    //authorization request
    @GET
    Call<ResponseBody> getRequest(@Url String url);

    // VP Token submission (Form URL Encoded)
    @FormUrlEncoded
    @POST
    Call<ResponseBody> postVpToken(
            @Url String url,
            @Field("vp_token") String vpToken,
            @Field("state") String state
    );
}