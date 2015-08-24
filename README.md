Watson Speech Android SDK
=====================

An SDK for Android mobile applications enabling use of the Bluemix Watson Speech To Text and Text To Speech APIs from [Watson Developer Cloud][wdc]

The SDK include support for recording and streaming audio and receiving a transcript of the audio in response.


Table of Contents
-----------------
* [Watson Developer Cloud Speech APIs][wdc]

    * [Installation](#installation)

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

1. Download the [watsonsdk.aar.zip](https://git.hursley.ibm.com/w3bluemix/WatsonAndroidSpeechSDK/blob/master/speech-android-wrapper/build/outputs/aar/watsonsdk.aar.zip) and unzip it somewhere convenient
2. Once unzipped drag the watsonsdk.aar file into your Android Studio project view under the libs folder.
3. Go to build.gradle file of your app, then set the dependencies as below:
```
    dependencies {
        compile fileTree(dir: 'libs', include: ['*.jar'])
        compile (name:'watsonsdk',ext:'aar')
        compile 'com.android.support:appcompat-v7:22.0.0'
    }
    repositories{
        flatDir{
            dirs 'libs'
        }
    }
```
4. Clean and run the Android Studio project

#Speech To Text
===============

Implement the SpeechDelegate and SpeechRecorderDelgate in the MainActivity
--------------------------------------------------------------------------

These delgates implement the callbacks when a response from the server is recieved or when the recording is completed.

```
   public class MainActivity extends Activity implements SpeechDelegate, SpeechRecorderDelegate{}
```

Instantiate the SpeechToText instance
-------------------------------------

```
   SpeechToText.sharedInstance().initWithContext(this.getHost(), this.getApplicationContext());
```

**Set the Credentials and the delegate**

```
   SpeechToText.sharedInstance().setCredentials(this.USERNAME,this.PASSWORD);
   SpeechToText.sharedInstance().setDelegate(this);
```

**Alternatively pass a token factory object to be used by the SDK to retrieve authentication tokens to authenticate against the STT service**

```
   SpeechToText.sharedInstance().setTokenProvider(new MyTokenProvider(this.strSTTTokenFactoryURL));
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
   SpeechToText.sharedInstance().setRecorderDelegate(this);
```

**Delegate function to receive messages from the sdk**

```
	@Override
	public void receivedMessage(int code, QueryResult result) {
		switch(code){
			case SpeechDelegate.OPEN:
				Log.i(TAG, "################ receivedMessage.Open");
			break;
			case SpeechDelegate.CLOSE:
				Log.i(TAG, "################ receivedMessage.Close"); // Final results
				break;
			case SpeechDelegate.ERROR:
				Log.e(TAG, result.getTranscript());
				break;
			case SpeechDelegate.MESSAGE:
				displayResult(result.getStatusCode(), result.getTranscript()); // Instant results
				break;
	
```

End Audio Transcription
------------------------------

By default the SDK uses Voice Activated Detection (VAD) to detect when a user has stopped speaking, this can be disabled with [SpeechToText.sharedInstance().setUseVAD(false);]

```
   SpeechRecognition.sharedInstance().stopRecording();
```

Receive speech power levels during the recognize
------------------------------

```
   ToDo
```


Text To Speech
==============

Instantiate the TextToSpeech instance
------------------------------

```
   TextToSpeech.sharedInstance().initWithContext(this.getHost(TTS_URL), this.getApplicationContext());
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