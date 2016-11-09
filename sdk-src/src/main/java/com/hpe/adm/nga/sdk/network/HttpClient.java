package com.hpe.adm.nga.sdk.network;

import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.hpe.adm.nga.sdk.Octane;
import com.hpe.adm.nga.sdk.authorisation.Authorisation;
import com.hpe.adm.nga.sdk.exception.OctaneException;
import com.hpe.adm.nga.sdk.model.ErrorModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.List;
import java.util.Optional;

/**
 * HTTP Client
 *
 * Created by leufl on 2/11/2016.
 */
public class HttpClient {

    //Constants
    private static final String OAUTH_AUTH_URL = "/authentication/sign_in";
    private static final String LOGGER_REQUEST_FORMAT = "Request: %s - %s - %s";
    private static final String LOGGER_RESPONSE_FORMAT = "Response: %d - %s - %s";
    private static final String LWSSO_COOKIE_KEY = "LWSSO_COOKIE_KEY";
    private static final String SET_COOKIE = "set-cookie";
    private static final String HPE_CLIENT_TYPE = "HPECLIENTTYPE";
    private static final String HPE_MQM_UI = "HPE_MQM_UI";

    private Logger logger = LogManager.getLogger(Octane.class.getName());
    private com.google.api.client.http.HttpRequestFactory requestFactory;
    private String lwssoValue = "";
    private String urlDomain = "";

    private HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    /**
     * Creates an HTTP request factory using the url and authorisation.
     * @param urlDomain
     * @param authorisation
     */
    public HttpRequestFactory getRequestFactory(String urlDomain, Authorisation authorisation) {
        this.urlDomain = urlDomain;
        requestFactory = HTTP_TRANSPORT
                .createRequestFactory(request -> {
                    request.setUnsuccessfulResponseHandler(new HttpUnsuccessfulResponseHandler() {
                        @Override
                        public boolean handleResponse(com.google.api.client.http.HttpRequest httpRequest, com.google.api.client.http.HttpResponse httpResponse, boolean b) throws IOException {
                            if (httpResponse.getStatusCode() == HttpStatusCodes.STATUS_CODE_UNAUTHORIZED) {
                                return refreshToken();
                            }

                            return false;
                        }

                        /**
                         * refresh the LWSSO token and retry
                         * @return true if token is renewed and need retry and false otherwise
                         */
                        private boolean refreshToken() {
                            logger.debug("Session is expired, renew token and retry.");
                            GenericUrl genericUrl = new GenericUrl(urlDomain + OAUTH_AUTH_URL);
                            try{
                                // clear lwssoValue for re-login
                                lwssoValue = "";
                                logger.debug("Login to renew token.");
                                com.google.api.client.http.HttpRequest httpRequest = requestFactory.buildPostRequest(genericUrl, null);
                                logger.debug(String.format(LOGGER_REQUEST_FORMAT,httpRequest.getRequestMethod(),urlDomain + OAUTH_AUTH_URL,httpRequest.getHeaders().toString()));
                                HttpResponse response = new HttpResponse(httpRequest.execute());
                                logger.debug(String.format(LOGGER_RESPONSE_FORMAT,response.getStatusCode(),response.getStatusMessage(),response.getHeaders().toString()));

                                // refresh Cookies keys
                                if (response.isSuccessStatusCode()) {
                                    HttpHeaders hdr1 = response.getHeaders();
                                    if (updateLWSSOCookieValue(hdr1)) {
                                        String newCookie = LWSSO_COOKIE_KEY + "=" + lwssoValue;
                                        request.getHeaders().setCookie(newCookie);
                                        logger.debug("Retry with updated token.");
                                        logger.debug(String.format(LOGGER_REQUEST_FORMAT, request.getRequestMethod(), request.getUrl().toString(), request.getHeaders().toString()));

                                        // return true for retrying the origin request
                                        return true;
                                    }
                                }
                            }
                            catch (Exception e){
                                ErrorModel errorModel =  new ErrorModel(e.getMessage());
                                logger.error("Error in contacting server: ", e);
                                throw new OctaneException(errorModel);
                            }

                            return false;
                        }
                    });

                    // BasicAuthentication needed only in first initialization
                    if (lwssoValue != null && lwssoValue.isEmpty())
                    {
                        authorisation.executeAuthorisation(new HttpRequest((request)));
                        // username and password should be transient.
                    }
                    else
                    {
                        // retrieve new LWSSO in response if any
                        HttpHeaders responseHeaders = request.getResponseHeaders();
                        String lastResponseCookie;
                        if (updateLWSSOCookieValue(responseHeaders)) {
                            // renew LWSSO cookie
                            lastResponseCookie = LWSSO_COOKIE_KEY + "=" + lwssoValue;
                        } else {
                            // use current request cookie or set from current lwssoValue
                            lastResponseCookie = request.getHeaders().getCookie() != null ? request.getHeaders().getCookie() : LWSSO_COOKIE_KEY + "=" + lwssoValue;
                        }

                        request.getHeaders().setCookie(lastResponseCookie);
                        request.getHeaders().set(HPE_CLIENT_TYPE, HPE_MQM_UI);
                    }
                });
        return new HttpRequestFactory(requestFactory);
    }

