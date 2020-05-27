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
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.DataKeys;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Views;
import com.startapp.sdk.ads.banner.Banner;
import com.startapp.sdk.ads.banner.BannerBase;
import com.startapp.sdk.ads.banner.BannerListener;
import com.startapp.sdk.ads.banner.Mrec;
import com.startapp.sdk.ads.banner.banner3d.Banner3D;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.WILL_LEAVE_APPLICATION;
import static com.mopub.mobileads.StartappConfig.AD_NETWORK_ID;

public class StartappBanner extends CustomEventBanner {
    private static final String LOG_TAG = StartappBanner.class.getSimpleName();

    @NonNull
    private final StartappConfig configuration = new StartappConfig();

    @Nullable
    private FrameLayout containerView;

    @Override
    protected void loadBanner(
            @NonNull Context context,
            @NonNull final CustomEventBannerListener listener,
            @Nullable Map<String, Object> localExtras,
            @Nullable Map<String, String> serverExtras
    ) {
        if (!(context instanceof Activity)) {
            listener.onBannerFailed(MoPubErrorCode.UNSPECIFIED);
            return;
        }

        final Integer adWidth;
        final Integer adHeight;

        if (localExtras != null && !localExtras.isEmpty()) {
            adWidth = (Integer) localExtras.get(DataKeys.AD_WIDTH);
            adHeight = (Integer) localExtras.get(DataKeys.AD_HEIGHT);
        } else {
            failed(listener);
            return;
        }

        if (adWidth == null || adHeight == null) {
            failed(listener);
            return;
        }

        containerView = new FrameLayout(context);

        final BannerListener loadListener = new BannerListener() {
            @Override
            public void onReceiveAd(@NonNull View view) {
                MoPubLog.log(AD_NETWORK_ID, LOAD_SUCCESS, LOG_TAG);
                MoPubLog.log(AD_NETWORK_ID, SHOW_ATTEMPTED, LOG_TAG);

                listener.onBannerLoaded(containerView);
            }

            @Override
            public void onFailedToReceiveAd(@NonNull View view) {
                failed(listener);
            }

            @Override
            public void onImpression(@NonNull View view) {
                MoPubLog.log(AD_NETWORK_ID, SHOW_SUCCESS, LOG_TAG);

                listener.onBannerImpression();
            }

            @Override
            public void onClick(@NonNull View view) {
                MoPubLog.log(AD_NETWORK_ID, CLICKED, LOG_TAG);
                MoPubLog.log(AD_NETWORK_ID, WILL_LEAVE_APPLICATION, LOG_TAG);

                listener.onBannerClicked();
                listener.onLeaveApplication();
            }
        };

        final BannerBase banner = loadBannerAd(context, adWidth, adHeight, localExtras, serverExtras, loadListener);
        // force banner to calculate its view size
        containerView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        containerView.addView(banner, new FrameLayout.LayoutParams(
                dpToPx(context, adWidth),
                dpToPx(context, adHeight),
                Gravity.CENTER));

        configuration.setCachedInitializationParameters(context, serverExtras);
    }

    @Override
    protected void onInvalidate() {
        Views.removeFromParent(containerView);
    }

    protected boolean isMediumRectangle() {
        return false;
    }

    private static int dpToPx(@NonNull Context context, int dp) {
        final DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return (int) ((dp * metrics.density) + 0.5);
    }

    private void failed(@NonNull CustomEventBannerListener listener) {
        MoPubLog.log(AD_NETWORK_ID, LOAD_FAILED, LOG_TAG,
                MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                MoPubErrorCode.NETWORK_NO_FILL);

        listener.onBannerFailed(MoPubErrorCode.NETWORK_NO_FILL);
    }

    @NonNull
    private BannerBase loadBannerAd(
            @NonNull Context context,
            int desirableWidthDp,
            int desirableHeightDp,
            @Nullable Map<String, Object> localExtras,
            @Nullable Map<String, String> serverExtras,
            @NonNull BannerListener listener
    ) {
        final StartappExtras extras = new StartappExtras(localExtras, serverExtras, false);

        StartappConfig.initializeSdkIfNeeded(context, extras.getAppId());

        setAutomaticImpressionAndClickTracking(false);

        final Activity activity = (Activity) context;
        final BannerBase result;
        if (isMediumRectangle()) {
            result = new Mrec(activity, extras.getAdPreferences(), listener);
        } else if (extras.is3DBanner()) {
            result = new Banner3D(activity, extras.getAdPreferences(), listener);
        } else {
            result = new Banner(activity, extras.getAdPreferences(), listener);
        }

        result.loadAd(desirableWidthDp, desirableHeightDp);

        MoPubLog.log(AD_NETWORK_ID, LOAD_ATTEMPTED, LOG_TAG);

        return result;
    }
}
