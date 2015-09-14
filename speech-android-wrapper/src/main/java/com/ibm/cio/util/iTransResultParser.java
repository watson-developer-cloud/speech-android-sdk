/**
 * Copyright IBM Corporation 2015
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

package com.ibm.cio.util;

import java.io.IOException;
import java.io.InputStream;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Xml;

public class iTransResultParser {

	// We don't use namespaces
	private static final String ns = null;
	private iTransResponse response;

	public iTransResponse parse(String xml) throws XmlPullParserException, IOException {

		response = new iTransResponse();

		InputStream in = new java.io.ByteArrayInputStream(xml.getBytes("utf-8"));
		try {
			XmlPullParser parser = Xml.newPullParser();
			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
			parser.setInput(in,null);
			parser.nextTag();
			readApi(parser);
			
			return response;
		} finally {
			in.close();
		}

	}
	/*
 <?xml version="1.0" encoding="UTF-8"?>
 <api><method>SynTranscription</method><success>true</success><description></description><retval><job-id>9996-ab48-b04e-d2b8</job-id><transcription></transcription></retval></api>
	 */
	private void readApi(XmlPullParser parser) throws XmlPullParserException, IOException {

		parser.require(XmlPullParser.START_TAG, ns, "api");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			// Starts by looking for the entry tag
			if (name.equals("success")) {
				readSuccess(parser);
			} 
			else if(name.equals("retval")){
				readRetval(parser);
			}else {
				skip(parser);
			}
		}  
	}

	/**
	 * skip - skip an element and don't parse it
	 * @param parser
	 * @throws XmlPullParserException
	 * @throws IOException
	 */
	private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
		if (parser.getEventType() != XmlPullParser.START_TAG) {
			throw new IllegalStateException();
		}
		int depth = 1;
		while (depth != 0) {
			switch (parser.next()) {
			case XmlPullParser.END_TAG:
				depth--;
				break;
			case XmlPullParser.START_TAG:
				depth++;
				break;
			}
		}
	}

	/***
	 * readText - read the value inside a tag
	 * @param parser
	 * @return
	 * @throws IOException
	 * @throws XmlPullParserException
	 */
	private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
		String result = "";
		if (parser.next() == XmlPullParser.TEXT) {
			result = parser.getText();
			System.out.println("text is "+result);
			parser.nextTag();
		}
		return result;
	}

	private void readSuccess(XmlPullParser parser) throws IOException, XmlPullParserException {
		parser.require(XmlPullParser.START_TAG, ns, "success");
		response.setSuccess(readText(parser));
		parser.require(XmlPullParser.END_TAG, ns, "success");

	}

	private void readTranscription(XmlPullParser parser) throws IOException, XmlPullParserException {
		parser.require(XmlPullParser.START_TAG, ns, "transcription");
		response.setTranscription(readText(parser));
		parser.require(XmlPullParser.END_TAG, ns, "transcription");
	}

	private void readJobId(XmlPullParser parser) throws IOException, XmlPullParserException {
		parser.require(XmlPullParser.START_TAG, ns, "job-id");
		response.setJobId(readText(parser));
		parser.require(XmlPullParser.END_TAG, ns, "job-id");
	}

	private void readRetval(XmlPullParser parser) throws XmlPullParserException, IOException {


		parser.require(XmlPullParser.START_TAG, ns, "retval");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			// Starts by looking for the entry tag
			if (name.equals("transcription")) {
				readTranscription(parser);
			} 
			else if(name.equals("job-id")){
				readJobId(parser);
			}else {
				skip(parser);
			}
		}  
	}
}
