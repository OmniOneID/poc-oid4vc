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

import org.omnione.did.sdk.oid4vc.data.dto.CredentialRequest;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Url;

public interface ApiService {

    /**
     * Retrieves a credential offer for testing purposes.
     *
     * @return A Call object for the response body.
     */
    @GET("credential-offer/test")
    Call<ResponseBody> getCredentialOfferForTest();

    /**
     * Retrieves information about the credential issuer.
     *
     * @return A Call object for the response body.
     */
    @GET(".well-known/openid-credential-issuer")
    Call<ResponseBody> getIssuerInfo();

    /**
     * Submits a credential request to the issuer.
     *
     * @param authorization The authorization header.
     * @param body The credential request body.
     * @return A Call object for the response body.
     */
    @POST("credential")
    Call<ResponseBody> getCredential(
            @Header("Authorization") String authorization,
            @Body CredentialRequest body
    );

    /**
     * Retrieves authorization from the issuer.
     *
     * @return A Call object for the response body.
     */
    @GET("authorize")
    Call<ResponseBody> getAuthorize();

    /**
     * Obtains a token using a pre-authorized code.
     *
     * @param authorization The authorization header.
     * @param grantType The grant type.
     * @param preAuthorizedCode The pre-authorized code.
     * @param txCode The transaction code.
     * @param authorizationDetailsJson JSON string containing authorization details.
     * @return A Call object for the response body.
     */
    @FormUrlEncoded
    @POST("oauth2/token")
    Call<ResponseBody> getTokenByPreAuthCode(
            @Header("Authorization") String authorization,
            @Field("grant_type") String grantType,
            @Field("pre-authorized_code") String preAuthorizedCode,
            @Field("tx_code") String txCode,
            @Field("authorization_details") String authorizationDetailsJson
    );

    /**
     * Obtains a token using an authorization code.
     *
     * @param authorization The authorization header.
     * @param grantType The grant type.
     * @param code The authorization code.
     * @param codeVerifier The code verifier for PKCE.
     * @param redirectUri The redirect URI.
     * @param clientId The client identifier.
     * @return A Call object for the response body.
     */
    @FormUrlEncoded
    @POST("oauth2/token")
    Call<ResponseBody> getTokenByAuthCode(
            @Header("Authorization") String authorization,
            @Field("grant_type") String grantType,
            @Field("code") String code,
            @Field("code_verifier") String codeVerifier,
            @Field("redirect_uri") String redirectUri,
            @Field("client_id") String clientId
    );

    /**
     * Performs a GET request to a specified URL.
     *
     * @param url The URL to send the request to.
     * @return A Call object for the response body.
     */
    @GET
    Call<ResponseBody> getRequest(@Url String url);

    /**
     * Submits a VP (Verifiable Presentation) token to a specified URL.
     *
     * @param url The URL to send the request to.
     * @param vpToken The VP token.
     * @param state The state parameter.
     * @return A Call object for the response body.
     */
    @FormUrlEncoded
    @POST
    Call<ResponseBody> postVpToken(
            @Url String url,
            @Field("vp_token") String vpToken,
            @Field("state") String state
    );
}
