package com.ibm.watson.developer_cloud.android.speech_common.v1;

import android.util.Log;

import java.net.URI;

/**
 * Created by mihui on 6/5/16.
 */
public class AuthConfiguration {
    private static final String TAG = AuthConfiguration.class.getSimpleName();

    public String basicAuthUsername;
    public String basicAuthPassword;
    public URI apiEndpoint;
    public String apiURL;

    public String token = null;
    protected ITokenProvider tokenProvider = null;

    // Indicates whether to opt out of data collection for the request sent over the connection.
    // If true, no data is collected; if false (the default), data is collected for the request and results.
    public boolean xWatsonLearningOptOut = false;

    /**
     * Set token provider
     * @param tokenProvider ITokenProvider
     */
    public void setTokenProvider(ITokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    /**
     * If the ITokenProvider is available
     * @return boolean
     */
    public boolean hasTokenProvider() {
        return this.tokenProvider != null;
    }

    /**
     * Request a token
     * @return String
     */
    public String requestToken(){
        return this.requestToken(false);
    }

    /**
     * Request a token with refresh cache option
     * @param refreshCache if set to true, the token will be automatically refreshed
     * @return String
     */
    public String requestToken(boolean refreshCache){
        if(this.token == null || refreshCache) {
            if(this.tokenProvider != null)
                this.token = this.tokenProvider.getToken();
            else
                Log.e(TAG, "NULL ITokenProvider");
        }
        return this.token;
    }
}
