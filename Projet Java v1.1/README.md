# file-management-app
A limited system for file synchronization and sharing.
## Compile Client
```cd Client```
```javac -d classes Client/*.java```
## Compile Server
```cd Server```
```javac -d classes Server/*.java ./Utils/*.java```
## Run Client
```cd Client```
```java -cp "classes" Client.ClientApp```
## Run Server 
```cd Server```
```java -cp "classes" Server.ServerApp```