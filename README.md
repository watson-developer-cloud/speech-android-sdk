Watson Speech Android SDK
=====================

The Watson Speech SDK for the Android platform enables an easy and lightweight interaction with the IBM's Watson Speech-To-Text (STT) and Text-To-Speech (TTS) services in Bluemix. The SDK includes support for recording and streaming audio in real time to the STT service while receiving a transcript of the audio as you speak. This project includes an example application that showcases the interaction with both the STT and TTS Watson services in the cloud.

The current version of the SDK uses a minSdkVersion of 9, while the example application uses a minSdkVersion of 16.


Table of Contents
-----------------
* [Watson Developer Cloud Speech APIs][wdc]

    * [Installation](#installation)

    * [Getting Credentials](#getting-credentials)
    
    * [A Quick Start Guide](#a-quick-start-guide)

    * [Speech To Text](#speech-to-text)
        * [Implement the delegates](#implement-the-speechdelegate-and-speechrecorderdelegate-in-the-mainactivity)
    	* [Instantiate the SpeechToText instance](#instantiate-the-speechtotext-instance)
    	* [List supported models](#get-a-list-of-models-supported-by-the-service)
    	* [Get model details](#get-details-of-a-particular-model)
    	* [Start audio transcription](#start-audio-transcription)
    	* [End audio transcription](#end-audio-transcription)

	* [Text To Speech](#text-to-speech)
    	* [Instantiate the TextToSpeech instance](#instantiate-the-texttospeech-instance)
    	* [List supported voices](#get-a-list-of-voices-supported-by-the-service)
    	* [Generate and play audio](#generate-and-play-audio)


Installation
------------

**Using the library**

1. Download the [speech-android-wrapper.aar](https://github.com/watson-developer-cloud/speech-android-sdk/releases/download/watsonsdk.aar/speech-android-wrapper.aar)
2. Once unzipped drag the speech-android-wrapper.aar file into your Android Studio project view under the libs folder.
3. Go to build.gradle file of your app, then set the dependencies as below:

```
    dependencies {
        compile fileTree(dir: 'libs', include: ['*.jar'])
        compile (name:'speech-android-wrapper',ext:'aar')
        compile 'com.android.support:appcompat-v7:22.0.0'
    }
    repositories{
        flatDir{
            dirs 'libs'
        }
    }
```

4. Clean and run the Android Studio project


Getting credentials
--------------------

1. Create an account on [Bluemix](https://console.ng.bluemix.net) if you have not already.
2. Follow instructions at http://www.ibm.com/smarterplanet/us/en/ibmwatson/developercloud/doc/getting_started/gs-credentials.shtml to get service credentials.

A Quick Start Guide
--------------------

To get started, you can also take a look at a [quick start guide](https://github.com/watson-developer-cloud/speech-android-sdk/issues/7#issue-130902950) created by [@KeyOnTech](https://github.com/KeyOnTech).

Speech To Text
===============

Implement the ISpeechToTextDelegate in the MainActivity
-------------------------------------------------

These delegates implement the callbacks when a response from the server is received or when the recorder is sending back the audio data.

```java
   public class MainActivity extends Activity implements ISpeechToTextDelegate{}
```

Instantiate the SpeechToText instance
-------------------------------------

**Enabling audio compression**

By default audio sent to the server is uncompressed PCM encoded data, compressed audio using the Opus codec can be enabled.

```java
   STTConfiguration sConfig = new STTConfiguration(STTConfiguration.AUDIO_FORMAT_OGGOPUS, STTConfiguration.SAMPLE_RATE_OGGOPUS);

   sConfig.basicAuthUsername = "<your-username>";
   sConfig.basicAuthPassword = "<your-password>";
```

**Alternatively pass a token factory object to be used by the SDK to retrieve authentication tokens to authenticate against the STT service**

```java
   SpeechToText.sharedInstance().setTokenProvider(new MyTokenProvider(this.strSTTTokenFactoryURL));
```

Then instantiate SpeechToText instance, the ISpeechToTextDelegate is required now:

```java
   SpeechToText.sharedInstance().initWithConfig(sConfig, this);
```

Get a list of models supported by the service
---------------------------------------------

```java
   SpeechToText.sharedInstance().getModels();
```

Get details of a particular model
---------------------------------

```java
   SpeechToText.sharedInstance().getModelInfo("en-US_BroadbandModel");
```

Pick the model to be used
-------------------------

```java
   SpeechToText.sharedInstance().setModel("en-US_BroadbandModel");
```

Start Audio Transcription
-------------------------

```java
   SpeechToText.sharedInstance().recognize();
```

**Delegate methods to receive messages from the SDK**

```
    @Override
    public void onOpen() {
        // the  connection to the STT service is successfully opened 
    }
    
    @Override
    public void onError(String error) {
        // error interacting with the STT service
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        // the connection with the STT service was just closed
    }

    @Override
    public void onMessage(String message) {
        // a message comes from the STT service with recognition results 
    }

    @Override
    public void onBegin() {
        // only called after listening state is returned
    }

    @Override
    public void onAmplitude(double amplitude, double volume) {
        // Receive the data of amplitude and volume, the amplitude is calculated from the audio data buffer, and the volume (in dB) is calculated based on it
    }
```

End audio transmission
----------------------

```java
   SpeechToText.sharedInstance().endTransmission();
```

End audio recording
-------------------

```java
   SpeechToText.sharedInstance().stopRecording();
```


Text To Speech
==============

Implement the ITextToSpeechDelegate in the MainActivity
-----------------------------------------------------

These delegates implement the callbacks when a response from the server is received, now the ITextToSpeechDelegate is required

```java
   public class MainActivity extends Activity implements ITextToSpeechDelegate{}
```

Instantiate the TextToSpeech instance
-------------------------------------

```java
   TTSConfiguration tConfig = new TTSConfiguration();
   tConfig.basicAuthUsername = "<your-username>";
   tConfig.basicAuthPassword = "<your-password>";
   tConfig.appContext = this.getActivity().getApplicationContext();

   TextToSpeech.sharedInstance().initWithConfig(tConfig, this);
```

**Alternatively pass a token factory object to be used by the SDK to retrieve authentication tokens to authenticate against the TTS service**

```
   tConfig.setTokenProvider.setTokenProvider(new MyTokenProvider(this.strTTSTokenFactoryURL));
```


Get a list of voices supported by the service
---------------------------------------------

```
   TextToSpeech.sharedInstance().getVoices();
```

Pick the voice to be used
-------------------------

```
   TextToSpeech.sharedInstance().setVoice("en-US_MichaelVoice");
```

Generate and play audio
-----------------------

```java
   TextToSpeech.sharedInstance().synthesize("Hello World!");
```

or use customization ID

```java
   TextToSpeech.sharedInstance().synthesize("Hello World!", "<your-customization-id>");
```

**Delegate methods to receive messages from the SDK**

```
    @Override
    public void onTTSStart() {
        // Start sending request to service
    }

    @Override
    public void onTTSWillPlay() {
        // The audio data is fully downloaded and ready for play
    }

    @Override
    public void onTTSStopped() {
        // Player is stopped 
    }

    @Override
    public void onTTSError(int statusCode) {
        // Error occurs
    }
```


Common issues
-------------


[wdc]: http://www.ibm.com/smarterplanet/us/en/ibmwatson/developercloud/apis/#!/speech-to-text
