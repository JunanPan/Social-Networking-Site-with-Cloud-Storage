package edu.cmu.cc.minisite;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * In this task you will populate a user's timeline.
 * This task helps you understand the concept of fan-out and caching.
 * Practice writing complex fan-out queries that span multiple databases.
 * Also practice using caching mechanism to boost your backend!
 *
 * Task 5 (1):
 * Get the name and profile of the user as you did in Task 3
 * Put them as fields in the result JSON object
 *
 * Task 5 (2);
 * Get the follower name and profiles as you did in Task 4
 * Put them in the result JSON object as one array
 *
 * Task 5 (3):
 * From the user's followees, get the 30 most popular comments
 * and put them in the result JSON object as one JSON array.
 * (Remember to find their parent and grandparent)
 *
 * Task 5 (4):
 * Make sure your implementation can finish a request that is sent
 * before in a short time.
 *
 * The posts should be sorted:
 * First by ups in descending order.
 * Break tie by the timestamp in descending order.
 */
public class TimelineWithCacheServlet extends HttpServlet {

    /**
     * You need to use this variable to implement your caching
     * mechanism. Please see {@link Cache#put}, {@link Cache#get}.
     *
     */
    private static Cache cache = new Cache();

    /**
     * Your initialization code goes here.
     */
    public TimelineWithCacheServlet() {
    }

