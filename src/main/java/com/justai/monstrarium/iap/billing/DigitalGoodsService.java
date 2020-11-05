/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.justai.monstrarium.iap.billing;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.justai.monstrarium.utils.Config;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class DigitalGoodsService {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            DigitalGoodsService.class);
    private static final String APP_PKG = Config.Companion.getINAPP_PACKAGE_NAME();
    private static final String DG_SCOPE =
            "https://www.googleapis.com/auth/actions.purchases.digital";
    private static final String SKU_ENDPOINT =
            "https://actions.googleapis.com/v3/packages/" + APP_PKG + "/skus:batchGet";
    private static final List<String> SKU_TYPE_IN_APP_IDS =
            Arrays.asList("monstrarium.gold.pack.1", "monstrarium.gold.pack.2", "monstrarium.gold.pack.3");
    private static final List<String> SKU_TYPE_SUBSCRIPTION_IDS =
            Arrays.asList("gold_monthly", "gold_yearly");
    private static final String SKU_TYPE_IN_APP = "SKU_TYPE_IN_APP";
    private static final String SKU_TYPE_SUBSCRIPTION = "SKU_TYPE_SUBSCRIPTION";

    private static String getConsumeEndpoint(String convId) {
        return "https://actions.googleapis.com/v3/conversations/"
                + convId + "/entitlement:consume";
    }

    private static GoogleCredentials createGoogleCredential() {
        LOGGER.info(">>>>>>>>>> createGoogleCredential");
        GoogleCredentials credential;
        try {
            InputStream in = GoogleCredentials.class
                    .getResourceAsStream(Config.Companion.getIAP_CREDENTIAL_FILE());
            credential = GoogleCredentials.fromStream(in)
                    .createScoped(Collections.singleton(DG_SCOPE));
        } catch (IOException e) {
            LOGGER.error("Error loading credentials.json", e);
            return null;
        }

        LOGGER.info("+++++ credential created");
        try {
            credential.refresh();
        } catch (IOException e) {
            LOGGER.error("Error refreshing credentials.", e);
        }

        try {
            if (credential.getAccessToken() == null) {
                LOGGER.info("+++++ Refreshing Access Token");
                credential.refreshAccessToken();
            }
        } catch (Exception e) {
            LOGGER.error("Error refreshing access token", e);
        }

        return credential;
    }

    public static List<SkuDetails> getSkuDetails(String conversationId)
            throws IOException {
        LOGGER.info(">>>>>>>>>> getSkuDetails");
        ArrayList<SkuDetails> skuDetailsList = new ArrayList<>();

        AccessToken token = createGoogleCredential().getAccessToken();
        LOGGER.info("+++++ Access token: " + token.toString());

        JsonObject apiResponseForInApp = callApi(SKU_ENDPOINT,
                createPayloadForGet(conversationId,
                        SKU_TYPE_IN_APP, SKU_TYPE_IN_APP_IDS), token);
        JsonObject apiResponseForSubscriptions = callApi(SKU_ENDPOINT,
                createPayloadForGet(conversationId, SKU_TYPE_SUBSCRIPTION,
                        SKU_TYPE_SUBSCRIPTION_IDS), token);
        LOGGER.info("apiResponseForSubscriptions " + apiResponseForSubscriptions);
        LOGGER.info("apiResponseForIap " + apiResponseForInApp);
        if(apiResponseForInApp != null){
            for (JsonElement e : apiResponseForInApp.getAsJsonArray("skus")) {
                skuDetailsList.add(new Gson().fromJson(e, SkuDetails.class));
            }
        }

        // for (JsonElement e : apiResponseForSubscriptions.getAsJsonArray("skus")) {
        //  skuDetailsList.add(new Gson().fromJson(e, SkuDetails.class));
        //  }

        return skuDetailsList;
    }

    private static JSONObject createPayloadForGet(String conversationId,
                                                  String skuType,
                                                  List<String> ids) {
        JSONObject payload = new JSONObject();
        // construct the payload for the API
        payload.put("conversationId", conversationId);
        payload.put("skuType", skuType);
        JSONArray arrayWithinPayload = new JSONArray();
        for (String id : ids) {
            arrayWithinPayload.add(id);
        }
        payload.put("ids", arrayWithinPayload);
        return payload;
    }

    public static JsonObject consumePurchase(String conversationId,
                                       String purchaseToken)
            throws IOException {
        AccessToken token = createGoogleCredential().getAccessToken();
        LOGGER.info("+++++ Access token: " + token.toString());
         JsonObject result = callApi(getConsumeEndpoint(conversationId),
                createPayloadForConsume(purchaseToken), token);
        LOGGER.info(String.valueOf(result));
        return result;
    }

    private static JSONObject createPayloadForConsume(String purchaseToken) {
        JSONObject payload = new JSONObject();
        // construct the payload for the API
        payload.put("purchaseToken", purchaseToken);
        return payload;
    }

    private static JsonObject callApi(String urlString, JSONObject payload,
                                      AccessToken token)
            throws IOException {
        LOGGER.info(">>>>>>>>>> callApi");
        HttpResponse response = Request.Post(urlString)
                .setHeader("Authorization", "Bearer " + token.getTokenValue())
                .setHeader("Accept", "application/json")
                .setHeader("Content-type", "application/json")
                .body(new StringEntity(payload.toString()))
                .execute().returnResponse();
        int status = response.getStatusLine().getStatusCode();

        if (status <= HttpStatus.SC_MULTI_STATUS) {
            JsonObject resJson;
            String res = EntityUtils.toString(response.getEntity());
            LOGGER.info("++++ Response received: " + res);
            resJson = new Gson().fromJson(res, JsonObject.class);

            return resJson;
        } else if (status >= HttpStatus.SC_INTERNAL_SERVER_ERROR) {
            LOGGER.error("Request to " + urlString + " failed. Status: " + status);
            return null;
        } else if (status >= HttpStatus.SC_BAD_REQUEST) {
            LOGGER.error("Bad Request to " + urlString + ". Status: " + status);
            return null;
        } else if (status >= HttpStatus.SC_MULTIPLE_CHOICES) {
            LOGGER.error("Request to " + urlString + " has been redirected");
            return null;
        } else {
            LOGGER.error("Request to " + urlString + " failed. Please try again.");
            return null;
        }
    }

    public enum PurchaseResult {
        PURCHASE_STATUS_OK,
        PURCHASE_STATUS_ALREADY_OWNED,
        PURCHASE_STATUS_ITEM_UNAVAILABLE,
        PURCHASE_STATUS_ITEM_CHANGE_REQUESTED,
        PURCHASE_STATUS_USER_CANCELLED,
        PURCHASE_STATUS_ERROR,
        PURCHASE_STATUS_UNSPECIFIED
    }
}
