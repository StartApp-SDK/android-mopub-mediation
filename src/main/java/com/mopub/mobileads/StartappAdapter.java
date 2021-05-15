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
import android.content.Context;
import android.location.Location;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.LifecycleListener;
import com.mopub.common.MediationSettings;
import com.mopub.common.MoPubReward;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Views;
import com.startapp.sdk.ads.banner.Banner;
import com.startapp.sdk.ads.banner.BannerBase;
import com.startapp.sdk.ads.banner.BannerListener;
import com.startapp.sdk.ads.banner.Mrec;
import com.startapp.sdk.ads.banner.banner3d.Banner3D;
import com.startapp.sdk.ads.nativead.NativeAdPreferences;
import com.startapp.sdk.adsbase.Ad;
import com.startapp.sdk.adsbase.StartAppAd;
import com.startapp.sdk.adsbase.StartAppSDK;
import com.startapp.sdk.adsbase.adlisteners.AdDisplayListener;
import com.startapp.sdk.adsbase.adlisteners.AdEventListener;
import com.startapp.sdk.adsbase.adlisteners.VideoListener;
import com.startapp.sdk.adsbase.model.AdPreferences;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.mopub.common.DataKeys.ADUNIT_FORMAT;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.DID_DISAPPEAR;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOULD_REWARD;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.WILL_LEAVE_APPLICATION;
import static java.util.Locale.ENGLISH;

@Keep
public class StartappAdapter extends BaseAd {
    private static final String LOG_TAG = StartappAdapter.class.getSimpleName();

    public static final String AD_NETWORK_ID = "StartAppSDK";

    private static final String ADUNIT_BANNER = "banner";
    private static final String ADUNIT_MEDIUM_RECTANGLE = "medium_rectangle";
    private static final String APP_ID = "startappAppId";

    private static final AtomicBoolean isInitialized = new AtomicBoolean(false);

    public static boolean initializeSdkIfNeeded(@NonNull Context context, @Nullable String appId) {
        if (TextUtils.isEmpty(appId)) {
            return false;
        }

        if (!isInitialized.getAndSet(true)) {
            StartAppAd.disableSplash();
            StartAppAd.enableConsent(context, false);
            StartAppSDK.addWrapper(context, "MoPub", BuildConfig.VERSION_NAME);
            StartAppSDK.init(context, appId, false);

            return true;
        }

        return false;
    }

    // region Extras
    @Keep
    public static class Extras {
        private static final String AD_TAG = "adTag";
        private static final String INTERSTITIAL_MODE = "interstitialMode";
        private static final String MIN_CPM = "minCPM";
        private static final String MUTE_VIDEO = "muteVideo";
        private static final String IS_3D_BANNER = "is3DBanner";
        private static final String NATIVE_IMAGE_SIZE = "nativeImageSize";
        private static final String NATIVE_SECONDARY_IMAGE_SIZE = "nativeSecondaryImageSize";

        public enum Mode {
            OFFERWALL,
            VIDEO,
            OVERLAY
        }

        public enum Size {
            SIZE72X72,
            SIZE100X100,
            SIZE150X150,
            SIZE340X340,
            SIZE1200X628
        }

        @NonNull
        private final AdPreferences adPreferences;

        @NonNull
        public AdPreferences getAdPreferences() {
            return adPreferences;
        }

        private boolean is3DBanner;

        boolean is3DBanner() {
            return is3DBanner;
        }

        @Nullable
        private StartAppAd.AdMode adMode;

        @Nullable
        StartAppAd.AdMode getAdMode() {
            return adMode;
        }

        @Nullable
        private String appId;

        @Nullable
        public String getAppId() {
            return appId;
        }

        public Extras(@Nullable Map<String, Object> localExtras, @Nullable Map<String, String> serverExtras, boolean isNative) {
            adPreferences = makeAdPreferences(localExtras, serverExtras, isNative);
        }

