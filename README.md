# file-management-app
A limited system for file synchronization and sharing.
## Compile Client
javac -d classes ./Client/Client.java ./Enums/*.java
## Compile Server
javac -d classes ./Server/AppServer.java ./Server/AuthentificationManager.java ./Server/ClientHandler.java ./Server/User.java ./Enums/*.java
## Run Client
javac -cp "./classes/" Client.Client
## Run Server 
javac -cp "./classes/" Server.AppServer