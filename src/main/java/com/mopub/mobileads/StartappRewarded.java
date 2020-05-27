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

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.DataKeys;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPubReward;
import com.mopub.common.logging.MoPubLog;
import com.startapp.sdk.adsbase.Ad;
import com.startapp.sdk.adsbase.StartAppAd;
import com.startapp.sdk.adsbase.VideoListener;
import com.startapp.sdk.adsbase.adlisteners.AdDisplayListener;
import com.startapp.sdk.adsbase.adlisteners.AdEventListener;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.DID_DISAPPEAR;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOULD_REWARD;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;
import static com.mopub.mobileads.StartappConfig.AD_NETWORK_ID;

public class StartappRewarded extends CustomEventRewardedVideo {
    private static final String LOG_TAG = StartappRewarded.class.getSimpleName();

    @Nullable
    private StartAppAd rewarded;

    @NonNull
    private final StartappConfig configuration = new StartappConfig();

    private boolean isLoaded;

    @Override
    protected boolean hasVideoAvailable() {
        return rewarded != null && isLoaded;
    }

    @Override
    protected void showVideo() {
        MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, LOG_TAG);

        if (rewarded == null) {
            return;
        }

        if (!hasVideoAvailable()) {
            MoPubLog.log(getAdNetworkId(), SHOW_FAILED, LOG_TAG,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);

            MoPubRewardedVideoManager.onRewardedVideoPlaybackError(
                    StartappRewarded.class,
                    getAdNetworkId(),
                    MoPubErrorCode.NETWORK_NO_FILL);

            return;
        }

        rewarded.showAd(new AdDisplayListener() {
            @Override
            public void adHidden(@NonNull Ad ad) {
                MoPubLog.log(getAdNetworkId(), DID_DISAPPEAR, LOG_TAG);

                MoPubRewardedVideoManager.onRewardedVideoClosed(
                        StartappRewarded.class,
                        getAdNetworkId());
            }

            @Override
            public void adDisplayed(@NonNull Ad ad) {
                MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, LOG_TAG);

                MoPubRewardedVideoManager.onRewardedVideoStarted(
                        StartappRewarded.class,
                        getAdNetworkId());
            }

            @Override
            public void adClicked(@NonNull Ad ad) {
                MoPubLog.log(getAdNetworkId(), CLICKED, LOG_TAG);

                MoPubRewardedVideoManager.onRewardedVideoClicked(
                        StartappRewarded.class,
                        getAdNetworkId());
            }

            @Override
            public void adNotDisplayed(@NonNull Ad ad) {
                MoPubLog.log(getAdNetworkId(), SHOW_FAILED, LOG_TAG);

                MoPubRewardedVideoManager.onRewardedVideoPlaybackError(
                        StartappRewarded.class,
                        getAdNetworkId(),
                        MoPubErrorCode.VIDEO_PLAYBACK_ERROR);
            }
        });
    }

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return null;
    }

    @Override
    protected boolean checkAndInitializeSdk(
            @NonNull Activity launcherActivity,
            @NonNull Map<String, Object> localExtras,
            @NonNull Map<String, String> serverExtras
    ) throws Exception {
        final StartappExtras extras = new StartappExtras(localExtras, serverExtras, false);

        if (StartappConfig.initializeSdkIfNeeded(launcherActivity, extras.getAppId())) {
            configuration.setCachedInitializationParameters(launcherActivity, serverExtras);
            return true;
        }

        return false;
    }

    @Override
    protected void loadWithSdkInitialized(
            @NonNull Activity activity,
            @NonNull Map<String, Object> localExtras,
            @NonNull Map<String, String> serverExtras
    ) throws Exception {
        rewarded = new StartAppAd(activity.getApplicationContext());
        rewarded.setVideoListener(new VideoListener() {
            @Override
            public void onVideoCompleted() {
                MoPubLog.log(getAdNetworkId(), SHOULD_REWARD, LOG_TAG);

                MoPubRewardedVideoManager.onRewardedVideoCompleted(
                        StartappRewarded.class,
                        getAdNetworkId(),
                        MoPubReward.success("", 1));
            }
        });

        final AdEventListener loadListener = new AdEventListener() {
            @Override
            public void onReceiveAd(@NonNull Ad ad) {
                isLoaded = true;

                MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, LOG_TAG);

                MoPubRewardedVideoManager.onRewardedVideoLoadSuccess(
                        StartappRewarded.class,
                        getAdNetworkId());
            }

            @Override
            public void onFailedToReceiveAd(@NonNull Ad ad) {
                final String message = ad.getErrorMessage();
                final MoPubErrorCode errorCode = (message != null && (message.contains("204") || message.contains("Empty Response")))
                        ? MoPubErrorCode.NETWORK_NO_FILL
                        : MoPubErrorCode.UNSPECIFIED;

                MoPubLog.log(getAdNetworkId(), LOAD_FAILED, LOG_TAG,
                        errorCode.getIntCode(),
                        errorCode);

                MoPubRewardedVideoManager.onRewardedVideoLoadFailure(
                        StartappRewarded.class,
                        getAdNetworkId(),
                        errorCode);
            }
        };

        final String adUnitId = (String) localExtras.get(DataKeys.AD_UNIT_ID_KEY);
        StartappExtras.LocalExtras settings = null;
        if (adUnitId != null) {
            settings = MoPubRewardedVideoManager.getInstanceMediationSettings(
                    StartappExtras.LocalExtras.class, adUnitId);
        }

        final StartappExtras extras = new StartappExtras(settings, serverExtras, false);
        rewarded.loadAd(StartAppAd.AdMode.REWARDED_VIDEO, extras.getAdPreferences(), loadListener);

        MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, LOG_TAG);
    }

    @NonNull
    @Override
    protected String getAdNetworkId() {
        return AD_NETWORK_ID;
    }

    @Override
    protected void onInvalidate() {
        if (rewarded != null) {
            rewarded = null;
        }
    }
}