        @NonNull
        private AdPreferences makeAdPreferences(
                @Nullable Map<String, Object> localExtras,
                @Nullable Map<String, String> serverExtras,
                boolean isNative
        ) {
            String adTag = null;
            boolean isVideoMuted = false;
            Double minCPM = null;
            Size nativeImageSize = null;
            Size nativeSecondaryImageSize = null;

            if (localExtras != null) {
                adTag = (String) localExtras.get(AD_TAG);
                nativeImageSize = (Extras.Size)localExtras.get(NATIVE_IMAGE_SIZE);
                nativeSecondaryImageSize = (Extras.Size)localExtras.get(NATIVE_SECONDARY_IMAGE_SIZE);

                if (localExtras.containsKey(MUTE_VIDEO)) {
                    isVideoMuted = (boolean) localExtras.get(MUTE_VIDEO);
                }

                if (localExtras.containsKey(IS_3D_BANNER)) {
                    is3DBanner = (boolean) localExtras.get(IS_3D_BANNER);
                }

                if (localExtras.containsKey(MIN_CPM)) {
                    minCPM = (double) localExtras.get(MIN_CPM);
                }

                if (localExtras.containsKey(INTERSTITIAL_MODE)) {
                    final Extras.Mode srcMode = (Extras.Mode) localExtras.get(INTERSTITIAL_MODE);
                    if (srcMode != null) {
                        switch (srcMode) {
                            case OVERLAY:
                                adMode = StartAppAd.AdMode.OVERLAY;
                                break;
                            case VIDEO:
                                adMode = StartAppAd.AdMode.VIDEO;
                                break;
                            case OFFERWALL:
                                adMode = StartAppAd.AdMode.OFFERWALL;
                                break;
                        }
                    }
                }
            }

            if (serverExtras != null) {
                Log.v(LOG_TAG, "Startapp serverParameter:" + serverExtras.toString());

                if (serverExtras.containsKey(AD_TAG)) {
                    adTag = serverExtras.get(AD_TAG);
                }

                if (serverExtras.containsKey(MUTE_VIDEO)) {
                    final String str = serverExtras.get(MUTE_VIDEO);
                    if (!TextUtils.isEmpty(str)) {
                        isVideoMuted = str.contains("true");
                    }
                }

                if (serverExtras.containsKey(IS_3D_BANNER)) {
                    final String str = serverExtras.get(IS_3D_BANNER);
                    if (!TextUtils.isEmpty(str)) {
                        is3DBanner = str.contains("true");
                    }
                }

                if (serverExtras.containsKey(MIN_CPM)) {
                    final String str = serverExtras.get(MIN_CPM);
                    if (!TextUtils.isEmpty(str)) {
                        minCPM = Double.valueOf(str);
                    }
                }

                if (serverExtras.containsKey(INTERSTITIAL_MODE)) {
                    final String mode = serverExtras.get(INTERSTITIAL_MODE);
                    if (!TextUtils.isEmpty(mode)) {
                        switch (mode) {
                            case "OVERLAY":
                                adMode = StartAppAd.AdMode.OVERLAY;
                                break;
                            case "VIDEO":
                                adMode = StartAppAd.AdMode.VIDEO;
                                break;
                            case "OFFERWALL":
                                adMode = StartAppAd.AdMode.OFFERWALL;
                                break;
                        }
                    }
                }

                if (serverExtras.containsKey(NATIVE_IMAGE_SIZE)) {
                    final String name = serverExtras.get(NATIVE_IMAGE_SIZE);
                    if (name != null) {
                        nativeImageSize = Extras.Size.valueOf(name);
                    }
                }

                if (serverExtras.containsKey(NATIVE_SECONDARY_IMAGE_SIZE)) {
                    final String name = serverExtras.get(NATIVE_SECONDARY_IMAGE_SIZE);
                    if (name != null) {
                        nativeSecondaryImageSize = Extras.Size.valueOf(name);
                    }
                }

                if (serverExtras.containsKey(APP_ID)) {
                    appId = serverExtras.get(APP_ID);
                }
            }

            NativeAdPreferences nativeAdPrefs = null;
            AdPreferences prefs;
            if (isNative) {
                nativeAdPrefs = new NativeAdPreferences();
                prefs = nativeAdPrefs;
            } else {
                prefs = new AdPreferences();
            }

            if (localExtras != null) {
                final String locationKey = "location";
                if (localExtras.containsKey(locationKey)) {
                    final Object locationObject = localExtras.get(locationKey);
                    if (locationObject instanceof Location) {
                        final Location location = (Location) locationObject;
                        prefs.setLatitude(location.getLatitude());
                        prefs.setLongitude(location.getLongitude());
                    }
                }
            }

            prefs.setAdTag(adTag);
            prefs.setMinCpm(minCPM);

            if (isVideoMuted) {
                prefs.muteVideo();
            }

            if (isNative) {
                if (nativeImageSize != null) {
                    nativeAdPrefs.setPrimaryImageSize(nativeImageSize.ordinal());
                }

                if (nativeSecondaryImageSize != null) {
                    nativeAdPrefs.setSecondaryImageSize(nativeSecondaryImageSize.ordinal());
                }
            }

            return prefs;
        }

