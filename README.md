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
    	* [Start Audio Transcription](#start-audio-transcription)
    	* [End Audio Transcription](#end-audio-transcription)
    	* [Speech power levels](#receive-speech-power-levels-during-the-recognize)

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

Implement the ISpeechDelegate and SpeechRecorderDelegate in the MainActivity
--------------------------------------------------------------------------

These delegates implement the callbacks when a response from the server is received or when the recorder is sending back the audio data. SpeechRecorderDelegate is optional.

```
   public class MainActivity extends Activity implements ISpeechDelegate{}
```

Or with SpeechRecorderDelegate

```
   public class MainActivity extends Activity implements ISpeechDelegate, SpeechRecorderDelegate{}
```

Instantiate the SpeechToText instance
-------------------------------------

```
   SpeechToText.sharedInstance().initWithContext(new URI("wss://stream.watsonplatform.net/speech-to-text/api"), this.getApplicationContext(), new SpeechConfiguration());
```

**Enabling audio compression**

By default audio sent to the server is uncompressed PCM encoded data, compressed audio using the Opus codec can be enabled.
```
   SpeechToText.sharedInstance().initWithContext(this.getHost(STT_URL), this.getApplicationContext(), new SpeechConfiguration(SpeechConfiguration.AUDIO_FORMAT_OGGOPUS));
```
Or this way:
```
    // Configuration
    SpeechConfiguration sConfig = new SpeechConfiguration(SpeechConfiguration.AUDIO_FORMAT_OGGOPUS);
    // STT
    SpeechToText.sharedInstance().initWithContext(this.getHost(STT_URL), this.getApplicationContext(), sConfig);
```

**Set the Credentials and the delegate**

```
   SpeechToText.sharedInstance().setCredentials(this.USERNAME,this.PASSWORD);
   SpeechToText.sharedInstance().setDelegate(this);
```

**Alternatively pass a token factory object to be used by the SDK to retrieve authentication tokens to authenticate against the STT service**

```
   SpeechToText.sharedInstance().setTokenProvider(new MyTokenProvider(this.strSTTTokenFactoryURL));
   SpeechToText.sharedInstance().setDelegate(this);
```

Get a list of models supported by the service
------------------------------

```   
   JSONObject models = getModels();
```

Get details of a particular model
------------------------------

```
   JSONObject model = getModelInfo("en-US_BroadbandModel");
```

Pick the model to be used
------------------------

```
   SpeechToText.sharedInstance().setModel("en-US_BroadbandModel");
```

Start Audio Transcription
------------------------------

```
   SpeechToText.sharedInstance().recognize();
```

If you implemented SpeechRecorderDelegate, and needs to process the audio data which is recorded, you can use set the delegate.
```
   SpeechToText.sharedInstance().recognize();
   SpeechToText.sharedInstance().setRecorderDelegate(this);
```

**Delegate methods to receive messages from the sdk**

```
    public void onOpen() {
        // the  connection to the STT service is successfully opened 
    }

    public void onError(String error) {
    	// error interacting with the STT service
    }

    public void onClose(int code, String reason, boolean remote) {
        // the connection with the STT service was just closed
    }

    public void onMessage(String message) {
        // a message comes from the STT service with recognition results 
    }	
```

End Audio Transcription
------------------------------

```
   SpeechRecognition.sharedInstance().stopRecording();
```

Receive speech power levels during the recognize
------------------------------
The amplitude is calculated from the audio data buffer, and the volume (in dB) is calculated based on it.

```
    @Override
    public void onAmplitude(double amplitude, double volume) {
        // your code here
    }
```


Text To Speech
==============

Instantiate the TextToSpeech instance
------------------------------

```
   TextToSpeech.sharedInstance().initWithContext(this.getHost(TTS_URL));
```

**Set the Credentials**

```
   TextToSpeech.sharedInstance().setCredentials(this.USERNAME,this.PASSWORD);
```

**Alternatively pass a token factory object to be used by the SDK to retrieve authentication tokens to authenticate against the TTS service**

```
   TextToSpeech.sharedInstance().setTokenProvider(new MyTokenProvider(this.strTTSTokenFactoryURL));
```

Get a list of voices supported by the service
------------------------------

```
   TextToSpeech.sharedInstance().voices();
```

Pick the voice to be used 
---------------------------------------------------

```
   TextToSpeech.sharedInstance().setVoice("en-US_MichaelVoice");
```

Generate and play audio
------------------------------

```
  TextToSpeech.sharedInstance().synthesize(ttsText);
```

Common issues
-------------


[wdc]: http://www.ibm.com/smarterplanet/us/en/ibmwatson/developercloud/apis/#!/speech-to-text
