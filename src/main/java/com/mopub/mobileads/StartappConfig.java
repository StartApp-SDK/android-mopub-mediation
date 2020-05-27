/**
 * Copyright 2020 StartApp Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mopub.mobileads;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.BaseAdapterConfiguration;
import com.mopub.common.OnNetworkInitializationFinishedListener;
import com.mopub.mobileads.startapp.BuildConfig;
import com.startapp.sdk.GeneratedConstants;
import com.startapp.sdk.adsbase.StartAppAd;
import com.startapp.sdk.adsbase.StartAppSDK;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.mopub.mobileads.MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR;
import static com.mopub.mobileads.MoPubErrorCode.ADAPTER_INITIALIZATION_SUCCESS;
import static com.mopub.mobileads.startapp.BuildConfig.VERSION_CODE;

@Keep
public class StartappConfig extends BaseAdapterConfiguration {
    public static final String AD_NETWORK_ID = "StartAppSDK";

    @NonNull
    @Override
    public String getAdapterVersion() {
        final String versionString = GeneratedConstants.INAPP_VERSION;
        final String[] splits = versionString.split("\\.");

        if (splits.length >= 3) {
            return splits[0] + '.' + splits[1] + '.' + splits[2] + '.' + VERSION_CODE;
        }

        return versionString + '.' + VERSION_CODE;
    }

    @Nullable
    @Override
    public String getBiddingToken(@NonNull Context context) {
        return null;
    }

    @NonNull
    @Override
    public String getMoPubNetworkName() {
        return AD_NETWORK_ID;
    }

    @NonNull
    @Override
    public String getNetworkSdkVersion() {
        return GeneratedConstants.INAPP_VERSION;
    }

    @Override
    public void initializeNetwork(
            @NonNull Context context,
            @Nullable Map<String, String> configuration,
            @NonNull OnNetworkInitializationFinishedListener listener
    ) {
        final String appId = configuration != null ? configuration.get("startappAppId") : null;
        if (appId != null) {
            initializeSdkIfNeeded(context, appId);
            listener.onNetworkInitializationFinished(StartappConfig.class, ADAPTER_INITIALIZATION_SUCCESS);
        } else {
            listener.onNetworkInitializationFinished(StartappConfig.class, ADAPTER_CONFIGURATION_ERROR);
        }
    }

    private static AtomicBoolean sIsInitialized = new AtomicBoolean(false);

    public static boolean initializeSdkIfNeeded(@NonNull Context context, @Nullable String appId) {
        if (TextUtils.isEmpty(appId)) {
            return false;
        }

        if (!sIsInitialized.getAndSet(true)) {
            StartAppSDK.init(context, appId, false);
            StartAppAd.disableSplash();
            StartAppSDK.addWrapper(context, "MoPub", BuildConfig.VERSION_NAME);

            return true;
        }

        return false;
    }
}
