package carnero.movement.network.foursquare;

import java.util.ArrayList;
import java.util.List;

import carnero.movement.model.Checkin;
import retrofit.Callback;
import retrofit.http.GET;
import retrofit.http.Query;

public interface Checkins {

    public static final String SORT_NEWEST = "newestfirst";
    public static final String SORT_OLDEST = "oldestfirst";

    @GET("/users/self/checkins")
    public void checkins(
        @Query("afterTimestamp") long afterTimestamp,
        @Query("sort") String sort,
        @Query("limit") int limit,
        Callback<CheckinsResponse> callback
    );

    public static class CheckinsResponse {

        public Response response;

        public List<Checkin> getCheckins() {
            final List<Checkin> checkins = new ArrayList<Checkin>();

            for (ResponseItem item : response.checkins.items) {
                checkins.add(parseCheckin(item));
            }

            return checkins;
        }

        public static Checkin parseCheckin(ResponseItem item) {
            final Checkin checkin = new Checkin();

            checkin.checkinId = item.id;
            checkin.createdAt = item.createdAt;
            checkin.shout = item.shout;
            checkin.name = item.venue.name;
            checkin.latitude = item.venue.location.lat;
            checkin.longitude = item.venue.location.lng;

            for (ResponseCategory category : item.venue.categories) {
                if (category.primary) {
                    checkin.iconPrefix = category.icon.prefix;
                    checkin.iconSuffix = category.icon.suffix;
                }
            }

            return checkin;
        }
    }

    // Parser

    public class Response {

        public ResponseCheckins checkins;
    }

    public class ResponseCheckins {

        public List<ResponseItem> items;
    }

    public class ResponseItem {

        public String id;
        public long createdAt;
        public String shout;
        public ResponseVenue venue;
    }

    public class ResponseVenue {

        public String name;
        public ResponseLocation location;
        public List<ResponseCategory> categories;
    }

    public class ResponseLocation {

        public double lat;
        public double lng;
    }

    public class ResponseCategory {

        public boolean primary;
        public ResponseCategoryIcon icon;
    }

    public class ResponseCategoryIcon {

        public String prefix;
        public String suffix;
    }
}
