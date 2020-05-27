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

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.logging.MoPubLog;
import com.startapp.sdk.adsbase.Ad;
import com.startapp.sdk.adsbase.StartAppAd;
import com.startapp.sdk.adsbase.adlisteners.AdDisplayListener;
import com.startapp.sdk.adsbase.adlisteners.AdEventListener;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.DID_DISAPPEAR;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;
import static com.mopub.mobileads.StartappConfig.AD_NETWORK_ID;

@Keep
public class StartappInterstitial extends CustomEventInterstitial {
    private static final String LOG_TAG = StartappInterstitial.class.getSimpleName();

    @NonNull
    private final StartappConfig configuration = new StartappConfig();

    @Nullable
    private StartAppAd interstitial;

    @Nullable
    private CustomEventInterstitialListener interstitialListener;

    @Override
    protected void loadInterstitial(
            @NonNull Context context,
            @NonNull final CustomEventInterstitialListener listener,
            @Nullable Map<String, Object> localExtras,
            @Nullable Map<String, String> serverExtras
    ) {
        final StartappExtras extras = new StartappExtras(localExtras, serverExtras, false);

        StartappConfig.initializeSdkIfNeeded(context, extras.getAppId());

        setAutomaticImpressionAndClickTracking(false);

        interstitialListener = listener;
        interstitial = new StartAppAd(context);
        configuration.setCachedInitializationParameters(context, serverExtras);

        final AdEventListener loadListener = new AdEventListener() {
            @Override
            public void onReceiveAd(@NonNull Ad ad) {
                MoPubLog.log(AD_NETWORK_ID, LOAD_SUCCESS, LOG_TAG);

                listener.onInterstitialLoaded();
            }

            @Override
            public void onFailedToReceiveAd(@NonNull Ad ad) {
                final String message = ad.getErrorMessage();
                final MoPubErrorCode errorCode = (message != null && (message.contains("204") || message.contains("Empty Response")))
                        ? MoPubErrorCode.NETWORK_NO_FILL
                        : MoPubErrorCode.UNSPECIFIED;

                MoPubLog.log(AD_NETWORK_ID, LOAD_FAILED, LOG_TAG,
                        errorCode.getIntCode(),
                        errorCode);

                listener.onInterstitialFailed(errorCode);
            }
        };

        if (extras.getAdMode() == null) {
            interstitial.loadAd(extras.getAdPreferences(), loadListener);
        } else {
            interstitial.loadAd(extras.getAdMode(), extras.getAdPreferences(), loadListener);
        }

        MoPubLog.log(AD_NETWORK_ID, LOAD_ATTEMPTED, LOG_TAG);
    }

    @Override
    protected void showInterstitial() {
        if (interstitial == null) {
            return;
        }

        MoPubLog.log(AD_NETWORK_ID, SHOW_ATTEMPTED, LOG_TAG);

        if (!interstitial.isReady()) {
            MoPubLog.log(AD_NETWORK_ID, SHOW_FAILED, LOG_TAG,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);

            return;
        }

        interstitial.showAd(new AdDisplayListener() {
            @Override
            public void adHidden(@NonNull Ad ad) {
                if (interstitialListener == null) {
                    return;
                }

                MoPubLog.log(AD_NETWORK_ID, DID_DISAPPEAR, LOG_TAG);

                interstitialListener.onInterstitialDismissed();
            }

            @Override
            public void adDisplayed(@NonNull Ad ad) {
                MoPubLog.log(AD_NETWORK_ID, SHOW_SUCCESS, LOG_TAG);

                if (interstitialListener == null) {
                    return;
                }

                interstitialListener.onInterstitialShown();
                interstitialListener.onInterstitialImpression();
            }

            @Override
            public void adClicked(@NonNull Ad ad) {
                MoPubLog.log(AD_NETWORK_ID, CLICKED, LOG_TAG);

                if (interstitialListener == null) {
                    return;
                }

                interstitialListener.onInterstitialClicked();
                interstitialListener.onLeaveApplication();
            }

            @Override
            public void adNotDisplayed(@NonNull Ad ad) {
                MoPubLog.log(AD_NETWORK_ID, SHOW_FAILED, LOG_TAG);
            }
        });
    }

    @Override
    protected void onInvalidate() {
        if (interstitial != null) {
            interstitial = null;
        }
    }
}
