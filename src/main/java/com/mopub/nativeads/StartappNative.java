package com.mopub.nativeads;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.StartappConfig;
import com.mopub.mobileads.StartappExtras;
import com.startapp.sdk.ads.nativead.NativeAdDetails;
import com.startapp.sdk.ads.nativead.NativeAdDisplayListener;
import com.startapp.sdk.ads.nativead.NativeAdInterface;
import com.startapp.sdk.ads.nativead.NativeAdPreferences;
import com.startapp.sdk.ads.nativead.StartAppNativeAd;
import com.startapp.sdk.adsbase.Ad;
import com.startapp.sdk.adsbase.adlisteners.AdEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.DID_DISAPPEAR;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;
import static com.mopub.mobileads.StartappConfig.AD_NETWORK_ID;

@Keep
public class StartappNative extends CustomEventNative {
    private static final String LOG_TAG = StartappNative.class.getSimpleName();

    @NonNull
    private final StartappConfig configuration = new StartappConfig();

    @Override
    protected void loadNativeAd(
            @NonNull Context context,
            @NonNull CustomEventNativeListener listener,
            @NonNull Map<String, Object> localExtras,
            @NonNull Map<String, String> serverExtras
    ) {
        final StartappExtras extras = new StartappExtras(localExtras, serverExtras, true);

        StartappConfig.initializeSdkIfNeeded(context, extras.getAppId());

        new StartappStaticNativeAd().loadAd(context, extras, listener);
        configuration.setCachedInitializationParameters(context, serverExtras);
    }

    static class StartappStaticNativeAd extends StaticNativeAd {
        @Nullable
        private NativeAdDetails adDetails;

        @Override
        public void prepare(@NonNull View view) {
            if (adDetails != null) {
                adDetails.registerViewForInteraction(view, null, new NativeAdDisplayListener() {
                    @Override
                    public void adHidden(NativeAdInterface nativeAdInterface) {
                        MoPubLog.log(AD_NETWORK_ID, DID_DISAPPEAR, LOG_TAG);
                    }

                    @Override
                    public void adDisplayed(NativeAdInterface nativeAdInterface) {
                        StartappStaticNativeAd.this.notifyAdImpressed();

                        MoPubLog.log(AD_NETWORK_ID, SHOW_SUCCESS, LOG_TAG);
                    }

                    @Override
                    public void adClicked(NativeAdInterface nativeAdInterface) {
                        StartappStaticNativeAd.this.notifyAdClicked();

                        MoPubLog.log(AD_NETWORK_ID, CLICKED, LOG_TAG);
                    }

                    @Override
                    public void adNotDisplayed(NativeAdInterface nativeAdInterface) {
                        MoPubLog.log(AD_NETWORK_ID, SHOW_FAILED, LOG_TAG);
                    }
                });
            }

            super.prepare(view);
        }

        @Override
        public void clear(@NonNull View view) {
            super.clear(view);

            if (adDetails != null) {
                adDetails.unregisterView();
            }
        }

        @Override
        public void destroy() {
            super.destroy();

            adDetails = null;
        }

        private void populateAdProperties(boolean isContentAd) {
            if (adDetails == null) {
                return;
            }

            setTitle(adDetails.getTitle());
            setText(adDetails.getDescription());
            setCallToAction(mapCallToAction(adDetails.getCampaignAction()));
            setStarRating((double) adDetails.getRating());

            if (!isContentAd) {
                setMainImageUrl(adDetails.getImageUrl());
                setIconImageUrl(adDetails.getSecondaryImageUrl());
            }
        }

        @NonNull
        private static String mapCallToAction(@NonNull StartAppNativeAd.CampaignAction action) {
            switch (action) {
                case OPEN_MARKET:
                    return "Install";
                case LAUNCH_APP:
                    return "Launch app";
            }
            return "Open";
        }

        void loadAd(
                @NonNull final Context context,
                @NonNull StartappExtras extras,
                @NonNull final CustomEventNativeListener listener
        ) {
            final StartAppNativeAd startappAds = new StartAppNativeAd(context);
            final NativeAdPreferences prefs = (NativeAdPreferences) extras.getAdPreferences();

            startappAds.loadAd(prefs, new AdEventListener() {
                @Override
                public void onReceiveAd(@NonNull Ad ad) {
                    final ArrayList<NativeAdDetails> ads = startappAds.getNativeAds();
                    if (ads != null && !ads.isEmpty()) {
                        adDetails = ads.get(0);

                        if (adDetails != null) {
                            populateAdProperties(prefs.isContentAd());

                            if (prefs.isContentAd()) {
                                MoPubLog.log(AD_NETWORK_ID, LOAD_SUCCESS, LOG_TAG);

                                listener.onNativeAdLoaded(StartappStaticNativeAd.this);
                            } else {
                                final List<String> imageUrls = new ArrayList<>(2);

                                if (!TextUtils.isEmpty(adDetails.getImageUrl())) {
                                    imageUrls.add(adDetails.getImageUrl());
                                }

                                if (!TextUtils.isEmpty(adDetails.getSecondaryImageUrl())) {
                                    imageUrls.add(adDetails.getSecondaryImageUrl());
                                }

                                preCacheImages(context, imageUrls, listener);
                            }
                        } else {
                            failed(listener);
                        }
                    } else {
                        failed(listener);
                    }
                }

                @Override
                public void onFailedToReceiveAd(@NonNull Ad ad) {
                    final String message = ad.getErrorMessage();
                    final NativeErrorCode errorCode = (message != null && (message.contains("204") || message.contains("Empty Response")))
                            ? NativeErrorCode.NETWORK_NO_FILL
                            : NativeErrorCode.UNSPECIFIED;

                    MoPubLog.log(AD_NETWORK_ID, LOAD_FAILED, LOG_TAG,
                            errorCode.getIntCode(),
                            errorCode);

                    listener.onNativeAdFailed(errorCode);
                }
            });

            MoPubLog.log(AD_NETWORK_ID, LOAD_ATTEMPTED, LOG_TAG);
        }

        private void preCacheImages(
                @NonNull Context context,
                @NonNull List<String> imageUrls,
                @NonNull final CustomEventNativeListener listener
        ) {
            NativeImageHelper.preCacheImages(context, imageUrls, new NativeImageHelper.ImageListener() {
                @Override
                public void onImagesCached() {
                    MoPubLog.log(AD_NETWORK_ID, LOAD_SUCCESS, LOG_TAG);

                    listener.onNativeAdLoaded(StartappStaticNativeAd.this);
                }

                @Override
                public void onImagesFailedToCache(@NonNull NativeErrorCode errorCode) {
                    MoPubLog.log(AD_NETWORK_ID, LOAD_FAILED, LOG_TAG,
                            errorCode.getIntCode(),
                            errorCode);

                    listener.onNativeAdFailed(errorCode);
                }
            });
        }

        private void failed(@NonNull CustomEventNativeListener listener) {
            MoPubLog.log(AD_NETWORK_ID, LOAD_FAILED, LOG_TAG,
                    NativeErrorCode.NETWORK_NO_FILL.getIntCode(),
                    NativeErrorCode.NETWORK_NO_FILL);

            listener.onNativeAdFailed(NativeErrorCode.NETWORK_NO_FILL);
        }
    }
}
