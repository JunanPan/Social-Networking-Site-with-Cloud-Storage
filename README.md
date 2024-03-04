# Social Networking Site 

This repository hosts the backend code for a sophisticated social networking site extension that enhances the Reddit.com experience by allowing users to view their friends' recent activities. The system integrates several database technologies, including SQL, Neo4j, and MongoDB, to support complex functionalities such as user authentication, social graph management, user-generated content, and efficient data retrieval.

## Project Components Overview  
<img align="center" src="https://github.com/JunanPan/Social-Networking-Site-with-Cloud-Storage/blob/intro/imgs/architecture.png" width="800px">  

### User Authentication System

- Implemented using Azure Database for MySQL server, the user authentication system securely manages user logins and registrations. It serves as the gateway to accessing the social networking features, ensuring that user credentials are verified and protected.  
<p align="center">
<img src="https://github.com/JunanPan/Social-Networking-Site-with-Cloud-Storage/blob/intro/imgs/profile_serv.png" width="300px">  
</p>

### Follower/Followee Servlet

- Leveraging Neo4j, a powerful graph database, this component is responsible for storing and querying the social relationships among users. It efficiently manages connections, such as followers and followees, enabling the core social networking functionality of the platform.  
<p align="center">
<img src="https://github.com/JunanPan/Social-Networking-Site-with-Cloud-Storage/blob/intro/imgs/follower_serv.png" width="300px">  
</p>

### User-Generated Content Handling

- MongoDB is utilized to store user-generated content, including comments and posts. This noSQL database is chosen for its flexibility in handling unstructured data and its scalability, essential for supporting dynamic content creation and retrieval on the homepage.  
<p align="center">
<img src="https://github.com/JunanPan/Social-Networking-Site-with-Cloud-Storage/blob/intro/imgs/comments_serv.png" width="600px">  
</p>

### Timeline Generation

This feature combines SQL, Neo4j, and MongoDB to extract and display the user's timeline. It:
- Identifies top 30 comments from followees by "ups."
- Includes user details, followers, and hierarchical comments (parent and grandparent).

### Caching for Performance

A caching mechanism targets "top users" (over 300 followers) to enhance response times. It:
- Caches timelines of "top users" for quick retrieval.
- Ensures all user requests are processed efficiently, with special attention to reducing load times for users with significant followings.

