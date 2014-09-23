package carnero.movement.network.foursquare;

import carnero.movement.common.Preferences;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;

public class Api {

    public static final String URL = "https://api.foursquare.com/v2";
    public static final String VERSION = "20140923";

    public static RestAdapter get() {
        return new RestAdapter.Builder()
            .setEndpoint(URL)
            .setLogLevel(RestAdapter.LogLevel.BASIC)
            .setRequestInterceptor(new ApiRequestInterceptor())
            .build();
    }

    // classes

    private static class ApiRequestInterceptor implements RequestInterceptor {

        @Override
        public void intercept(RequestFacade requestFacade) {
            final Preferences preferences = new Preferences();

            requestFacade.addQueryParam("v", VERSION);
            requestFacade.addQueryParam("oauth_token", preferences.getFoursquareToken());
        }
    }
}
