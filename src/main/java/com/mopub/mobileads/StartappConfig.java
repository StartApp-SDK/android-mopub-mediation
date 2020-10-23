package com.mopub.mobileads;

import android.content.Context;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.BaseAdapterConfiguration;
import com.mopub.common.OnNetworkInitializationFinishedListener;
import com.startapp.sdk.adsbase.StartAppSDK;

import java.util.Map;

import static com.mopub.mobileads.MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR;
import static com.mopub.mobileads.MoPubErrorCode.ADAPTER_INITIALIZATION_SUCCESS;
import static com.mopub.mobileads.StartappAdapter.AD_NETWORK_ID;
import static com.mopub.mobileads.startapp.BuildConfig.VERSION_NAME;

@Keep
public class StartappConfig extends BaseAdapterConfiguration {
    @NonNull
    @Override
    public String getAdapterVersion() {
        return VERSION_NAME;
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
    @SuppressWarnings("JavaReflectionMemberAccess")
    public String getNetworkSdkVersion() {
        String result = null;

        try {
            result = (String) StartAppSDK.class.getDeclaredMethod("getVersion").invoke(null);
        } catch (Throwable ex) {
            // ignore
        }

        if (result == null) {
            try {
                result = (String) Class.forName("com.startapp.sdk.GeneratedConstants")
                        .getDeclaredField("INAPP_VERSION")
                        .get(null);
            } catch (Throwable ex) {
                // ignore
            }
        }

        return result != null ? result : "0";
    }

    @Override
    public void initializeNetwork(
            @NonNull Context context,
            @Nullable Map<String, String> configuration,
            @NonNull OnNetworkInitializationFinishedListener listener
    ) {
        final String appId = configuration != null ? configuration.get("startappAppId") : null;
        if (appId != null) {
            StartappAdapter.initializeSdkIfNeeded(context, appId);
            listener.onNetworkInitializationFinished(StartappConfig.class, ADAPTER_INITIALIZATION_SUCCESS);
        } else {
            listener.onNetworkInitializationFinished(StartappConfig.class, ADAPTER_CONFIGURATION_ERROR);
        }
    }
}
