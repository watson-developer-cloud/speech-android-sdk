Watson Speech Android SDK
=====================

An SDK for Android mobile applications enabling use of the Bluemix Watson Speech To Text and Text To Speech APIs from [Watson Developer Cloud][wdc]

The SDK include support for recording and streaming audio and receiving a transcript of the audio in response.


Table of Contents
-----------------
* [Watson Developer Cloud Speech APIs][wdc]

    * [Installation](#installation)
    * [Getting Started](#getting started)

Installation
------------

**Using the library**

1. Download the [watsonsdk.aar.zip]() and unzip it somewhere convenient
2. Once unzipped drag the watsonsdk.aar file into your Android Studio project view under the libs folder.
3. Go to build.gradle file of your app, then set the dependencies as below:
```
dependencies {
    compile fileTree(dir: 'libs', include: ['*.jar'])
    compile project(name:'watsonsdk',ext:'aar')
    compile 'com.android.support:appcompat-v7:22.0.0'
}

repositories{
    flatDir{
        dirs 'libs'
    }
}
```
4. Clean and run the Android Studio project

Getting started
--------------

**Implement the SpeechDelegate and SpeechRecorderDelgate in the MainActivity
These delgates implement the callbacks when a response from the server is recieved or when the recording is completed.
```
public class MainActivity extends Activity implements SpeechDelegate, SpeechRecorderDelegate{}

```
**Instantiate the SpeechToText shared instance with the appplication context and the host URL**
```
SpeechToText.sharedInstance().initWithContext(this.getHost(), this.getApplicationContext());
```

**Set the Credentials and the delegate**
```
SpeechToText.sharedInstance().setUsername(this.USERNAME);
SpeechToText.sharedInstance().setPassword(this.PASSWORD);
SpeechToText.sharedInstance().setDelegate(this);
```

**Get a list of models supported by the service**

```
ToDo
```


**Get details of a particular model**
```
ToDo
```

**Start Audio Transcription**
```
SpeechToText.sharedInstance().recognize();
SpeechToText.sharedInstance().setRecorderDelegate(this);
```

**Delegate function to recieve messages from the sdk
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

**End Audio Transcription**

By default the SDK uses Voice Activated Detection (VAD) to detect when a user has stopped speaking, this can be disabled with [        SpeechToText.sharedInstance().setUseVAD(false);]
```
SpeechRecognition.sharedInstance().stopRecording();
```


**Receive speech power levels during the recognize**

```
ToDo
```

Common issues
-------------


[wdc]: http://www.ibm.com/smarterplanet/us/en/ibmwatson/developercloud/apis/#!/speech-to-text
