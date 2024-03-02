package edu.cmu.cc.minisite;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Objects;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bson.Document;


/**
 * Task 3:
 * Implement your logic to return all the comments authored by this user.
 *
 * You should sort the comments by ups in descending order (from the largest to the smallest one).
 * If there is a tie in the ups, sort the comments in descending order by their timestamp.
 */
public class HomepageServlet extends HttpServlet {

    /**
     * The endpoint of the database.
     *
     * To avoid hardcoding credentials, use environment variables to include
     * the credentials.
     *
     * e.g., before running "mvn clean package exec:java" to start the server
     * run the following commands to set the environment variables.
     * export MONGO_HOST=...
     */
    private static final String MONGO_HOST = System.getenv("MONGO_HOST");
    /**
     * MongoDB server URL.
     */
    private static final String URL = "mongodb://" + MONGO_HOST + ":27017";
    /**
     * Database name.
     */
    private static final String DB_NAME = "reddit_db";
    /**
     * Collection name.
     */
    private static final String COLLECTION_NAME = "posts";
    /**
     * MongoDB connection.
     */
    private static MongoCollection<Document> collection;

    /**
     * Initialize the connection.
     */
    public HomepageServlet() {
        Objects.requireNonNull(MONGO_HOST);
        MongoClientURI connectionString = new MongoClientURI(URL);
        MongoClient mongoClient = new MongoClient(connectionString);
        MongoDatabase database = mongoClient.getDatabase(DB_NAME);
        collection = database.getCollection(COLLECTION_NAME);
    }

    /**
     * Implement this method.
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
        JsonObject result = new JsonObject();
        String id = request.getParameter("id");
        JsonArray commentsArray = getComments(id);
        result.add("comments", commentsArray);
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        PrintWriter writer = response.getWriter();
        writer.write(result.toString());
        writer.close();
    }

    /**
     * Method to get the comments authored by the user.
     *
     * @param id user id
     * @return comments authored by the user
     */
    public JsonArray getComments(String id) {
        JsonArray commentsArray = new JsonArray();
        Document query = new Document("uid", id);
        // Sort by ups in descending order, and then by timestamp in descending order
        Document sort = new Document("ups", -1).append("timestamp", -1);
        for (Document doc : collection.find(query).sort(sort)) {
            JsonObject comment = new JsonObject();
            comment.addProperty("cid", doc.getString("cid"));
            comment.addProperty("parent_id", doc.getString("parent_id"));
            comment.addProperty("uid", doc.getString("uid"));
            comment.addProperty("timestamp", doc.getString("timestamp"));
            comment.addProperty("content", doc.getString("content"));
            comment.addProperty("subreddit", doc.getString("subreddit"));
            // ups and downs are integers
            comment.addProperty("ups", doc.getInteger("ups"));
            comment.addProperty("downs", doc.getInteger("downs"));
            commentsArray.add(comment);
        }
        return commentsArray;
    }
    // get comment by c_id
    public JsonObject getCommentByCid(String c_id) {
        JsonObject comment = new JsonObject();
        Document query = new Document("cid", c_id);
        Document doc = collection.find(query).first();
        if (doc == null) {
            return null;
        }
        comment.addProperty("cid", doc.getString("cid"));
        comment.addProperty("parent_id", doc.getString("parent_id"));
        comment.addProperty("uid", doc.getString("uid"));
        comment.addProperty("timestamp", doc.getString("timestamp"));
        comment.addProperty("content", doc.getString("content"));
        comment.addProperty("subreddit", doc.getString("subreddit"));
        // ups and downs are integers
        comment.addProperty("ups", doc.getInteger("ups"));
        comment.addProperty("downs", doc.getInteger("downs"));
        return comment;
    }
}