    /**
     * Don't modify this method.
     *
     * @param request  the request object that is passed to the servlet
     * @param response the response object that the servlet
     *                 uses to return the headers to the client
     * @throws IOException      if an input or output error occurs
     * @throws ServletException if the request for the HEAD
     *                          could not be handled
     */
    @Override
    protected void doGet(final HttpServletRequest request,
                         final HttpServletResponse response) throws ServletException, IOException {

        // DON'T modify this method
        String id = request.getParameter("id");
        String result = getTimeline(id);

        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.addHeader("CacheHit", String.valueOf(cache.get(id) != null));
        PrintWriter writer = response.getWriter();
        writer.print(result);
        writer.close();
    }
    /**
     * get profile
     */
    private JsonObject getProfileFromServ(String id) throws IOException {
        try{
            ProfileServlet profileServlet = new ProfileServlet();
            JsonObject profile = profileServlet.getProfile(id);
            return profile;
        } 
        catch (Exception e){
            e.printStackTrace();
            return null;
        }

    }
    /**
     * get comments
     * @param id
     * @return
     * @throws IOException
     */
    private JsonArray getCommentsFromServ(String id) throws IOException {
        HomepageServlet homepageServlet = new HomepageServlet();
        FollowerServlet followerServlet = new FollowerServlet();
        JsonArray followees = followerServlet.getFollowees(id);
        // get all comments from followees
        JsonArray limitedComments = new JsonArray();
        for (int i = 0; i < followees.size(); i++) {
            String followeeName = followees.get(i).getAsJsonObject().get("name").getAsString();
            // try to get comments from cache
            try {
                String comments = cache.get(followeeName + "comments");
                if (comments != null) {
                    // convert comments to JsonArray and add to limitedComments
                    JsonArray commentsArray = JsonParser.parseString(comments).getAsJsonArray();
                    limitedComments.addAll(commentsArray);
                    continue;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            JsonArray comments = homepageServlet.getComments(followeeName);
            limitedComments.addAll(comments);
        }
        // Sort the most popular 30 comments from the user's followees based on "ups" in descending order. 
        // In case of ties, use the comment's timestamp. 
        List<JsonObject> commentList = new ArrayList<>();
        for (int i = 0; i < limitedComments.size(); i++) {
            commentList.add(limitedComments.get(i).getAsJsonObject());
        }
        Collections.sort(commentList, new Comparator<JsonObject>() {
        @Override
        public int compare(JsonObject o1, JsonObject o2) {
            int ups1 = o1.get("ups").getAsInt();
            int ups2 = o2.get("ups").getAsInt();
            // First compare by "ups"
            if (ups1 != ups2) {
                return Integer.compare(ups2, ups1); // Note the order for descending
            }
            // If "ups" are equal, compare by "timestamp" (assuming timestamp is a String that can be compared directly; adjust as needed)
            String timestamp1 = o1.get("timestamp").getAsString();
            String timestamp2 = o2.get("timestamp").getAsString();
            return timestamp2.compareTo(timestamp1); // For descending order
            }
        });

        // get top 30 comments
        List<JsonObject> topComments = commentList.size() > 30 ? commentList.subList(0, 30) : commentList;

        // change it back from list to JsonArray
        limitedComments = new JsonArray();
        for (JsonObject comment : topComments) {
            limitedComments.add(comment);
        }

        // get parent and grandparent
        for (int i = 0; i < limitedComments.size(); i++) {
            JsonObject comment = limitedComments.get(i).getAsJsonObject();
            String parent_id = comment.get("parent_id").getAsString();
            // try to get parent
            JsonObject parent = homepageServlet.getCommentByCid(parent_id);
            if (parent != null) {
                // if parent exists, add it to the comment
                comment.add("parent", parent);
                String grandparent_id = parent.get("parent_id").getAsString();
                // try to get grandparent
                JsonObject grandparent = homepageServlet.getCommentByCid(grandparent_id);
                if (grandparent != null){
                    // if grandparent exists, add it to the comment
                    comment.add("grand_parent", grandparent);
                }
            
            }
        }
        return limitedComments;
    }

    /**
     * Method to get given user's timeline.
     * You are required to implement caching mechanism with
     * given cache variable.
     *
     * @param id user id
     * @return timeline of this user
     */
    private String getTimeline(String id) throws IOException {
        // TODO: implement this method

    
        // if current user is top or not
        // A user is considered a "top user" if this user has more than 300 followers. 
        Boolean top = false;
        FollowerServlet followerServlet = new FollowerServlet();
        Integer followersNumber = followerServlet.getFollowersNumber(id);
        System.out.println(" id: " + id, "followersNumber: " + followersNumber);
        if (followersNumber > 300) {
            top = true;
        }

        JsonObject result = new JsonObject();
        System.out.println("getting profile");
        if (top) {
            // if user is top, cache his profile
            String profile_string = cache.get(id + "profile");
            if (profile_string != null) {
                result.add("name", JsonParser.parseString(profile_string).getAsJsonObject().get("name"));
                result.add("profile", JsonParser.parseString(profile_string).getAsJsonObject().get("profile"));
            }else{
                JsonObject profile = getProfileFromServ(id);
                result.add("name", profile.get("name"));
                result.add("profile", profile.get("profile"));
                cache.put(id + "profile", profile.toString());
            }
        }else{
            JsonObject profile = getProfileFromServ(id);
            result.add("name", profile.get("name"));
            result.add("profile", profile.get("profile"));
        }

        System.out.println("getting followers");
        if(top){
            String followers_string = cache.get(id + "followers");
            if (followers_string != null) {
                // convert followers to JsonArray and add to result
                JsonArray followersArray = JsonParser.parseString(followers_string).getAsJsonArray();
                result.add("followers", followersArray);
            }else{
                JsonArray followers = followerServlet.getFollowers(id);
                result.add("followers", followers);
                // if user has more than 300 followers, cache his followers
                cache.put(id + "followers", followers.toString());
            }
        }else{
            JsonArray followers = followerServlet.getFollowers(id);
            result.add("followers", followers);
        }


        // get posts
        System.out.println("getting comments");
        if (top) {
            String comments_string = cache.get(id + "comments");
            if (comments_string != null) {
                JsonArray commentsArray = JsonParser.parseString(comments_string).getAsJsonArray();
                result.add("comments", commentsArray);}
            else{
                JsonArray limitedComments = getCommentsFromServ(id);
                result.add("comments", limitedComments);
                cache.put(id + "comments", limitedComments.toString());
            }
        }
        else{
            JsonArray limitedComments = getCommentsFromServ(id);
            result.add("comments", limitedComments);
        }
        return result.toString();
    }
}