        @Keep
        public static class LocalExtras extends HashMap<String, Object> implements MediationSettings {}

        @Keep
        public static class Builder {
            final LocalExtras extras = new LocalExtras();

            @NonNull
            public Builder setAdTag(@NonNull String adTag) {
                extras.put(AD_TAG, adTag);
                return this;
            }

            @NonNull
            public Builder setInterstitialMode(@NonNull Mode interstitialMode) {
                extras.put(INTERSTITIAL_MODE, interstitialMode);
                return this;
            }

            @NonNull
            public Builder setMinCPM(double cpm) {
                extras.put(MIN_CPM, cpm);
                return this;
            }

            @NonNull
            public Builder muteVideo() {
                extras.put(MUTE_VIDEO, true);
                return this;
            }

            @NonNull
            public Builder enable3DBanner() {
                extras.put(IS_3D_BANNER, true);
                return this;
            }

            @NonNull
            public Builder setNativeImageSize(@NonNull Size size) {
                extras.put(NATIVE_IMAGE_SIZE, size);
                return this;
            }

            @NonNull
            public Builder setNativeSecondaryImageSize(@NonNull Size size) {
                extras.put(NATIVE_SECONDARY_IMAGE_SIZE, size);
                return this;
            }

            @NonNull
            public LocalExtras toMap() {
                return extras;
            }
        }
    }
    // endregion

    // region Common
    @Nullable
    private StartAppAd startAppAd;

