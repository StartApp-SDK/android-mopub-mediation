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

import android.location.Location;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.MediationSettings;
import com.startapp.sdk.ads.nativead.NativeAdPreferences;
import com.startapp.sdk.adsbase.StartAppAd;
import com.startapp.sdk.adsbase.model.AdPreferences;

import java.util.HashMap;
import java.util.Map;

@Keep
public class StartappExtras {
    private static final String LOG_TAG = StartappExtras.class.getSimpleName();

    private static final String AD_TAG = "adTag";
    private static final String INTERSTITIAL_MODE = "interstitialMode";
    private static final String MIN_CPM = "minCPM";
    private static final String MUTE_VIDEO = "muteVideo";
    private static final String IS_3D_BANNER = "is3DBanner";
    private static final String NATIVE_IMAGE_SIZE = "nativeImageSize";
    private static final String NATIVE_SECONDARY_IMAGE_SIZE = "nativeSecondaryImageSize";
    private static final String APP_ID = "startappAppId";

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
        SIZE1200X628,
        SIZE320X480,
        SIZE480X320
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

    public StartappExtras(@Nullable Map<String, Object> localExtras, @Nullable Map<String, String> serverExtras, boolean isNative) {
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
            nativeImageSize = (StartappExtras.Size)localExtras.get(NATIVE_IMAGE_SIZE);
            nativeSecondaryImageSize = (StartappExtras.Size)localExtras.get(NATIVE_SECONDARY_IMAGE_SIZE);

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
                final StartappExtras.Mode srcMode = (StartappExtras.Mode) localExtras.get(INTERSTITIAL_MODE);
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
                    nativeImageSize = StartappExtras.Size.valueOf(name);
                }
            }

            if (serverExtras.containsKey(NATIVE_SECONDARY_IMAGE_SIZE)) {
                final String name = serverExtras.get(NATIVE_SECONDARY_IMAGE_SIZE);
                if (name != null) {
                    nativeSecondaryImageSize = StartappExtras.Size.valueOf(name);
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

    public static class LocalExtras extends HashMap<String, Object> implements MediationSettings {}

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