    /**
     * @return - Returns true if the authentication succeeded, false otherwise.
     */
    public boolean checkAuthentication() {
        GenericUrl genericUrl = new GenericUrl(urlDomain + OAUTH_AUTH_URL);
        try{
            com.google.api.client.http.HttpRequest httpRequest = requestFactory.buildPostRequest(genericUrl, null);
            logger.debug(String.format(LOGGER_REQUEST_FORMAT, httpRequest.getRequestMethod(), urlDomain + OAUTH_AUTH_URL, httpRequest.getHeaders().toString()));
            HttpResponse response = new HttpResponse(httpRequest.execute());
            logger.debug(String.format(LOGGER_RESPONSE_FORMAT,response.getStatusCode(),response.getStatusMessage(),response.getHeaders().toString()));

            // Initialize Cookies keys
            if (response.isSuccessStatusCode()) {
                HttpHeaders hdr1 = response.getHeaders();
                updateLWSSOCookieValue(hdr1);

                // TBD - Remove after debugging
					/* for (HttpCookie ck : Cookies) {

						 if (ck.getName().equals(HPSSO_COOKIE_CSRF))
							 hppsValue = ck.getValue();

						 if (ck.getName().equals(LWSSO_COOKIE_KEY))
							 lwssoValue = ck.getValue();

					 }*/

                if(lwssoValue != null && !lwssoValue.isEmpty()) {
                    return true;
                }
            }
        }
        catch (Exception e){

            ErrorModel errorModel =  new ErrorModel(e.getMessage());
            logger.error("Error in contacting server: ", e);
            throw new OctaneException(errorModel);
        }
        return false;
    }

    /**
     * Retrieve new cookie from set-cookie header
     * @param headers
     * @return true if LWSSO cookie is renewed
     */
    private boolean updateLWSSOCookieValue(HttpHeaders headers) {
        boolean renewed = false;
        List<String> strHPSSOCookieCsrf1 = headers.getHeaderStringValues(SET_COOKIE);
        if (strHPSSOCookieCsrf1.isEmpty()) {
            return false;
        }

            /* Following code failed to parse set-cookie to get LWSSO cookie due to cookie version, check RFC 2965
            String strCookies = strHPSSOCookieCsrf1.toString();
            List<HttpCookie> Cookies = java.net.HttpCookie.parse(strCookies.substring(1, strCookies.length()-1));
            lwssoValue = Cookies.stream().filter(a -> a.getName().equals(LWSSO_COOKIE_KEY)).findFirst().get().getValue();*/
        for (String strCookie :
                strHPSSOCookieCsrf1) {
            List<HttpCookie> cookies = HttpCookie.parse(strCookie);
            Optional<HttpCookie> lwssoCookie = cookies.stream().filter(a -> a.getName().equals(LWSSO_COOKIE_KEY)).findFirst();
            if (lwssoCookie.isPresent()) {
                lwssoValue = lwssoCookie.get().getValue();
                renewed = true;
                break;
            }
        }

        return renewed;
    }
}