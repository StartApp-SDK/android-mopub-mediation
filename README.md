# android-mopub-mediation
## Enables you to serve Start.io (formerly StartApp) Ads in your android application using MoPub mediation network

### 1. Getting Started

The following manual assumes that you are already familiar with the MoPub Mediation Network and have already integrated the MoPub Android SDK into your application with at least one Ad Unit. 
Otherwise, please start by reading the following articles for a walk-through explanation of what mediation is, how to use the MoPub Mediation UI, and instructions on how to add MoPub mediation code into your app.
  * [MoPub mediation overview](https://developers.mopub.com/publishers/mediation/mopub-network-mediation)
  * [MoPub mediation android integration](https://developers.mopub.com/publishers/mediation/integrate-android)
  * [MoPub instructions](https://developers.mopub.com/publishers/android/integrate)
  
### 2. Adding Your Application to Your Start.io Developer's Account
1. Login into your [Start.io developer's account](https://portal.start.io/#/signin)
1. Add your application and get its App ID

### 3. Integrating the Start.io <-> MoPub Mediation Adapter
The easiest way is to use maven depencency, just add to your Gradle file following line
```
dependencies { 
    implementation 'com.startapp:inapp-sdk:4.+'
    implementation 'com.startapp:mopub-mediation:3.+'
}
```
But you might as well use [this source code](https://github.com/StartApp-SDK/android-admob-mediation) from Github and add it to your project

### 4. Adding a Custom Event
1. Login into your [MoPub account](https://app.mopub.com/login?next=/dashboard/)
1. On the top menu select "Orders"
1. Create new one pressing "Create Order" unless you already have prepared one
1. Select your order from the list
1. Press "New line item" button
1. Name it as you prefear and in "Type & priority" select "Network line item"
1. In the field "Network" select "Custom SDK network"
1. Fill in the appeared fields "Custom event class" and "Custom event data" regarding to your ad type:

Ad Type | Custom event class | Custom event data | Options
------- | ------------------ | ----------------- | -------
Fullscreen | com.mopub.mobileads.StartappAdapter | {"startappAppId":"your_id_from_portal", "adTag":"any_your_tag", "interstitialMode":"OVERLAY", "minCPM":0.03, "muteVideo":false} | interstitialMode can be OVERLAY, VIDEO or OFFERWALL
Banner/Medium Rectangle | com.mopub.mobileads.StartappAdapter | {"startappAppId":"your_id_from_portal", "adTag":"any_your_tag", "minCPM":0.03, "is3DBanner":false} | 
Rewarded | com.mopub.mobileads.StartappAdapter | {"startappAppId":"your_id_from_portal", "adTag":"any_your_tag", "minCPM":0.03, "muteVideo":false} |
Native | com.mopub.nativeads.StartappNative | {"startappAppId":"your_id_from_portal", "adTag":"any_your_tag", "minCPM":0.03, "nativeImageSize":"SIZE340X340", "nativeSecondaryImageSize":"SIZE72X72"} | nativeImageSize and nativeSecondaryImageSize can be any of SIZE72X72, SIZE100X100, SIZE150X150, SIZE340X340, SIZE1200X628(for main image only) | 

All parameters in the "custom event data" field are optional except the "startappAppId" which you must provide if you initialize start.io sdk from MoPub UI
You can also pass these parameters from your code using mopub's "setLocalExtras". But be aware that every parameter from "custom event data" will override same parameter which is set locally

Fullscreen example:
```java
final Map<String, Object> extras = new StartappExtras.Builder()
	.setAdTag("interstitialTagFromAdRequest")
	.setInterstitialMode(StartappExtras.Mode.OFFERWALL)
	.muteVideo()
	.setMinCPM(0.01)
	.toMap();

interstitial.setLocalExtras(extras);
interstitial.load(); 
```

Banner example:
```java
final Map<String, Object> extras = new StartappExtras.Builder()
	.setAdTag("bannerTagFromAdRequest")
	.enable3DBanner()
	.setMinCPM(0.01)
	.toMap();

bannerView.setLocalExtras(extras);
bannerView.loadAd(); 
}
```

Rewarded example:
```java
final StartappAdapter.Extras.LocalExtras extras = new StartappAdapter.Extras.Builder()
                .setAdTag("rewardedTagFromAdRequest")
                .muteVideo()
                .setMinCPM(0.01)
                .toMap();
				
MoPubRewardedVideos.loadRewardedVideo(getResources().getString(R.string.rewardedId), extras);
}
```

Native example:
```java
final Map<String, Object> extras = new StartappExtras.Builder()
    .setAdTag("nativeTagFromAdRequest")
    .setMinCPM(0.01)
    .setNativeImageSize(StartappExtras.Size.SIZE72X72)
    .setNativeSecondaryImageSize(StartappExtras.Size.SIZE150X150)
    .toMap();

moPubNative.setLocalExtras(extras);
moPubNative.makeRequest(requestParameters); 
}
```
#### If you need additional assistance you can take a look on our app example which works with this mediation adapter [here](https://github.com/StartApp-SDK/android-mopub-mediation-sample)