    @Override
    protected void onInvalidate() {
        startAppAd = null;
        isRewarded = false;
        isMediumRectangle = false;
        isRewardedVideoLoaded = false;

        Views.removeFromParent(bannerView);
        bannerView = null;
    }

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return null;
    }

    @NonNull
    @Override
    protected String getAdNetworkId() {
        return AD_NETWORK_ID;
    }

    @Nullable
    @Override
    protected View getAdView() {
        return bannerView;
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull Activity launcherActivity, @NonNull AdData adData) throws Exception {
        return initializeSdkIfNeeded(launcherActivity, adData.getExtras().get(APP_ID));
    }

    @Override
    protected void load(@NonNull Context context, @NonNull AdData adData) throws Exception {
        if (adData.isRewarded()) {
            loadRewardedVideo(context, adData);
            return;
        }

        final String adUnitFormat = adData.getExtras().get(ADUNIT_FORMAT).toLowerCase(ENGLISH);
        if (adUnitFormat.contains(ADUNIT_BANNER) || adUnitFormat.contains(ADUNIT_MEDIUM_RECTANGLE)) {
            if (adUnitFormat.contains(ADUNIT_MEDIUM_RECTANGLE)) {
                isMediumRectangle = true;
            }

            loadBanner(context, adData);
        } else {
            loadInterstitial(context, adData);
        }
    }

    @Override
    protected void show() {
        if (isRewarded) {
            showRewardedVideo();
        } else if (bannerView == null) {
            showInterstitial();
        }
    }
    // endregion

    // region Interstitial
    private void loadInterstitial(@NonNull Context context, @NonNull AdData adData) {
        setAutomaticImpressionAndClickTracking(false);

        startAppAd = new StartAppAd(context);
        final AdEventListener loadListener = new AdEventListener() {
            @Override
            public void onReceiveAd(@NonNull Ad ad) {
                MoPubLog.log(AD_NETWORK_ID, LOAD_SUCCESS, LOG_TAG);

                if (mLoadListener != null) {
                    mLoadListener.onAdLoaded();
                }
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

                if (mLoadListener != null) {
                    mLoadListener.onAdLoadFailed(errorCode);
                }
            }
        };

        final Extras prefs = new Extras(null, adData.getExtras(), false);
        if (prefs.getAdMode() == null) {
            startAppAd.loadAd(prefs.getAdPreferences(), loadListener);
        } else {
            startAppAd.loadAd(prefs.getAdMode(), prefs.getAdPreferences(), loadListener);
        }

        MoPubLog.log(AD_NETWORK_ID, LOAD_ATTEMPTED, LOG_TAG);
    }

    private void showInterstitial() {
        if (startAppAd == null) {
            return;
        }

        MoPubLog.log(AD_NETWORK_ID, SHOW_ATTEMPTED, LOG_TAG);

        if (!startAppAd.isReady()) {
            final MoPubErrorCode errorCode = MoPubErrorCode.FULLSCREEN_SHOW_ERROR;

            MoPubLog.log(AD_NETWORK_ID, SHOW_FAILED, LOG_TAG,
                    errorCode.getIntCode(),
                    errorCode);

            if (mInteractionListener != null) {
                mInteractionListener.onAdFailed(errorCode);
            }

            return;
        }

        startAppAd.showAd(new AdDisplayListener() {
            @Override
            public void adHidden(@NonNull Ad ad) {
                MoPubLog.log(AD_NETWORK_ID, DID_DISAPPEAR, LOG_TAG);

                if (mInteractionListener != null) {
                    mInteractionListener.onAdDismissed();
                }
            }

            @Override
            public void adDisplayed(@NonNull Ad ad) {
                MoPubLog.log(AD_NETWORK_ID, SHOW_SUCCESS, LOG_TAG);

                if (mInteractionListener != null) {
                    mInteractionListener.onAdShown();
                    mInteractionListener.onAdImpression();
                }
            }

            @Override
            public void adClicked(@NonNull Ad ad) {
                MoPubLog.log(AD_NETWORK_ID, CLICKED, LOG_TAG);

                if (mInteractionListener != null) {
                    mInteractionListener.onAdClicked();
                }
            }

            @Override
            public void adNotDisplayed(@NonNull Ad ad) {
                MoPubLog.log(AD_NETWORK_ID, SHOW_FAILED, LOG_TAG);

                if (mInteractionListener != null) {
                    mInteractionListener.onAdFailed(MoPubErrorCode.FULLSCREEN_SHOW_ERROR);
                }
            }
        });
    }
    // endregion

    // region Rewarded
    private boolean isRewarded;
    private boolean isRewardedVideoLoaded;

    private void loadRewardedVideo(@NonNull Context context, @NonNull AdData adData) {
        isRewarded = true;

        startAppAd = new StartAppAd(context);
        startAppAd.setVideoListener(new VideoListener() {
            @Override
            public void onVideoCompleted() {
                MoPubLog.log(getAdNetworkId(), SHOULD_REWARD, LOG_TAG);

                if (mInteractionListener != null) {
                    mInteractionListener.onAdComplete(MoPubReward.success(
                            MoPubReward.NO_REWARD_LABEL,
                            MoPubReward.DEFAULT_REWARD_AMOUNT));
                }
            }
        });

        final AdEventListener loadListener = new AdEventListener() {
            @Override
            public void onReceiveAd(@NonNull Ad ad) {
                isRewardedVideoLoaded = true;

                MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, LOG_TAG);

                if (mLoadListener != null) {
                    mLoadListener.onAdLoaded();
                }
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

                if (mLoadListener != null) {
                    mLoadListener.onAdLoadFailed(errorCode);
                }
            }
        };

        final Extras prefs = new Extras(null, adData.getExtras(), false);
        startAppAd.loadAd(StartAppAd.AdMode.REWARDED_VIDEO, prefs.getAdPreferences(), loadListener);

        MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, LOG_TAG);
    }

    private void showRewardedVideo() {
        MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, LOG_TAG);

        if (startAppAd == null || !isRewardedVideoLoaded) {
            final MoPubErrorCode errorCode = MoPubErrorCode.VIDEO_DOWNLOAD_ERROR;
            MoPubLog.log(getAdNetworkId(), SHOW_FAILED, LOG_TAG,
                    errorCode.getIntCode(),
                    errorCode);

            if (mInteractionListener != null) {
                mInteractionListener.onAdFailed(errorCode);
            }

            return;
        }

        startAppAd.showAd(new AdDisplayListener() {
            @Override
            public void adHidden(@NonNull Ad ad) {
                MoPubLog.log(getAdNetworkId(), DID_DISAPPEAR, LOG_TAG);

                if (mInteractionListener != null) {
                    mInteractionListener.onAdDismissed();
                }
            }

            @Override
            public void adDisplayed(@NonNull Ad ad) {
                MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, LOG_TAG);

                if (mInteractionListener != null) {
                    mInteractionListener.onAdShown();
                    mInteractionListener.onAdImpression();
                }
            }

            @Override
            public void adClicked(@NonNull Ad ad) {
                MoPubLog.log(getAdNetworkId(), CLICKED, LOG_TAG);

                if (mInteractionListener != null) {
                    mInteractionListener.onAdClicked();
                }
            }

            @Override
            public void adNotDisplayed(@NonNull Ad ad) {
                MoPubLog.log(getAdNetworkId(), SHOW_FAILED, LOG_TAG);

                if (mInteractionListener != null) {
                    mInteractionListener.onAdFailed(MoPubErrorCode.VIDEO_PLAYBACK_ERROR);
                }
            }
        });
    }
    // endregion

    // region Banner
    private boolean isMediumRectangle;

    @Nullable
    private FrameLayout bannerView;

    private void loadBanner(@NonNull Context context, @NonNull AdData adData) {
        if (!(context instanceof Activity)) {
            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }
            return;
        }

        if (adData.getAdWidth() == null || adData.getAdHeight() == null) {
            bannerLoadingFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        bannerView = new FrameLayout(context);

        final BannerListener loadListener = new BannerListener() {
            @Override
            public void onReceiveAd(@NonNull View view) {
                MoPubLog.log(AD_NETWORK_ID, LOAD_SUCCESS, LOG_TAG);
                MoPubLog.log(AD_NETWORK_ID, SHOW_ATTEMPTED, LOG_TAG);

                if (mLoadListener != null) {
                    mLoadListener.onAdLoaded();
                }
            }

            @Override
            public void onFailedToReceiveAd(@NonNull View view) {
                bannerLoadingFailed(MoPubErrorCode.NETWORK_NO_FILL);
            }

            @Override
            public void onImpression(@NonNull View view) {
                MoPubLog.log(AD_NETWORK_ID, SHOW_SUCCESS, LOG_TAG);

                if (mInteractionListener != null) {
                    mInteractionListener.onAdImpression();
                }
            }

            @Override
            public void onClick(@NonNull View view) {
                MoPubLog.log(AD_NETWORK_ID, CLICKED, LOG_TAG);
                MoPubLog.log(AD_NETWORK_ID, WILL_LEAVE_APPLICATION, LOG_TAG);

                if (mInteractionListener != null) {
                    mInteractionListener.onAdClicked();
                }
            }
        };

        final BannerBase banner = chooseBanner(context, adData.getExtras(), loadListener);
        banner.loadAd(adData.getAdWidth(), adData.getAdHeight());

        // force banner to calculate its view size
        bannerView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        bannerView.addView(banner, new FrameLayout.LayoutParams(
                dpToPx(context, adData.getAdWidth()),
                dpToPx(context, adData.getAdHeight()),
                Gravity.CENTER));
    }

    @NonNull
    private BannerBase chooseBanner(
            @NonNull Context context,
            @Nullable Map<String, String> extras,
            @NonNull BannerListener listener
    ) {
        setAutomaticImpressionAndClickTracking(false);

        final Extras prefs = new Extras(null, extras, false);
        final Activity activity = (Activity) context;
        final BannerBase result;

        if (isMediumRectangle) {
            result = new Mrec(activity, prefs.getAdPreferences(), listener);
        } else if (prefs.is3DBanner()) {
            result = new Banner3D(activity, prefs.getAdPreferences(), listener);
        } else {
            result = new Banner(activity, prefs.getAdPreferences(), listener);
        }

        MoPubLog.log(AD_NETWORK_ID, LOAD_ATTEMPTED, LOG_TAG);

        return result;
    }

    private static int dpToPx(@NonNull Context context, int dp) {
        final DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return (int) ((dp * metrics.density) + 0.5);
    }

    private void bannerLoadingFailed(@NonNull MoPubErrorCode code) {
        MoPubLog.log(AD_NETWORK_ID, LOAD_FAILED, LOG_TAG,
                code.getIntCode(),
                code);

        if (mLoadListener != null) {
            mLoadListener.onAdLoadFailed(code);
        }
    }
    // endregion
}
