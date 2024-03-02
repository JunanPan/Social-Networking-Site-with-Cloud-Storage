package edu.cmu.cc.minisite;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Comparator;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * In this task you will populate a user's timeline.
 * This task helps you understand the concept of fan-out. 
 * Practice writing complex fan-out queries that span multiple databases.
 *
 * Task 4 (1):
 * Get the name and profile of the user as you did in Task 1
 * Put them as fields in the result JSON object
 *
 * Task 4 (2);
 * Get the follower name and profiles as you did in Task 2
 * Put them in the result JSON object as one array
 *
 * Task 4 (3):
 * From the user's followees, get the 30 most popular comments
 * and put them in the result JSON object as one JSON array.
 * (Remember to find their parent and grandparent)
 *
 * The posts should be sorted:
 * First by ups in descending order.
 * Break tie by the timestamp in descending order.
 */
public class TimelineServlet extends HttpServlet {

    /**
     * Your initialization code goes here.
     */
    public TimelineServlet() {
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

        // DON'T modify this method.
        String id = request.getParameter("id");
        String result = getTimeline(id);
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        PrintWriter writer = response.getWriter();
        writer.print(result);
        writer.close();
    }

    /**
     * Method to get given user's timeline.
     *
     * @param id user id
     * @return timeline of this user
     */
    private String getTimeline(String id) {
        JsonObject result = new JsonObject();
        // TODO: implement this method
        // get profile
        System.out.println("getting profile");
        try{
            ProfileServlet profileServlet = new ProfileServlet();
            JsonObject profile = profileServlet.getProfile(id);
            result.add("name", profile.get("name"));
            result.add("profile", profile.get("profile"));
        }
        catch (Exception e){
            e.printStackTrace();
        }
        
        System.out.println("getting followers");
        // get followers from FollowerServlet by http request
        FollowerServlet followerServlet = new FollowerServlet();
        JsonArray followers = followerServlet.getFollowers(id);
        result.add("followers", followers);

        // get posts
        System.out.println("getting comments");
        HomepageServlet homepageServlet = new HomepageServlet();
        JsonArray followees = followerServlet.getFollowees(id);
        // get all comments from followees
        JsonArray limitedComments = new JsonArray();
        for (int i = 0; i < followees.size(); i++) {
            String followeeName = followees.get(i).getAsJsonObject().get("name").getAsString();
            JsonArray comments = homepageServlet.getComments(followeeName);
            for (int j = 0; j < comments.size(); j++) {
                limitedComments.add(comments.get(j));
            }
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

        result.add("comments", limitedComments);
        return result.toString();
    }
}

